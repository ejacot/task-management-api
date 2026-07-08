package com.ejacot.taskmanagement.hotel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class PlanLogService {
    private final WorkLogRepository logs;
    private final ShiftPlanRepository plans;
    public PlanLogService(WorkLogRepository logs,ShiftPlanRepository plans){this.logs=logs;this.plans=plans;}

    public WorkLog ensureForId(Long planId){return ensureFor(plans.findById(planId).orElseThrow());}

    public WorkLog ensureFor(ShiftPlan plan){
        if(plan.getKind()!=ShiftKind.WORK||plan.getWorkType()==null||plan.getStartTime()==null||plan.getEndTime()==null){logs.findByShiftPlanId(plan.getId()).ifPresent(logs::delete);return null;}
        int breakMinutes=breakMinutes(plan);
        BigDecimal hours=hours(plan.getStartTime(),plan.getEndTime(),breakMinutes);
        return logs.findByShiftPlanId(plan.getId()).map(existing->{existing.syncFromPlan(hours,breakMinutes);return existing;})
                .orElseGet(()->logs.save(new WorkLog(plan,hours,breakMinutes)));
    }

    public int breakMinutes(ShiftPlan plan){
        if(plan.getWorkType().getUnit()==WorkUnit.ROOMS)return 0;
        if(plan.getWorkType().getDefaultBreakMinutes()>0)return plan.getWorkType().getDefaultBreakMinutes();
        long minutes=ChronoUnit.MINUTES.between(plan.getStartTime(),plan.getEndTime());
        return minutes>=510?30:0;
    }

    public BigDecimal plannedHours(ShiftPlan plan){return hours(plan.getStartTime(),plan.getEndTime(),breakMinutes(plan));}
    public BigDecimal hours(LocalTime start,LocalTime end,int breakMinutes){
        long minutes=ChronoUnit.MINUTES.between(start,end)-breakMinutes;
        if(minutes<=0)throw new IllegalArgumentException("Intervalul de lucru nu este valid");
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP);
    }
}
