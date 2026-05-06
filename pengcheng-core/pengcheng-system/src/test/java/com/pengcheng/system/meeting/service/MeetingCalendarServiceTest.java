package com.pengcheng.system.meeting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import com.pengcheng.system.calendar.mapper.CalendarEventMapper;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.meeting.dto.MeetingMinutesSaveRequest;
import com.pengcheng.system.meeting.dto.ReminderConfigDTO;
import com.pengcheng.system.meeting.entity.MeetingMinutes;
import com.pengcheng.system.meeting.mapper.MeetingFileMapper;
import com.pengcheng.system.meeting.mapper.MeetingMinutesMapper;
import com.pengcheng.system.meeting.mapper.MeetingNotificationMapper;
import com.pengcheng.system.meeting.vo.MeetingCalendarVO;
import com.pengcheng.system.meeting.vo.MeetingMinutesVO;
import com.pengcheng.system.service.SysConfigGroupService;
import com.pengcheng.system.service.SysUserService;
import com.pengcheng.system.todo.entity.Todo;
import com.pengcheng.system.todo.mapper.TodoMapper;
import com.pengcheng.system.todo.service.TodoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingCalendarServiceTest {

    @Mock
    private CalendarEventMapper calendarEventMapper;
    @Mock
    private MeetingMinutesMapper meetingMinutesMapper;
    @Mock
    private MeetingFileMapper meetingFileMapper;
    @Mock
    private MeetingNotificationMapper meetingNotificationMapper;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysConfigGroupService configGroupService;
    @Mock
    private TodoService todoService;
    @Mock
    private TodoMapper todoMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getDayMeetingsShouldMapOrganizerAndParticipants() {
        MeetingCalendarService service = createService();

        CalendarEvent event = new CalendarEvent();
        event.setId(11L);
        event.setEventType("meeting");
        event.setTitle("项目评审");
        event.setMeetingType(2);
        event.setUserId(8L);
        event.setOrganizerId(8L);
        event.setStartTime(LocalDateTime.now().plusHours(2));
        event.setEndTime(LocalDateTime.now().plusHours(3));
        event.setStatus(1);

        when(calendarEventMapper.selectList(any())).thenReturn(List.of(event));

        SysUser organizer = new SysUser();
        organizer.setId(8L);
        organizer.setNickname("组织者");
        SysUser participant = new SysUser();
        participant.setId(9L);
        participant.setNickname("参会人");

        com.pengcheng.system.meeting.entity.MeetingNotification notification = new com.pengcheng.system.meeting.entity.MeetingNotification();
        notification.setMeetingId(11L);
        notification.setUserId(9L);
        when(meetingNotificationMapper.selectList(any())).thenReturn(List.of(notification));
        when(sysUserService.listByIds(any())).thenReturn(List.of(organizer, participant));

        List<MeetingCalendarVO> result = service.getDayMeetings(9L, LocalDate.now());

        assertEquals(1, result.size());
        MeetingCalendarVO meeting = result.get(0);
        assertEquals("项目评审", meeting.getTitle());
        assertEquals(2, meeting.getType());
        assertEquals("组织者", meeting.getOrganizerName());
        assertEquals(List.of(9L), meeting.getParticipantIds());
        assertEquals(1, meeting.getParticipants().size());
        assertEquals("参会人", meeting.getParticipants().get(0).getName());
    }

    @Test
    void saveMinutesShouldUpdateExistingRecord() {
        MeetingCalendarService service = createService();

        CalendarEvent event = new CalendarEvent();
        event.setId(21L);
        event.setEventType("meeting");
        event.setUserId(7L);
        event.setOrganizerId(7L);
        when(calendarEventMapper.selectById(21L)).thenReturn(event);

        MeetingMinutes existing = new MeetingMinutes();
        existing.setId(31L);
        existing.setMeetingId(21L);
        existing.setCreatorId(7L);
        existing.setStatus(1);
        when(meetingMinutesMapper.selectOne(any())).thenReturn(existing);

        MeetingMinutesSaveRequest request = new MeetingMinutesSaveRequest();
        request.setContent("纪要内容");
        request.setConclusions("会议结论");
        request.setActionItems("后续动作");
        request.setStatus(2);

        MeetingMinutesVO saved = service.saveMinutes(7L, 21L, request);

        assertNotNull(saved);
        assertEquals("纪要内容", saved.getContent());
        assertEquals(2, saved.getStatus());
        verify(meetingMinutesMapper).updateById(existing);
        verify(meetingMinutesMapper, never()).insert(any());
        verify(todoService).createTodo(any(Todo.class));
    }

    @Test
    void saveMinutesShouldCreateTodoForEachActionItem() {
        MeetingCalendarService service = createService();

        CalendarEvent event = new CalendarEvent();
        event.setId(22L);
        event.setEventType("meeting");
        event.setUserId(7L);
        event.setOrganizerId(7L);
        event.setTitle("周例会");
        when(calendarEventMapper.selectById(22L)).thenReturn(event);
        when(meetingMinutesMapper.selectOne(any())).thenReturn(null);
        when(todoMapper.selectCount(any())).thenReturn(0L);

        MeetingMinutesSaveRequest request = new MeetingMinutesSaveRequest();
        request.setContent("纪要内容");
        request.setConclusions("同步本周工作");
        request.setActionItems("1. 联系客户确认时间\n- 更新合同版本");
        request.setStatus(2);

        service.saveMinutes(7L, 22L, request);

        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoService, times(2)).createTodo(todoCaptor.capture());
        assertEquals("联系客户确认时间", todoCaptor.getAllValues().get(0).getTitle());
        assertEquals("更新合同版本", todoCaptor.getAllValues().get(1).getTitle());
        assertEquals("meeting_action", todoCaptor.getAllValues().get(0).getSourceType());
        assertEquals(22L, todoCaptor.getAllValues().get(0).getSourceId());
    }

    @Test
    void saveMinutesShouldNotCreateTodoWhenActionItemsBlank() {
        MeetingCalendarService service = createService();

        CalendarEvent event = new CalendarEvent();
        event.setId(23L);
        event.setEventType("meeting");
        event.setUserId(7L);
        event.setOrganizerId(7L);
        when(calendarEventMapper.selectById(23L)).thenReturn(event);
        when(meetingMinutesMapper.selectOne(any())).thenReturn(null);

        MeetingMinutesSaveRequest request = new MeetingMinutesSaveRequest();
        request.setContent("纪要内容");
        request.setActionItems("   ");
        request.setStatus(2);

        service.saveMinutes(7L, 23L, request);

        verify(todoService, never()).createTodo(any(Todo.class));
        verify(todoMapper, never()).selectCount(any());
    }

    @Test
    void saveMinutesShouldSkipExistingMeetingTodo() {
        MeetingCalendarService service = createService();

        CalendarEvent event = new CalendarEvent();
        event.setId(24L);
        event.setEventType("meeting");
        event.setUserId(7L);
        event.setOrganizerId(7L);
        when(calendarEventMapper.selectById(24L)).thenReturn(event);
        when(meetingMinutesMapper.selectOne(any())).thenReturn(null);
        when(todoMapper.selectCount(any())).thenReturn(1L);

        MeetingMinutesSaveRequest request = new MeetingMinutesSaveRequest();
        request.setContent("纪要内容");
        request.setActionItems("跟进审批");
        request.setStatus(2);

        service.saveMinutes(7L, 24L, request);

        verify(todoService, never()).createTodo(any(Todo.class));
    }

    @Test
    void saveReminderConfigShouldPersistJsonConfig() throws Exception {
        MeetingCalendarService service = createService();

        ReminderConfigDTO request = new ReminderConfigDTO();
        request.setDefaultReminder(30);
        request.setInternalNotification(Boolean.FALSE);
        request.setEmail(Boolean.TRUE);

        service.saveReminderConfig(request);

        ArgumentCaptor<String> configCaptor = ArgumentCaptor.forClass(String.class);
        verify(configGroupService).saveConfig(eq("meetingConfig"), configCaptor.capture());

        ReminderConfigDTO persisted = objectMapper.readValue(configCaptor.getValue(), ReminderConfigDTO.class);
        assertEquals(30, persisted.getDefaultReminder());
        assertEquals(Boolean.FALSE, persisted.getInternalNotification());
        assertEquals(Boolean.TRUE, persisted.getEmail());
    }

    @Test
    void getReminderConfigShouldFallbackToDefaultsWhenConfigMissing() {
        MeetingCalendarService service = createService();

        when(configGroupService.getByGroupCode("meetingConfig")).thenReturn(null);

        ReminderConfigDTO config = service.getReminderConfig();

        assertEquals(15, config.getDefaultReminder());
        assertEquals(Boolean.TRUE, config.getInternalNotification());
        assertEquals(Boolean.FALSE, config.getEmail());
    }

    @Test
    void getReminderConfigShouldParseSavedJson() throws Exception {
        MeetingCalendarService service = createService();

        SysConfigGroup group = new SysConfigGroup();
        group.setConfigValue("{\"defaultReminder\":5,\"internalNotification\":false,\"email\":true}");
        when(configGroupService.getByGroupCode("meetingConfig")).thenReturn(group);

        ReminderConfigDTO config = service.getReminderConfig();

        assertEquals(5, config.getDefaultReminder());
        assertEquals(Boolean.FALSE, config.getInternalNotification());
        assertEquals(Boolean.TRUE, config.getEmail());
    }

    private MeetingCalendarService createService() {
        lenient().when(todoService.createTodo(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new MeetingCalendarService(
                calendarEventMapper,
                meetingMinutesMapper,
                meetingFileMapper,
                meetingNotificationMapper,
                sysUserService,
                configGroupService,
                objectMapper,
                todoService,
                todoMapper
        );
    }
}
