package com.ejacot.taskmanagement.personal;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
public class PersonalWorkController {
    private final PersonalWorkService service;

    public PersonalWorkController(PersonalWorkService service) {
        this.service = service;
    }

    @GetMapping("/api/app/bootstrap")
    public PersonalDtos.AppBootstrap bootstrap(Authentication authentication) {
        return service.bootstrap(authentication.getName(), LocalDate.now());
    }

    @GetMapping("/api/work-entries/today")
    public PersonalDtos.DayEntries today(Authentication authentication, @RequestParam(required = false) LocalDate date) {
        return service.dayEntries(authentication.getName(), date == null ? LocalDate.now() : date);
    }

    @GetMapping("/api/work-entries")
    public PersonalDtos.EntryList entries(Authentication authentication,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) Integer month,
                                          @RequestParam(required = false) LocalDate from,
                                          @RequestParam(required = false) LocalDate to,
                                          @RequestParam(required = false) Long workTypeId) {
        return service.entries(authentication.getName(), year, month, from, to, workTypeId);
    }

    @GetMapping("/api/work-entries/calendar")
    public PersonalDtos.CalendarMonth calendar(Authentication authentication,
                                               @RequestParam int year,
                                               @RequestParam int month) {
        return service.calendar(authentication.getName(), year, month);
    }

    @PostMapping("/api/work-entries")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalDtos.WorkEntryView createEntry(Authentication authentication, @Valid @RequestBody PersonalDtos.WorkEntryRequest request) {
        return service.createEntry(authentication.getName(), request);
    }

    @PutMapping("/api/work-entries/{id}")
    public PersonalDtos.WorkEntryView updateEntry(Authentication authentication, @PathVariable Long id, @Valid @RequestBody PersonalDtos.WorkEntryRequest request) {
        return service.updateEntry(authentication.getName(), id, request);
    }

    @DeleteMapping("/api/work-entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(Authentication authentication, @PathVariable Long id) {
        service.deleteEntry(authentication.getName(), id);
    }

    @PostMapping("/api/work-entries/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalDtos.WorkEntryView duplicateEntry(Authentication authentication, @PathVariable Long id, @Valid @RequestBody PersonalDtos.DuplicateRequest request) {
        return service.duplicateEntry(authentication.getName(), id, request.date());
    }

    @GetMapping("/api/work-types")
    public java.util.List<PersonalDtos.WorkTypeView> workTypes(Authentication authentication) {
        return service.workTypes(authentication.getName());
    }

    @PostMapping("/api/work-types")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalDtos.WorkTypeView createWorkType(Authentication authentication, @Valid @RequestBody PersonalDtos.WorkTypeRequest request) {
        return service.createWorkType(authentication.getName(), request);
    }

    @PutMapping("/api/work-types/{id}")
    public PersonalDtos.WorkTypeView updateWorkType(Authentication authentication, @PathVariable Long id, @Valid @RequestBody PersonalDtos.WorkTypeRequest request) {
        return service.updateWorkType(authentication.getName(), id, request);
    }

    @PutMapping("/api/work-types/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateWorkType(Authentication authentication, @PathVariable Long id) {
        service.deactivateWorkType(authentication.getName(), id);
    }

    @GetMapping("/api/statistics/summary")
    public PersonalDtos.PeriodSummary summary(Authentication authentication,
                                              @RequestParam(required = false) Integer year,
                                              @RequestParam(required = false) Integer month,
                                              @RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to,
                                              @RequestParam(required = false) Long workTypeId) {
        return service.summary(authentication.getName(), year, month, from, to, workTypeId);
    }

    @GetMapping("/api/statistics/monthly")
    public PersonalDtos.MonthlySeries monthly(Authentication authentication,
                                              @RequestParam int year,
                                              @RequestParam(defaultValue = "hours") String metric,
                                              @RequestParam(required = false) Long workTypeId) {
        return service.monthly(authentication.getName(), year, metric, workTypeId);
    }

    @GetMapping("/api/statistics/compare")
    public PersonalDtos.Comparison compare(Authentication authentication,
                                           @RequestParam LocalDate fromA,
                                           @RequestParam LocalDate toA,
                                           @RequestParam LocalDate fromB,
                                           @RequestParam LocalDate toB,
                                           @RequestParam(defaultValue = "hours") String metric,
                                           @RequestParam(required = false) Long workTypeId) {
        return service.compare(authentication.getName(), fromA, toA, fromB, toB, metric, workTypeId);
    }

    @GetMapping("/api/export/csv")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication,
                                            @RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Integer month,
                                            @RequestParam(required = false) LocalDate from,
                                            @RequestParam(required = false) LocalDate to) {
        byte[] body = service.exportCsv(authentication.getName(), year, month, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("roomly-export.csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/api/export/excel")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication,
                                              @RequestParam(required = false) Integer year,
                                              @RequestParam(required = false) Integer month,
                                              @RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to) {
        byte[] body = service.exportExcel(authentication.getName(), year, month, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("roomly-export.xlsx").build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
