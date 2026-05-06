package com.pengcheng.system.meeting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.mapper.CalendarEventMapper;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.meeting.dto.MeetingCalendarSaveRequest;
import com.pengcheng.system.meeting.dto.MeetingMinutesSaveRequest;
import com.pengcheng.system.meeting.dto.ReminderConfigDTO;
import com.pengcheng.system.meeting.entity.MeetingFile;
import com.pengcheng.system.meeting.entity.MeetingMinutes;
import com.pengcheng.system.meeting.entity.MeetingNotification;
import com.pengcheng.system.meeting.mapper.MeetingFileMapper;
import com.pengcheng.system.meeting.mapper.MeetingMinutesMapper;
import com.pengcheng.system.meeting.mapper.MeetingNotificationMapper;
import com.pengcheng.system.meeting.vo.MeetingCalendarVO;
import com.pengcheng.system.meeting.vo.MeetingFileVO;
import com.pengcheng.system.meeting.vo.MeetingMinutesVO;
import com.pengcheng.system.meeting.vo.MeetingParticipantVO;
import com.pengcheng.system.service.SysConfigGroupService;
import com.pengcheng.system.service.SysUserService;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.mapper.TodoMapper;
import com.pengcheng.system.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingCalendarService {

    static final String MEETING_EVENT_TYPE = "meeting";
    static final String MEETING_CONFIG_GROUP_CODE = "meetingConfig";
    static final String MEETING_ACTION_TODO_SOURCE_TYPE = "meeting_action";

    private final CalendarEventMapper calendarEventMapper;
    private final MeetingMinutesMapper meetingMinutesMapper;
    private final MeetingFileMapper meetingFileMapper;
    private final MeetingNotificationMapper meetingNotificationMapper;
    private final SysUserService sysUserService;
    private final SysConfigGroupService configGroupService;
    private final ObjectMapper objectMapper;
    private final TodoService todoService;
    private final TodoMapper todoMapper;

    public List<MeetingCalendarVO> getMonthMeetings(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return listMeetings(userId, start, end);
    }

    public List<MeetingCalendarVO> getDayMeetings(Long userId, LocalDate date) {
        return listMeetings(userId, date, date);
    }

    public MeetingCalendarVO getMeetingDetail(Long userId, Long meetingId) {
        CalendarEvent event = getAccessibleMeeting(userId, meetingId);
        return buildMeetingVO(event, true);
    }

    @Transactional
    public MeetingCalendarVO createMeeting(Long userId, MeetingCalendarSaveRequest request) {
        validateRequest(request);

        CalendarEvent event = new CalendarEvent();
        applyRequest(event, request);
        event.setEventType(MEETING_EVENT_TYPE);
        event.setUserId(userId);
        event.setOrganizerId(userId);
        event.setStatus(1);
        event.setReminderSent(false);
        calendarEventMapper.insert(event);

        replaceParticipants(event.getId(), request.getParticipantIds(), event.getStartTime(), event.getReminderMinutes());
        return getMeetingDetail(userId, event.getId());
    }

    @Transactional
    public MeetingCalendarVO updateMeeting(Long userId, Long meetingId, MeetingCalendarSaveRequest request) {
        validateRequest(request);

        CalendarEvent existing = getOwnedMeeting(userId, meetingId);
        applyRequest(existing, request);
        calendarEventMapper.updateById(existing);

        replaceParticipants(existing.getId(), request.getParticipantIds(), existing.getStartTime(), existing.getReminderMinutes());
        return getMeetingDetail(userId, meetingId);
    }

    @Transactional
    public void cancelMeeting(Long userId, Long meetingId) {
        CalendarEvent existing = getOwnedMeeting(userId, meetingId);
        existing.setStatus(0);
        calendarEventMapper.updateById(existing);
    }

    @Transactional
    public MeetingMinutesVO saveMinutes(Long userId, Long meetingId, MeetingMinutesSaveRequest request) {
        CalendarEvent meeting = getOwnedMeeting(userId, meetingId);

        MeetingMinutes minutes = meetingMinutesMapper.selectOne(new LambdaQueryWrapper<MeetingMinutes>()
                .eq(MeetingMinutes::getMeetingId, meetingId)
                .orderByDesc(MeetingMinutes::getUpdateTime)
                .last("LIMIT 1"));
        if (minutes == null) {
            minutes = new MeetingMinutes();
            minutes.setMeetingId(meetingId);
            minutes.setCreatorId(userId);
            minutes.setStatus(defaultMinutesStatus(request.getStatus()));
            applyMinutes(minutes, request);
            meetingMinutesMapper.insert(minutes);
        } else {
            applyMinutes(minutes, request);
            minutes.setStatus(defaultMinutesStatus(request.getStatus() != null ? request.getStatus() : minutes.getStatus()));
            meetingMinutesMapper.updateById(minutes);
        }
        syncActionItemsToTodos(meeting, minutes, userId);
        return toMinutesVO(minutes);
    }

    public ReminderConfigDTO getReminderConfig() {
        ReminderConfigDTO config = new ReminderConfigDTO();
        SysConfigGroup group = configGroupService.getByGroupCode(MEETING_CONFIG_GROUP_CODE);
        if (group == null || !StringUtils.hasText(group.getConfigValue())) {
            return config;
        }
        try {
            ReminderConfigDTO saved = objectMapper.readValue(group.getConfigValue(), ReminderConfigDTO.class);
            if (saved.getDefaultReminder() != null) {
                config.setDefaultReminder(saved.getDefaultReminder());
            }
            if (saved.getInternalNotification() != null) {
                config.setInternalNotification(saved.getInternalNotification());
            }
            if (saved.getEmail() != null) {
                config.setEmail(saved.getEmail());
            }
        } catch (Exception ignored) {
        }
        return config;
    }

    public void saveReminderConfig(ReminderConfigDTO request) {
        ReminderConfigDTO config = new ReminderConfigDTO();
        if (request.getDefaultReminder() != null) {
            config.setDefaultReminder(request.getDefaultReminder());
        }
        if (request.getInternalNotification() != null) {
            config.setInternalNotification(request.getInternalNotification());
        }
        if (request.getEmail() != null) {
            config.setEmail(request.getEmail());
        }
        try {
            configGroupService.saveConfig(MEETING_CONFIG_GROUP_CODE, objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            throw new IllegalStateException("保存会议提醒配置失败", e);
        }
    }

    List<MeetingCalendarVO> listMeetings(Long userId, LocalDate start, LocalDate end) {
        List<CalendarEvent> events = calendarEventMapper.selectList(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getEventType, MEETING_EVENT_TYPE)
                .ge(CalendarEvent::getStartTime, start.atStartOfDay())
                .lt(CalendarEvent::getStartTime, end.plusDays(1).atStartOfDay())
                .orderByAsc(CalendarEvent::getStartTime));
        if (CollectionUtils.isEmpty(events)) {
            return Collections.emptyList();
        }

        Map<Long, Set<Long>> participantIdsByMeeting = getParticipantIdsByMeeting(
                events.stream().map(CalendarEvent::getId).collect(Collectors.toList()));

        return events.stream()
                .filter(event -> isAccessible(userId, event, participantIdsByMeeting.getOrDefault(event.getId(), Collections.emptySet())))
                .map(event -> buildMeetingVO(event, false, participantIdsByMeeting.get(event.getId())))
                .sorted(Comparator.comparing(MeetingCalendarVO::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
    }

    private MeetingCalendarVO buildMeetingVO(CalendarEvent event, boolean includeDetails) {
        return buildMeetingVO(event, includeDetails, null);
    }

    private MeetingCalendarVO buildMeetingVO(CalendarEvent event, boolean includeDetails, Collection<Long> participantIds) {
        MeetingCalendarVO vo = new MeetingCalendarVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setDescription(event.getDescription());
        vo.setType(event.getMeetingType() != null ? event.getMeetingType() : 1);
        vo.setStartTime(event.getStartTime());
        vo.setEndTime(event.getEndTime());
        vo.setLocation(event.getLocation());
        vo.setMeetingUrl(event.getMeetingUrl());
        vo.setReminderMinutes(event.getReminderMinutes());
        vo.setStatus(resolveMeetingStatus(event));
        vo.setOrganizerId(event.getOrganizerId() != null ? event.getOrganizerId() : event.getUserId());

        Map<Long, SysUser> userMap = loadUsers(collectUserIds(event, participantIds));
        SysUser organizer = userMap.get(vo.getOrganizerId());
        if (organizer != null) {
            vo.setOrganizerName(resolveUserName(organizer));
        }

        List<Long> normalizedParticipantIds = normalizeParticipantIds(participantIds);
        vo.setParticipantIds(normalizedParticipantIds);
        vo.setParticipants(normalizedParticipantIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(this::toParticipantVO)
                .toList());

        if (includeDetails) {
            vo.setMinutes(loadMinutes(event.getId()));
            vo.setFiles(loadFiles(event.getId()));
        }
        return vo;
    }

    private Map<Long, Set<Long>> getParticipantIdsByMeeting(List<Long> meetingIds) {
        if (CollectionUtils.isEmpty(meetingIds)) {
            return Collections.emptyMap();
        }
        List<MeetingNotification> rows = meetingNotificationMapper.selectList(new LambdaQueryWrapper<MeetingNotification>()
                .in(MeetingNotification::getMeetingId, meetingIds));
        Map<Long, Set<Long>> grouped = new LinkedHashMap<>();
        for (MeetingNotification row : rows) {
            grouped.computeIfAbsent(row.getMeetingId(), key -> new LinkedHashSet<>()).add(row.getUserId());
        }
        return grouped;
    }

    private Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        List<Long> ids = normalizeParticipantIds(userIds);
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return sysUserService.listByIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Set<Long> collectUserIds(CalendarEvent event, Collection<Long> participantIds) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (event.getOrganizerId() != null) {
            userIds.add(event.getOrganizerId());
        }
        if (event.getUserId() != null) {
            userIds.add(event.getUserId());
        }
        if (participantIds != null) {
            userIds.addAll(participantIds);
        }
        return userIds;
    }

    private MeetingParticipantVO toParticipantVO(SysUser user) {
        MeetingParticipantVO participant = new MeetingParticipantVO();
        participant.setId(user.getId());
        participant.setName(resolveUserName(user));
        participant.setAvatar(user.getAvatar());
        return participant;
    }

    private MeetingMinutesVO loadMinutes(Long meetingId) {
        MeetingMinutes minutes = meetingMinutesMapper.selectOne(new LambdaQueryWrapper<MeetingMinutes>()
                .eq(MeetingMinutes::getMeetingId, meetingId)
                .orderByDesc(MeetingMinutes::getUpdateTime)
                .last("LIMIT 1"));
        return minutes == null ? null : toMinutesVO(minutes);
    }

    private MeetingMinutesVO toMinutesVO(MeetingMinutes minutes) {
        MeetingMinutesVO vo = new MeetingMinutesVO();
        vo.setId(minutes.getId());
        vo.setContent(minutes.getContent());
        vo.setConclusions(minutes.getConclusions());
        vo.setActionItems(minutes.getActionItems());
        vo.setStatus(minutes.getStatus());
        vo.setCreatorId(minutes.getCreatorId());
        vo.setCreateTime(minutes.getCreateTime());
        vo.setUpdateTime(minutes.getUpdateTime());
        return vo;
    }

    private List<MeetingFileVO> loadFiles(Long meetingId) {
        List<MeetingFile> files = meetingFileMapper.selectList(new LambdaQueryWrapper<MeetingFile>()
                .eq(MeetingFile::getMeetingId, meetingId)
                .orderByDesc(MeetingFile::getCreateTime));
        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptyList();
        }
        return files.stream().map(file -> {
            MeetingFileVO vo = new MeetingFileVO();
            vo.setId(file.getId());
            vo.setFileId(file.getFileId());
            vo.setName(file.getFileName());
            vo.setType(file.getFileType());
            vo.setSize(file.getFileSize());
            vo.setCreateTime(file.getCreateTime());
            vo.setUrl(file.getFileId() == null ? null : "/api/sys/file/download/" + file.getFileId());
            return vo;
        }).toList();
    }

    private void applyRequest(CalendarEvent event, MeetingCalendarSaveRequest request) {
        event.setTitle(request.getTitle().trim());
        event.setDescription(trimToNull(request.getDescription()));
        event.setMeetingType(request.getType() != null ? request.getType() : 1);
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setLocation(trimToNull(request.getLocation()));
        event.setMeetingUrl(trimToNull(request.getMeetingUrl()));
        event.setReminderMinutes(request.getReminderMinutes() != null ? request.getReminderMinutes() : getReminderConfig().getDefaultReminder());
        event.setAllDay(Boolean.FALSE);
        event.setColor(resolveMeetingColor(event.getMeetingType()));
    }

    private void applyMinutes(MeetingMinutes minutes, MeetingMinutesSaveRequest request) {
        minutes.setContent(trimToNull(request.getContent()));
        minutes.setConclusions(trimToNull(request.getConclusions()));
        minutes.setActionItems(trimToNull(request.getActionItems()));
    }

    private void syncActionItemsToTodos(CalendarEvent meeting, MeetingMinutes minutes, Long userId) {
        List<String> actionItems = extractActionItems(minutes.getActionItems());
        if (CollectionUtils.isEmpty(actionItems)) {
            return;
        }

        String description = buildMeetingTodoDescription(meeting, minutes);
        for (String actionTitle : actionItems) {
            if (meetingTodoExists(meeting.getId(), userId, actionTitle)) {
                continue;
            }
            Todo todo = new Todo();
            todo.setUserId(userId);
            todo.setAssigneeId(userId);
            todo.setTitle(actionTitle);
            todo.setDescription(description);
            todo.setSourceType(MEETING_ACTION_TODO_SOURCE_TYPE);
            todo.setSourceId(meeting.getId());
            todo.setStatus(0);
            todoService.createTodo(todo);
        }
    }

    private boolean meetingTodoExists(Long meetingId, Long userId, String actionTitle) {
        Long count = todoMapper.selectCount(new LambdaQueryWrapper<Todo>()
                .eq(Todo::getUserId, userId)
                .eq(Todo::getSourceType, MEETING_ACTION_TODO_SOURCE_TYPE)
                .eq(Todo::getSourceId, meetingId)
                .eq(Todo::getTitle, actionTitle));
        return count != null && count > 0;
    }

    private List<String> extractActionItems(String actionItems) {
        if (!StringUtils.hasText(actionItems)) {
            return Collections.emptyList();
        }

        String normalized = actionItems.replace("\r\n", "\n").trim();
        String[] rawItems = normalized.split("\\n+");
        if (rawItems.length <= 1) {
            rawItems = normalized.split("[；;]+");
        }

        List<String> items = new ArrayList<>();
        Set<String> uniqueItems = new LinkedHashSet<>();
        for (String rawItem : rawItems) {
            String cleaned = cleanActionItem(rawItem);
            if (StringUtils.hasText(cleaned) && uniqueItems.add(cleaned)) {
                items.add(cleaned);
            }
        }

        if (items.isEmpty()) {
            String cleaned = cleanActionItem(normalized);
            if (StringUtils.hasText(cleaned)) {
                items.add(cleaned);
            }
        }
        return items;
    }

    private String cleanActionItem(String rawItem) {
        if (!StringUtils.hasText(rawItem)) {
            return null;
        }
        String cleaned = rawItem.trim()
                .replaceFirst("^[-*•·]+\\s*", "")
                .replaceFirst("^[0-9]+[.、)]\\s*", "");
        return trimToNull(cleaned);
    }

    private String buildMeetingTodoDescription(CalendarEvent meeting, MeetingMinutes minutes) {
        String title = StringUtils.hasText(meeting.getTitle()) ? meeting.getTitle().trim() : "未命名会议";
        if (!StringUtils.hasText(minutes.getConclusions())) {
            return "来自会议《" + title + "》的行动项";
        }
        return "来自会议《" + title + "》的行动项。会议结论：" + minutes.getConclusions().trim();
    }

    private void replaceParticipants(Long meetingId, List<Long> participantIds, LocalDateTime startTime, Integer reminderMinutes) {
        meetingNotificationMapper.delete(new LambdaQueryWrapper<MeetingNotification>()
                .eq(MeetingNotification::getMeetingId, meetingId));

        List<Long> ids = normalizeParticipantIds(participantIds);
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        ReminderConfigDTO config = getReminderConfig();
        int notifyType = Boolean.TRUE.equals(config.getEmail()) && !Boolean.TRUE.equals(config.getInternalNotification()) ? 2 : 1;
        LocalDateTime notifyTime = reminderMinutes != null && reminderMinutes > 0 ? startTime.minusMinutes(reminderMinutes) : null;

        for (Long participantId : ids) {
            MeetingNotification notification = new MeetingNotification();
            notification.setMeetingId(meetingId);
            notification.setUserId(participantId);
            notification.setNotifyType(notifyType);
            notification.setNotifyTime(notifyTime);
            notification.setStatus(1);
            meetingNotificationMapper.insert(notification);
        }
    }

    private CalendarEvent getAccessibleMeeting(Long userId, Long meetingId) {
        CalendarEvent event = getMeetingOrThrow(meetingId);
        Set<Long> participantIds = getParticipantIdsByMeeting(List.of(meetingId)).getOrDefault(meetingId, Collections.emptySet());
        if (!isAccessible(userId, event, participantIds)) {
            throw new IllegalArgumentException("无权查看该会议");
        }
        return event;
    }

    private CalendarEvent getOwnedMeeting(Long userId, Long meetingId) {
        CalendarEvent event = getMeetingOrThrow(meetingId);
        Long organizerId = event.getOrganizerId() != null ? event.getOrganizerId() : event.getUserId();
        if (!Objects.equals(organizerId, userId)) {
            throw new IllegalArgumentException("仅组织者可编辑该会议");
        }
        return event;
    }

    private CalendarEvent getMeetingOrThrow(Long meetingId) {
        CalendarEvent event = calendarEventMapper.selectById(meetingId);
        if (event == null || !MEETING_EVENT_TYPE.equals(event.getEventType())) {
            throw new IllegalArgumentException("会议不存在");
        }
        return event;
    }

    private boolean isAccessible(Long userId, CalendarEvent event, Set<Long> participantIds) {
        Long organizerId = event.getOrganizerId() != null ? event.getOrganizerId() : event.getUserId();
        return Objects.equals(event.getUserId(), userId)
                || Objects.equals(organizerId, userId)
                || participantIds.contains(userId);
    }

    private int resolveMeetingStatus(CalendarEvent event) {
        if (event.getStatus() != null && event.getStatus() == 0) {
            return 3;
        }
        LocalDateTime now = LocalDateTime.now();
        if (event.getStartTime() != null && now.isBefore(event.getStartTime())) {
            return 1;
        }
        if (event.getEndTime() != null && now.isAfter(event.getEndTime())) {
            return 4;
        }
        return 2;
    }

    private void validateRequest(MeetingCalendarSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("会议主题不能为空");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("会议时间不能为空");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("会议结束时间不能早于开始时间");
        }
    }

    private List<Long> normalizeParticipantIds(Collection<Long> participantIds) {
        if (participantIds == null) {
            return Collections.emptyList();
        }
        return participantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String resolveUserName(SysUser user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "用户" + user.getId();
    }

    private String resolveMeetingColor(Integer type) {
        if (type == null) {
            return "#18a058";
        }
        return switch (type) {
            case 2 -> "#2080f0";
            case 3 -> "#f0a020";
            default -> "#18a058";
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int defaultMinutesStatus(Integer status) {
        return status != null ? status : 1;
    }
}
