package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.entity.Department;
import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.SubDepartment;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.service.DepartmentService;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.SubDepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TriageDepartmentTools {

    private final DepartmentService departmentService;
    private final SubDepartmentService subDepartmentService;
    private final DoctorDetailService doctorDetailService;
    private final ScheduleMapper scheduleMapper;

    @Tool("""
            查询医院系统内真实存在的一级科室、子科室、诊疗范围、科室特色、医生人数和未来7天排班概况。
            当需要做挂号分诊推荐时，应优先使用这个工具。
            """)
    public String queryHospitalDepartments(String symptomHint) {
        List<Department> departments = departmentService.list();
        List<SubDepartment> subDepartments = subDepartmentService.list();
        List<DoctorDetail> doctors = doctorDetailService.list();
        List<Schedule> futureSchedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .ge(Schedule::getScheduleDate, Date.valueOf(LocalDate.now()))
                .le(Schedule::getScheduleDate, Date.valueOf(LocalDate.now().plusDays(7))));

        Map<Long, Department> departmentMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Function.identity(), (a, b) -> a));
        Map<Long, Long> doctorCountMap = doctors.stream()
                .filter(doctor -> doctor.getSubDepartmentId() != null)
                .collect(Collectors.groupingBy(DoctorDetail::getSubDepartmentId, Collectors.counting()));
        Map<Long, Long> scheduleCountMap = futureSchedules.stream()
                .filter(schedule -> schedule.getSubDepartmentId() != null)
                .collect(Collectors.groupingBy(Schedule::getSubDepartmentId, Collectors.counting()));

        List<SubDepartment> ordered = subDepartments.stream()
                .sorted(Comparator.comparing(SubDepartment::getParentDepartmentId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(SubDepartment::getDepartmentName, Comparator.nullsLast(String::compareTo)))
                .toList();

        Set<String> hintTokens = tokenize(symptomHint);

        StringBuilder builder = new StringBuilder();
        if (!hintTokens.isEmpty()) {
            builder.append("用户症状提示词：").append(String.join("、", hintTokens)).append("\n");
        }
        builder.append("医院真实科室信息如下：\n");
        for (SubDepartment subDepartment : ordered) {
            Department parent = departmentMap.get(subDepartment.getParentDepartmentId());
            long doctorCount = doctorCountMap.getOrDefault(subDepartment.getId(), 0L);
            long scheduleCount = scheduleCountMap.getOrDefault(subDepartment.getId(), 0L);
            builder.append("- 一级科室：")
                    .append(parent == null ? "未知" : safe(parent.getDepartmentName()))
                    .append("；子科室：").append(safe(subDepartment.getDepartmentName()))
                    .append("；诊疗范围：").append(safe(subDepartment.getTreatmentScope()))
                    .append("；科室特色：").append(safe(subDepartment.getDepartmentFeatures()))
                    .append("；描述：").append(safe(subDepartment.getDescription()))
                    .append("；医生数：").append(doctorCount)
                    .append("；未来7天排班数：").append(scheduleCount)
                    .append("\n");
        }
        return builder.toString();
    }

    private Set<String> tokenize(String input) {
        if (!StringUtils.hasText(input)) {
            return Set.of();
        }
        return List.of(input.split("[，,。；;、\\s]+")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(token -> token.length() >= 2)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "未填写";
    }
}
