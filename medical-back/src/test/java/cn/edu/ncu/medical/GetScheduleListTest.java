package cn.edu.ncu.medical;

import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@SpringBootTest
public class GetScheduleListTest {
    @Autowired
    private ScheduleService scheduleService;

    @Test
    public void getScheduleListByDoctorId() {

        List<Schedule> scheduleList = scheduleService.getScheduleListByDoctorId(131L);
        System.out.println(scheduleList);


        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        System.out.println(monday);
        System.out.println(sunday);
    }
}
