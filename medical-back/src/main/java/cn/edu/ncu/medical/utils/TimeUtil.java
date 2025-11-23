package cn.edu.ncu.medical.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TimeUtil {
	public static Date getZeroDate(){
		LocalDateTime todayStartLdt = LocalDateTime.now()
				.withHour(0)
				.withMinute(0)
				.withSecond(0)
				.withNano(0); // 纳秒级精度清零

		// 转换为Date对象（需要指定时区，通常用系统默认时区）
		return Date.from(
				todayStartLdt.atZone(ZoneId.systemDefault()).toInstant()
		);

	}
	public static String dateToString(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(date);
	}
}
