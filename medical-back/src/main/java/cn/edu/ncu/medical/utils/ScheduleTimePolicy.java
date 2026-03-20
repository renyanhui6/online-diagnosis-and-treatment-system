package cn.edu.ncu.medical.utils;

import cn.edu.ncu.medical.entity.Schedule;

import java.time.*;
import java.util.Date;

/**
 * 排班“时间规则”统一封装：避免各处用 Calendar/LocalTime 写一遍，导致规则不一致。
 */
public final class ScheduleTimePolicy {

	public enum Session {
		MORNING,
		AFTERNOON
	}

	private static final LocalTime MORNING_START = LocalTime.of(8, 0);
	private static final LocalTime MORNING_END = LocalTime.of(12, 0);
	private static final LocalTime AFTERNOON_START = LocalTime.of(14, 0);
	private static final LocalTime AFTERNOON_END = LocalTime.of(18, 0);

	private ScheduleTimePolicy() {
	}

	public static Session resolveSession(Schedule schedule) {
		if (schedule == null) {
			throw new IllegalArgumentException("schedule is null");
		}
		Integer isMorning = schedule.getIsMorning();
		Integer isAfternoon = schedule.getIsAfternoon();
		boolean morning = isMorning != null && isMorning == 1;
		boolean afternoon = isAfternoon != null && isAfternoon == 1;
		if (morning == afternoon) {
			throw new IllegalArgumentException("invalid schedule session flags, isMorning=" + isMorning + ", isAfternoon=" + isAfternoon);
		}
		return morning ? Session.MORNING : Session.AFTERNOON;
	}

	public static LocalDate toLocalDate(Date date, ZoneId zoneId) {
		if (date == null) {
			throw new IllegalArgumentException("date is null");
		}
		if (date instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		return date.toInstant().atZone(zoneId).toLocalDate();
	}

	public static LocalDateTime sessionStart(LocalDate date, Session session) {
		return date.atTime(session == Session.MORNING ? MORNING_START : AFTERNOON_START);
	}

	public static LocalDateTime sessionEnd(LocalDate date, Session session) {
		return date.atTime(session == Session.MORNING ? MORNING_END : AFTERNOON_END);
	}

	/**
	 * 当前实现口径：需要在“开诊时间”之前完成挂号创建。
	 */
	public static boolean canCreateOrder(Schedule schedule, Clock clock) {
		if (schedule == null || schedule.getScheduleDate() == null) {
			return false;
		}
		Session session;
		try {
			session = resolveSession(schedule);
		} catch (IllegalArgumentException e) {
			return false;
		}
		ZoneId zoneId = clock.getZone();
		LocalDate scheduleDate = toLocalDate(schedule.getScheduleDate(), zoneId);
		LocalDateTime start = sessionStart(scheduleDate, session);
		LocalDateTime now = LocalDateTime.now(clock);
		return now.isBefore(start);
	}

	public static boolean isSessionStarted(Schedule schedule, Clock clock) {
		if (schedule == null || schedule.getScheduleDate() == null) {
			return false;
		}
		Session session = resolveSession(schedule);
		ZoneId zoneId = clock.getZone();
		LocalDate scheduleDate = toLocalDate(schedule.getScheduleDate(), zoneId);
		LocalDateTime start = sessionStart(scheduleDate, session);
		LocalDateTime now = LocalDateTime.now(clock);
		return !now.isBefore(start);
	}

	public static boolean isSessionEnded(Schedule schedule, Clock clock) {
		if (schedule == null || schedule.getScheduleDate() == null) {
			return false;
		}
		Session session = resolveSession(schedule);
		ZoneId zoneId = clock.getZone();
		LocalDate scheduleDate = toLocalDate(schedule.getScheduleDate(), zoneId);
		LocalDateTime end = sessionEnd(scheduleDate, session);
		LocalDateTime now = LocalDateTime.now(clock);
		return !now.isBefore(end);
	}
}
