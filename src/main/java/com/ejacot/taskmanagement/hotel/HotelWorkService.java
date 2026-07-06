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

    public HotelWorkService(UserAccountRepository users, WorkTypeRepository workTypes,
                            ShiftPlanRepository plans, WorkLogRepository logs) {
        this.users = users; this.workTypes = workTypes; this.plans = plans; this.logs = logs;
    }

    @Transactional(readOnly = true)
    public HotelDtos.Bootstrap bootstrap(String username) {
        UserAccount user = user(username);
        if (user.getHotel() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not assigned to a hotel");
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<WorkLog> monthLogs = logs.findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateDesc(username, monthStart, today);
        BigDecimal hours = monthLogs.stream().map(WorkLog::getCalculatedHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = hours.multiply(user.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedNet = gross.multiply(new BigDecimal("0.72")).setScale(2, RoundingMode.HALF_UP);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        return new HotelDtos.Bootstrap(
                HotelDtos.Me.from(user),
                new HotelDtos.HotelView(user.getHotel().getId(), user.getHotel().getName(), user.getHotel().getCity()),
                workTypes.findAllByHotelIdAndActiveTrueOrderByName(user.getHotel().getId()).stream().map(HotelDtos.WorkTypeView::from).toList(),
                plans.findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(username, weekStart, weekStart.plusDays(13)).stream().map(HotelDtos.PlanView::from).toList(),
                monthLogs.stream().map(HotelDtos.LogView::from).toList(),
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
        return HotelDtos.LogView.from(logs.save(log));
    }

    public HotelDtos.LogView submit(String username, Long id) {
        WorkLog log = logs.findByIdAndEmployeeUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work log not found"));
        log.submit();
        return HotelDtos.LogView.from(log);
    }

    private BigDecimal calculateHours(WorkType type, HotelDtos.CreateLog request) {
        if (type.getUnit() == WorkUnit.ROOMS) {
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

    private UserAccount user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}

