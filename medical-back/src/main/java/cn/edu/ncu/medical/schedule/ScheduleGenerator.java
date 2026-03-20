package cn.edu.ncu.medical.schedule;

import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.ScheduleTemplate;
import cn.edu.ncu.medical.entity.SubDepartment;
import cn.edu.ncu.medical.mapper.DoctorDetailMapper;
import cn.edu.ncu.medical.mapper.RegistrationMapper;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.mapper.ScheduleTemplateMapper;
import cn.edu.ncu.medical.mapper.SubDepartmentMapper;
import cn.edu.ncu.medical.utils.RedisCache;
import cn.edu.ncu.medical.utils.ScheduleCacheKeys;
import cn.edu.ncu.medical.utils.ScheduleTimePolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleGenerator {

	public enum GenerateMode {
		FILL_MISSING("fill_missing", false),
		FILL_AND_CLEAN_INVALID("fill_and_clean_invalid", true);

		private final String code;
		private final boolean cleanInvalid;

		GenerateMode(String code, boolean cleanInvalid) {
			this.code = code;
			this.cleanInvalid = cleanInvalid;
		}

		public String getCode() {
			return code;
		}

		public boolean shouldCleanInvalid() {
			return cleanInvalid;
		}

		public static GenerateMode fromCode(String code) {
			if (code == null || code.isBlank()) {
				return FILL_MISSING;
			}
			for (GenerateMode mode : values()) {
				if (mode.code.equalsIgnoreCase(code)) {
					return mode;
				}
			}
			throw new IllegalArgumentException("Unknown generate mode: " + code);
		}
	}

	@Autowired
	private ScheduleTemplateMapper scheduleTemplateMapper;
	@Autowired
	private ScheduleMapper scheduleMapper;
	@Autowired
	private DoctorDetailMapper doctorDetailMapper;
	@Autowired
	private SubDepartmentMapper subDepartmentMapper;
	@Autowired
	private RegistrationMapper registrationMapper;
	@Autowired
	private RedisCache redisCache;

	private final Clock clock = Clock.systemDefaultZone();
	private final ReentrantLock generationLock = new ReentrantLock();

	@Value("${app.schedule.generate-days:7}")
	private int generateDays;
	@Value("${app.schedule.cleanup-past-days:365}")
	private int cleanupPastDays;

	public record GenerationSummary(int createdCount, int cleanedCount) {
	}

	@EventListener(ApplicationReadyEvent.class)
	public void generateOnStartup() {
		generateSchedules("startup", Math.max(1, generateDays), GenerateMode.FILL_MISSING);
	}

	@Scheduled(fixedDelayString = "${app.schedule.generate-interval-ms:3600000}")
	public void generatePeriodically() {
		generateSchedules("periodic", Math.max(1, generateDays), GenerateMode.FILL_MISSING);
	}

	public int generateNow(Integer days) {
		return generateNowSummary(days, GenerateMode.FILL_MISSING).createdCount();
	}

	public GenerationSummary generateNowSummary(Integer days) {
		return generateNowSummary(days, GenerateMode.FILL_MISSING);
	}

	public GenerationSummary generateNowSummary(Integer days, GenerateMode mode) {
		int rangeDays = days == null ? Math.max(1, generateDays) : Math.max(1, days);
		GenerateMode safeMode = mode == null ? GenerateMode.FILL_MISSING : mode;
		return generateSchedules("manual", rangeDays, safeMode);
	}

	private GenerationSummary generateSchedules(String trigger, int rangeDays, GenerateMode mode) {
		int createdCount = 0;
		int cleanedCount = 0;
		generationLock.lock();
		try {
			LocalDate today = LocalDate.now(clock);
			if (mode.shouldCleanInvalid()) {
				LocalDate cleanupFrom = today.minusDays(Math.max(0, cleanupPastDays));
				LocalDate cleanupTo = today.plusDays(rangeDays - 1L);
				cleanedCount = cleanupInvalidSchedules(cleanupFrom, cleanupTo, today);
			}

			for (int i = 0; i < rangeDays; i++) {
				LocalDate date = today.plusDays(i);
				int weekDay = date.getDayOfWeek().getValue(); // 1-7
				List<ScheduleTemplate> templates = scheduleTemplateMapper.findByWeekDay(weekDay);
				if (templates == null || templates.isEmpty()) {
					continue;
				}

				for (ScheduleTemplate template : templates) {
					if (template == null) {
						continue;
					}
					DoctorDetail doctorDetail = doctorDetailMapper.selectById(template.getDoctorId());
					if (doctorDetail == null || doctorDetail.getIsDeleted() != null && doctorDetail.getIsDeleted() != 0) {
						continue;
					}
					SubDepartment subDepartment = subDepartmentMapper.selectById(doctorDetail.getSubDepartmentId());
					if (subDepartment == null || subDepartment.getIsDeleted() != null && subDepartment.getIsDeleted() != 0) {
						continue;
					}

					if (createScheduleIfNeeded(template, date, doctorDetail, subDepartment, true, template.getMorningLimit(), mode)) {
						createdCount++;
					}
					if (createScheduleIfNeeded(template, date, doctorDetail, subDepartment, false, template.getAfternoonLimit(), mode)) {
						createdCount++;
					}
				}
			}
		} catch (Exception e) {
			log.warn("Generate schedule failed (trigger={})", trigger, e);
		} finally {
			generationLock.unlock();
		}
		log.info("Generate schedule completed (trigger={}, mode={}, createdCount={}, cleanedCount={}, days={})",
				trigger, mode.getCode(), createdCount, cleanedCount, rangeDays);
		return new GenerationSummary(createdCount, cleanedCount);
	}

	private int cleanupInvalidSchedules(LocalDate fromDate, LocalDate toDate, LocalDate today) {
		LambdaQueryWrapper<Schedule> scheduleQuery = new LambdaQueryWrapper<>();
		scheduleQuery.eq(Schedule::getIsDeleted, 0)
				.ge(Schedule::getScheduleDate, java.sql.Date.valueOf(fromDate))
				.le(Schedule::getScheduleDate, java.sql.Date.valueOf(toDate))
				.orderByAsc(Schedule::getDoctorId)
				.orderByAsc(Schedule::getScheduleDate)
				.orderByDesc(Schedule::getIsMorning)
				.orderByAsc(Schedule::getId);
		List<Schedule> schedules = scheduleMapper.selectList(scheduleQuery);
		if (schedules == null || schedules.isEmpty()) {
			return 0;
		}

		Set<Long> deletedScheduleIds = new HashSet<>();
		int cleanedCount = 0;
		for (List<Schedule> group : schedules.stream()
				.collect(Collectors.groupingBy(this::buildSlotKey, java.util.LinkedHashMap::new, Collectors.toList()))
				.values()) {
			cleanedCount += cleanupDuplicateSchedules(group, deletedScheduleIds);
		}

		for (Schedule schedule : schedules) {
			if (schedule == null || schedule.getId() == null || deletedScheduleIds.contains(schedule.getId())) {
				continue;
			}
			LocalDate scheduleDate = toLocalDate(schedule);
			if (scheduleDate == null || scheduleDate.isBefore(today)) {
				continue;
			}
			if (hasRegistrations(schedule.getId())) {
				continue;
			}
			if (isFutureScheduleInvalid(schedule, scheduleDate)) {
				if (markScheduleDeleted(schedule)) {
					deletedScheduleIds.add(schedule.getId());
					cleanedCount++;
				}
			}
		}
		return cleanedCount;
	}

	private boolean createScheduleIfNeeded(ScheduleTemplate template,
										LocalDate date,
										DoctorDetail doctorDetail,
										SubDepartment subDepartment,
										boolean isMorning,
										Integer limit,
										GenerateMode mode) {
		if (limit == null || limit <= 0) {
			return false;
		}
		java.sql.Date scheduleDate = java.sql.Date.valueOf(date);
		LambdaQueryWrapper<Schedule> existingQuery = new LambdaQueryWrapper<>();
		existingQuery.eq(Schedule::getDoctorId, doctorDetail.getId())
				.eq(Schedule::getScheduleDate, scheduleDate)
				.eq(Schedule::getIsDeleted, 0)
				.eq(isMorning ? Schedule::getIsMorning : Schedule::getIsAfternoon, 1)
				.orderByAsc(Schedule::getId);
		List<Schedule> existingSchedules = scheduleMapper.selectList(existingQuery);
		if (existingSchedules != null && !existingSchedules.isEmpty()) {
			if (mode.shouldCleanInvalid()) {
				cleanupDuplicateSchedules(existingSchedules, new HashSet<>());
			}
			return false;
		}

		Schedule schedule = new Schedule();
		schedule.setTemplateId(template.getId());
		schedule.setSubDepartmentId(subDepartment.getId());
		schedule.setDepartmentName(subDepartment.getDepartmentName());
		schedule.setDoctorId(doctorDetail.getId());
		schedule.setDoctorName(doctorDetail.getRealName());
		schedule.setScheduleDate(scheduleDate);
		schedule.setIsMorning(isMorning ? 1 : 0);
		schedule.setIsAfternoon(isMorning ? 0 : 1);
		schedule.setStatus(1);
		schedule.setCurrentAppointmentCount(0);
		schedule.setAppointmentLimit(limit);
		schedule.setCreateTime(new Date());
		scheduleMapper.insert(schedule);
		redisCache.delete(ScheduleCacheKeys.scheduleListKey(subDepartment.getId(), scheduleDate));
		return true;
	}

	private int cleanupDuplicateSchedules(List<Schedule> existingSchedules, Set<Long> deletedScheduleIds) {
		if (existingSchedules == null || existingSchedules.size() <= 1) {
			return 0;
		}
		Schedule keepSchedule = existingSchedules.stream()
				.filter(schedule -> schedule != null && schedule.getId() != null && hasRegistrations(schedule.getId()))
				.findFirst()
				.orElse(existingSchedules.get(0));
		List<Long> duplicateIds = existingSchedules.stream()
				.filter(schedule -> schedule != null
						&& schedule.getId() != null
						&& !Objects.equals(schedule.getId(), keepSchedule.getId())
						&& !hasRegistrations(schedule.getId()))
				.map(Schedule::getId)
				.collect(Collectors.toList());
		if (duplicateIds.isEmpty()) {
			return 0;
		}

		LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.in(Schedule::getId, duplicateIds)
				.set(Schedule::getIsDeleted, 1);
		scheduleMapper.update(null, updateWrapper);
		for (Schedule schedule : existingSchedules) {
			if (schedule != null && schedule.getId() != null && duplicateIds.contains(schedule.getId())) {
				deletedScheduleIds.add(schedule.getId());
			}
		}
		clearScheduleCache(keepSchedule);
		log.warn("Deduplicated schedules, keepId={}, removedIds={}", keepSchedule.getId(), duplicateIds);
		return duplicateIds.size();
	}

	private boolean isFutureScheduleInvalid(Schedule schedule, LocalDate scheduleDate) {
		DoctorDetail doctorDetail = doctorDetailMapper.selectById(schedule.getDoctorId());
		if (doctorDetail == null || isLogicDeleted(doctorDetail.getIsDeleted())) {
			return true;
		}
		SubDepartment subDepartment = subDepartmentMapper.selectById(doctorDetail.getSubDepartmentId());
		if (subDepartment == null || isLogicDeleted(subDepartment.getIsDeleted())) {
			return true;
		}

		ScheduleTemplate template = schedule.getTemplateId() == null ? null : scheduleTemplateMapper.selectById(schedule.getTemplateId());
		if (template == null) {
			return !hasMatchingActiveTemplate(schedule, scheduleDate, doctorDetail, subDepartment);
		}
		if (isLogicDeleted(template.getIsDeleted()) || !Objects.equals(template.getIsActive(), 1)) {
			return true;
		}
		if (!Objects.equals(template.getDoctorId(), doctorDetail.getId())) {
			return true;
		}
		if (!Objects.equals(template.getWeekDay(), scheduleDate.getDayOfWeek().getValue())) {
			return true;
		}
		if (!Objects.equals(schedule.getSubDepartmentId(), subDepartment.getId())) {
			return true;
		}
		if (!Objects.equals(schedule.getDepartmentName(), subDepartment.getDepartmentName())) {
			return true;
		}
		if (!Objects.equals(schedule.getDoctorName(), doctorDetail.getRealName())) {
			return true;
		}

		boolean morningSlot = isMorningSlot(schedule);
		boolean afternoonSlot = isAfternoonSlot(schedule);
		if (morningSlot == afternoonSlot) {
			return true;
		}
		Integer expectedLimit = morningSlot ? template.getMorningLimit() : template.getAfternoonLimit();
		if (expectedLimit == null || expectedLimit <= 0) {
			return true;
		}
		return !Objects.equals(schedule.getAppointmentLimit(), expectedLimit);
	}

	private boolean hasMatchingActiveTemplate(Schedule schedule,
											LocalDate scheduleDate,
											DoctorDetail doctorDetail,
											SubDepartment subDepartment) {
		List<ScheduleTemplate> templates = scheduleTemplateMapper.findByWeekDay(scheduleDate.getDayOfWeek().getValue());
		if (templates == null || templates.isEmpty()) {
			return false;
		}
		return templates.stream().anyMatch(template -> matchesTemplate(template, schedule, doctorDetail, subDepartment));
	}

	private boolean matchesTemplate(ScheduleTemplate template,
									Schedule schedule,
									DoctorDetail doctorDetail,
									SubDepartment subDepartment) {
		if (template == null || !Objects.equals(template.getDoctorId(), doctorDetail.getId())) {
			return false;
		}
		if (!Objects.equals(schedule.getSubDepartmentId(), subDepartment.getId())) {
			return false;
		}
		if (!Objects.equals(schedule.getDepartmentName(), subDepartment.getDepartmentName())) {
			return false;
		}
		if (!Objects.equals(schedule.getDoctorName(), doctorDetail.getRealName())) {
			return false;
		}
		if (isMorningSlot(schedule)) {
			return template.getMorningLimit() != null && template.getMorningLimit() > 0
					&& Objects.equals(schedule.getAppointmentLimit(), template.getMorningLimit());
		}
		if (isAfternoonSlot(schedule)) {
			return template.getAfternoonLimit() != null && template.getAfternoonLimit() > 0
					&& Objects.equals(schedule.getAppointmentLimit(), template.getAfternoonLimit());
		}
		return false;
	}

	private boolean hasRegistrations(Long scheduleId) {
		if (scheduleId == null) {
			return false;
		}
		LambdaQueryWrapper<Registration> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(Registration::getScheduleId, scheduleId)
				.eq(Registration::getIsDeleted, 0);
		return registrationMapper.selectCount(queryWrapper) > 0;
	}

	private boolean markScheduleDeleted(Schedule schedule) {
		if (schedule == null || schedule.getId() == null) {
			return false;
		}
		LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(Schedule::getId, schedule.getId())
				.eq(Schedule::getIsDeleted, 0)
				.set(Schedule::getIsDeleted, 1);
		int updated = scheduleMapper.update(null, updateWrapper);
		if (updated > 0) {
			clearScheduleCache(schedule);
			log.info("Marked invalid schedule deleted, scheduleId={}", schedule.getId());
			return true;
		}
		return false;
	}

	private void clearScheduleCache(Schedule schedule) {
		if (schedule == null || schedule.getSubDepartmentId() == null || schedule.getScheduleDate() == null) {
			return;
		}
		redisCache.delete(ScheduleCacheKeys.scheduleListKey(schedule.getSubDepartmentId(), schedule.getScheduleDate()));
	}

	private String buildSlotKey(Schedule schedule) {
		String period = isMorningSlot(schedule) ? "AM" : isAfternoonSlot(schedule) ? "PM" : "UNKNOWN";
		return schedule.getDoctorId() + "|" + schedule.getScheduleDate() + "|" + period;
	}

	private LocalDate toLocalDate(Schedule schedule) {
		if (schedule == null || schedule.getScheduleDate() == null) {
			return null;
		}
		return ScheduleTimePolicy.toLocalDate(schedule.getScheduleDate(), clock.getZone());
	}

	private boolean isMorningSlot(Schedule schedule) {
		return schedule != null && Objects.equals(schedule.getIsMorning(), 1);
	}

	private boolean isAfternoonSlot(Schedule schedule) {
		return schedule != null && Objects.equals(schedule.getIsAfternoon(), 1);
	}

	private boolean isLogicDeleted(Integer isDeleted) {
		return isDeleted != null && isDeleted != 0;
	}
}
