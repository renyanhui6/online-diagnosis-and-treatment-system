package cn.edu.ncu.medical.utils;

import cn.edu.ncu.medical.entity.Schedule;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleTimePolicyTest {

    @Test
    void resolveSessionShouldReturnMorningOrAfternoon() {
        assertEquals(ScheduleTimePolicy.Session.MORNING, ScheduleTimePolicy.resolveSession(schedule(1, 0)));
        assertEquals(ScheduleTimePolicy.Session.AFTERNOON, ScheduleTimePolicy.resolveSession(schedule(0, 1)));
    }

    @Test
    void resolveSessionShouldRejectInvalidFlags() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleTimePolicy.resolveSession(schedule(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> ScheduleTimePolicy.resolveSession(schedule(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> ScheduleTimePolicy.resolveSession(null));
    }

    @Test
    void canCreateOrderShouldMatchSessionStartBoundary() {
        Schedule morning = scheduleWithDate(1, 0, LocalDate.of(2026, 4, 1));
        Clock beforeStart = fixedClock("2026-04-01T07:59:00", "Asia/Shanghai");
        Clock afterStart = fixedClock("2026-04-01T08:00:00", "Asia/Shanghai");

        assertTrue(ScheduleTimePolicy.canCreateOrder(morning, beforeStart));
        assertFalse(ScheduleTimePolicy.canCreateOrder(morning, afterStart));
    }

    @Test
    void isSessionStartedAndEndedShouldMatchMorningWindow() {
        Schedule morning = scheduleWithDate(1, 0, LocalDate.of(2026, 4, 1));

        assertFalse(ScheduleTimePolicy.isSessionStarted(morning, fixedClock("2026-04-01T07:59:00", "Asia/Shanghai")));
        assertTrue(ScheduleTimePolicy.isSessionStarted(morning, fixedClock("2026-04-01T08:00:00", "Asia/Shanghai")));
        assertFalse(ScheduleTimePolicy.isSessionEnded(morning, fixedClock("2026-04-01T11:59:00", "Asia/Shanghai")));
        assertTrue(ScheduleTimePolicy.isSessionEnded(morning, fixedClock("2026-04-01T12:00:00", "Asia/Shanghai")));
    }

    @Test
    void isSessionStartedAndEndedShouldMatchAfternoonWindow() {
        Schedule afternoon = scheduleWithDate(0, 1, LocalDate.of(2026, 4, 1));

        assertFalse(ScheduleTimePolicy.isSessionStarted(afternoon, fixedClock("2026-04-01T13:59:00", "Asia/Shanghai")));
        assertTrue(ScheduleTimePolicy.isSessionStarted(afternoon, fixedClock("2026-04-01T14:00:00", "Asia/Shanghai")));
        assertFalse(ScheduleTimePolicy.isSessionEnded(afternoon, fixedClock("2026-04-01T17:59:00", "Asia/Shanghai")));
        assertTrue(ScheduleTimePolicy.isSessionEnded(afternoon, fixedClock("2026-04-01T18:00:00", "Asia/Shanghai")));
    }

    private static Schedule schedule(int isMorning, int isAfternoon) {
        Schedule schedule = new Schedule();
        schedule.setIsMorning(isMorning);
        schedule.setIsAfternoon(isAfternoon);
        return schedule;
    }

    private static Schedule scheduleWithDate(int isMorning, int isAfternoon, LocalDate date) {
        Schedule schedule = schedule(isMorning, isAfternoon);
        schedule.setScheduleDate(java.sql.Date.valueOf(date));
        return schedule;
    }

    private static Clock fixedClock(String localDateTime, String zoneId) {
        ZoneId zone = ZoneId.of(zoneId);
        return Clock.fixed(LocalDateTime.parse(localDateTime).atZone(zone).toInstant(), zone);
    }
}
