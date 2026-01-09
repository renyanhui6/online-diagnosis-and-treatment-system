package cn.edu.ncu.medical.utils;

import java.util.Date;

/**
 * 排班相关缓存 Key 生成（统一口径，避免各处 String.join 不一致）。
 */
public final class ScheduleCacheKeys {

	private static final String PREFIX_SCHEDULE_LIST = "schedule:list";

	private ScheduleCacheKeys() {
	}

	/**
	 * 患者端“某子科室某天的排班列表”缓存 Key。
	 */
	public static String scheduleListKey(Long subDepartmentId, Date scheduleDate) {
		String dateStr = TimeUtil.dateToString(scheduleDate);
		return PREFIX_SCHEDULE_LIST + ":" + subDepartmentId + ":" + dateStr;
	}
}

