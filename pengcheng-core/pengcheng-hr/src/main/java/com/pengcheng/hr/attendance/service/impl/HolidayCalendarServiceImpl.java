package com.pengcheng.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.attendance.entity.HolidayCalendar;
import com.pengcheng.hr.attendance.mapper.HolidayCalendarMapper;
import com.pengcheng.hr.attendance.service.HolidayCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 节假日服务实现：按年缓存条目，isWorkday 走 O(1) HashMap 判定。
 * 写入操作（save/delete）会清空对应年份缓存，下次查询时重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayCalendarServiceImpl implements HolidayCalendarService {

    private final HolidayCalendarMapper mapper;

    /** year → (date → type)；type 1 休 / 2 上班 */
    private final Map<Integer, Map<LocalDate, Integer>> yearCache = new ConcurrentHashMap<>();

    @Override
    public boolean isWorkday(LocalDate date) {
        if (date == null) return false;
        Map<LocalDate, Integer> overrides = getOverridesForYear(date.getYear());
        Integer override = overrides.get(date);
        if (override != null) {
            // type=1 法定节假日 → 不工作；type=2 调休补班 → 工作
            return override == HolidayCalendar.TYPE_MAKEUP_WORKDAY;
        }
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    @Override
    public int countWorkdays(LocalDate start, LocalDate endInclusive) {
        if (start == null || endInclusive == null || endInclusive.isBefore(start)) return 0;
        int n = 0;
        for (LocalDate d = start; !d.isAfter(endInclusive); d = d.plusDays(1)) {
            if (isWorkday(d)) n++;
        }
        return n;
    }

    @Override
    public List<HolidayCalendar> listByYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return mapper.selectList(new LambdaQueryWrapper<HolidayCalendar>()
                .ge(HolidayCalendar::getHolidayDate, start)
                .le(HolidayCalendar::getHolidayDate, end)
                .orderByAsc(HolidayCalendar::getHolidayDate));
    }

    @Override
    public Long save(HolidayCalendar item) {
        if (item.getHolidayDate() == null) throw new IllegalArgumentException("日期不能为空");
        if (item.getType() == null
                || (item.getType() != HolidayCalendar.TYPE_HOLIDAY && item.getType() != HolidayCalendar.TYPE_MAKEUP_WORKDAY)) {
            throw new IllegalArgumentException("type 必须是 1 法定节假日或 2 调休补班");
        }
        if (item.getLabel() == null || item.getLabel().isBlank()) {
            throw new IllegalArgumentException("显示名不能为空");
        }
        // 同一天只允许一条（DB UNIQUE 约束兜底；这里 upsert 更友好）
        HolidayCalendar existing = mapper.selectOne(new LambdaQueryWrapper<HolidayCalendar>()
                .eq(HolidayCalendar::getHolidayDate, item.getHolidayDate())
                .last("LIMIT 1"));
        if (existing != null && (item.getId() == null || !existing.getId().equals(item.getId()))) {
            existing.setType(item.getType());
            existing.setLabel(item.getLabel());
            existing.setNote(item.getNote());
            mapper.updateById(existing);
            invalidateYearCache(item.getHolidayDate().getYear());
            return existing.getId();
        }
        if (item.getId() == null) {
            mapper.insert(item);
        } else {
            mapper.updateById(item);
        }
        invalidateYearCache(item.getHolidayDate().getYear());
        return item.getId();
    }

    @Override
    public void delete(Long id) {
        HolidayCalendar existing = mapper.selectById(id);
        if (existing == null) return;
        mapper.deleteById(id);
        if (existing.getHolidayDate() != null) {
            invalidateYearCache(existing.getHolidayDate().getYear());
        }
    }

    private Map<LocalDate, Integer> getOverridesForYear(int year) {
        return yearCache.computeIfAbsent(year, this::loadYear);
    }

    private Map<LocalDate, Integer> loadYear(int year) {
        List<HolidayCalendar> list = listByYear(year);
        Map<LocalDate, Integer> map = new HashMap<>(list.size() * 2);
        for (HolidayCalendar h : list) {
            map.put(h.getHolidayDate(), h.getType());
        }
        log.info("[holiday-calendar] 加载 {} 年节假日 {} 条", year, list.size());
        return map;
    }

    private void invalidateYearCache(int year) {
        yearCache.remove(year);
    }
}
