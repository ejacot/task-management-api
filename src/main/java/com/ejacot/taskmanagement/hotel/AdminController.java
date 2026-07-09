package com.ejacot.taskmanagement.hotel;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/admin") @PreAuthorize("hasAnyRole('MANAGER','EMPLOYER')")
public class AdminController {
 private final AdminService service;private final ExcelImportService excel;private final PayrollExportService payroll;public AdminController(AdminService service,ExcelImportService excel,PayrollExportService payroll){this.service=service;this.excel=excel;this.payroll=payroll;}
 @GetMapping("/employees") public AdminDtos.EmployeeList employees(Authentication a){return service.employees(a.getName());}
 @PostMapping("/employees") @ResponseStatus(HttpStatus.CREATED) public AdminDtos.EmployeeAdminView create(Authentication a,@Valid @RequestBody AdminDtos.EmployeeUpsert r){return service.createEmployee(a.getName(),r);}
 @PutMapping("/employees/{id}") public AdminDtos.EmployeeAdminView update(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminDtos.EmployeeUpsert r){return service.updateEmployee(a.getName(),id,r);}
 @PutMapping("/employees/{id}/active") public AdminDtos.EmployeeAdminView active(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminDtos.ActiveRequest r){return service.setActive(a.getName(),id,r);}
 @PostMapping("/employees/{id}/invite") public AdminDtos.InvitationView invite(Authentication a,@PathVariable Long id){return service.invite(a.getName(),id);}
 @GetMapping("/hotel-settings") public AdminDtos.HotelSettings hotel(Authentication a){return service.hotelSettings(a.getName());}
 @PutMapping("/hotel-settings") @PreAuthorize("hasRole('EMPLOYER')") public AdminDtos.HotelSettings hotel(Authentication a,@Valid @RequestBody AdminDtos.HotelSettings r){return service.updateHotel(a.getName(),r);}
 @PutMapping("/work-types/{id}") public HotelDtos.WorkTypeView type(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminDtos.WorkTypeUpdate r){return service.updateType(a.getName(),id,r);}
 @GetMapping("/payroll/monthly-close") @PreAuthorize("hasRole('EMPLOYER')") public AdminDtos.MonthlyClose close(Authentication a,@RequestParam int year,@RequestParam int month){return service.monthlyClose(a.getName(),year,month);}
 @GetMapping("/payroll/export.csv") @PreAuthorize("hasRole('EMPLOYER')") public ResponseEntity<byte[]> export(Authentication a,@RequestParam int year,@RequestParam int month){return payroll.csv(a.getName(),year,month);}
 @PostMapping(value="/imports/plans",consumes="multipart/form-data") public AdminDtos.ImportResult importPlans(Authentication a,@RequestPart("file") MultipartFile file,@RequestParam(defaultValue="false") boolean overwrite){return excel.importPlan(a.getName(),file,overwrite);}
}
