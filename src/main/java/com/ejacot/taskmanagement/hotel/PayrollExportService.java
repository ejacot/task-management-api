package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PayrollExportService {
    private final UserAccountRepository users; private final WorkLogRepository logs; private final PayRateRepository rates;
    public PayrollExportService(UserAccountRepository users, WorkLogRepository logs, PayRateRepository rates) { this.users = users; this.logs = logs; this.rates = rates; }
    public ResponseEntity<byte[]> csv(String username, int year, int month) {
        UserAccount actor = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (actor.getRole() != UserRole.EMPLOYER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        LocalDate from = LocalDate.of(year, month, 1), to = from.withDayOfMonth(from.lengthOfMonth());
        List<WorkLog> values = logs.findAllByHotelIdAndStatusOrderByWorkDateDesc(actor.getHotel().getId(), LogStatus.APPROVED)
                .stream().filter(log -> !log.getWorkDate().isBefore(from) && !log.getWorkDate().isAfter(to)).toList();
        StringBuilder csv = new StringBuilder("employee,date,task,hours,rate,gross,status\n");
        for (WorkLog log : values) {
            BigDecimal rate = rates.findFirstByEmployeeUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(log.getEmployee().getUsername(), log.getWorkDate()).map(PayRate::getHourlyRate).orElse(log.getEmployee().getHourlyRate());
            csv.append(safe(log.getEmployee().getUsername())).append(',').append(log.getWorkDate()).append(',').append(safe(log.getWorkType().getName())).append(',').append(log.getCalculatedHours()).append(',').append(rate).append(',').append(log.getCalculatedHours().multiply(rate).setScale(2, RoundingMode.HALF_UP)).append(',').append(log.getStatus()).append('\n');
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payroll-" + year + "-" + month + ".csv").contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    private String safe(String text) { return "\"" + (text == null ? "" : text.replace("\"", "\"\"")) + "\""; }
}
