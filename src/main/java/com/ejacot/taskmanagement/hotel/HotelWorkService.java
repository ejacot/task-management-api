package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class HotelWorkService {
    private final UserAccountRepository users;
    private final WorkTypeRepository workTypes;
    private final ShiftPlanRepository plans;
    private final WorkLogRepository logs;
    private final NotificationRepository notifications;
    private final PayRateRepository payRates;
    private final PlanLogService planLogs;

    public HotelWorkService(UserAccountRepository users, WorkTypeRepository workTypes,
                            ShiftPlanRepository plans, WorkLogRepository logs,NotificationRepository notifications,PayRateRepository payRates,PlanLogService planLogs) {
        this.users = users; this.workTypes = workTypes; this.plans = plans; this.logs = logs;this.notifications=notifications;this.payRates=payRates;this.planLogs=planLogs;
    }

    @Transactional(readOnly = true)
    public HotelDtos.Bootstrap bootstrap(String username) {
        UserAccount user = user(username);
        if (user.getHotel() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not assigned to a hotel");
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<WorkLog> monthLogs = logs.findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateDesc(username, monthStart, today);
        List<WorkLog> historyLogs = logs.findAllByEmployeeUsernameOrderByWorkDateDesc(username);
        BigDecimal hours = monthLogs.stream().map(WorkLog::getCalculatedHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rate=payRates.findFirstByEmployeeUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(username,today).map(PayRate::getHourlyRate).orElse(user.getHourlyRate());
        BigDecimal gross = hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedNet = gross.multiply(new BigDecimal("0.72")).setScale(2, RoundingMode.HALF_UP);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        return new HotelDtos.Bootstrap(
                HotelDtos.Me.from(user),
                HotelDtos.HotelView.from(user.getHotel()),
                workTypes.findAllByHotelIdAndActiveTrueOrderByName(user.getHotel().getId()).stream().map(HotelDtos.WorkTypeView::from).toList(),
                plans.findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(username, weekStart.minusWeeks(26), weekStart.plusWeeks(26).plusDays(6)).stream().map(HotelDtos.PlanView::from).toList(),
                historyLogs.stream().map(HotelDtos.LogView::from).toList(),
                notifications.findTop20ByRecipientUsernameOrderByCreatedAtDesc(username).stream().map(HotelDtos.NotificationView::from).toList(),
                new HotelDtos.Metrics(hours, gross, estimatedNet, monthLogs.stream().mapToInt(log -> log.getQuantity() == null ? 0 : log.getQuantity()).sum())
        );
    }

    public HotelDtos.LogView createLog(String username, HotelDtos.CreateLog request) {
        UserAccount user = user(username);
        WorkType type = workTypes.findById(request.workTypeId())
                .filter(item -> item.getHotel().getId().equals(user.getHotel().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work type not found"));
        BigDecimal hours = calculateHours(type, request);
        WorkLog log = new WorkLog(user, user.getHotel(), type, request.workDate(), request.startTime(), request.endTime(),
                request.breakMinutes() == null ? 0 : request.breakMinutes(), request.roomType(), request.quantity(), hours, request.notes());
        if(type.getUnit()==WorkUnit.ROOMS&&(value(request.normalRooms())+value(request.juniorRooms())+value(request.presidentRooms())>0||request.attachmentData()!=null)) log.setRoomBreakdown(value(request.normalRooms()),value(request.juniorRooms()),value(request.presidentRooms()),request.attachmentName(),request.attachmentData());
        return HotelDtos.LogView.from(logs.save(log));
    }

    public HotelDtos.LogView submit(String username, Long id) {
        WorkLog log = logs.findByIdAndEmployeeUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work log not found"));
        log.submit();
        return HotelDtos.LogView.from(log);
    }

    public HotelDtos.LogView correctPlannedLog(String username,Long id,HotelDtos.CorrectPlannedLog request){
        WorkLog log=logs.findByIdAndEmployeeUsername(id,username).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Work log not found"));
        if(log.getShiftPlan()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Activitatea nu provine din plan");
        int breakMinutes=log.getWorkType().getUnit()==WorkUnit.ROOMS?0:(request.breakMinutes()==null?0:request.breakMinutes());
        BigDecimal hours;
        try{hours=planLogs.hours(request.startTime(),request.endTime(),breakMinutes);}catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ex.getMessage());}
        log.requestCorrection(request.startTime(),request.endTime(),breakMinutes,hours,request.reason());
        return HotelDtos.LogView.from(log);
    }

    private BigDecimal calculateHours(WorkType type, HotelDtos.CreateLog request) {
        if (type.getUnit() == WorkUnit.ROOMS) {
            int total=value(request.normalRooms())+value(request.juniorRooms())+value(request.presidentRooms());
            if(total>0){Hotel h=type.getHotel();return BigDecimal.valueOf(value(request.normalRooms())).divide(h.getNormalRoomsPerHour(),2,RoundingMode.HALF_UP).add(BigDecimal.valueOf(value(request.juniorRooms())).divide(h.getJuniorRoomsPerHour(),2,RoundingMode.HALF_UP)).add(BigDecimal.valueOf(value(request.presidentRooms())).divide(h.getPresidentRoomsPerHour(),2,RoundingMode.HALF_UP));}
            if (request.quantity() == null || request.quantity() < 1 || type.getRoomsPerHour() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required for room work");
            }
            return BigDecimal.valueOf(request.quantity()).divide(type.getRoomsPerHour(), 2, RoundingMode.HALF_UP);
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end times are required");
        }
        long minutes = ChronoUnit.MINUTES.between(request.startTime(), request.endTime());
        minutes -= request.breakMinutes() == null ? 0 : request.breakMinutes();
        if (minutes <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
    public void readNotification(String username,Long id){Notification n=notifications.findById(id).filter(item->notifications.findTop20ByRecipientUsernameOrderByCreatedAtDesc(username).stream().anyMatch(x->x.getId().equals(item.getId()))).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));n.markRead();}
    private int value(Integer number){return number==null?0:number;}

    private UserAccount user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}

