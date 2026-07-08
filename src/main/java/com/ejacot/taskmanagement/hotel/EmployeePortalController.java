package com.ejacot.taskmanagement.hotel;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/employee")
public class EmployeePortalController {
 private final EmployeePortalService service;public EmployeePortalController(EmployeePortalService service){this.service=service;}
 @GetMapping("/requests") public List<EmployeePortalDtos.RequestView> requests(Authentication a){return service.requests(a.getName());}
 @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED) public EmployeePortalDtos.RequestView request(Authentication a,@Valid @RequestBody EmployeePortalDtos.CreateRequest r){return service.create(a.getName(),r);}
 @PutMapping("/profile") @ResponseStatus(HttpStatus.NO_CONTENT) public void profile(Authentication a,@Valid @RequestBody EmployeePortalDtos.ProfileUpdate r){service.profile(a.getName(),r);}
 @PutMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT) public void password(Authentication a,@Valid @RequestBody EmployeePortalDtos.PasswordChange r){service.password(a.getName(),r);}
 @GetMapping("/payroll/{year}") public EmployeePortalDtos.PayrollYear payroll(Authentication a,@PathVariable int year){return service.payroll(a.getName(),year);}
 @GetMapping("/management/requests") @PreAuthorize("hasAnyRole('MANAGER','EMPLOYER','CHECKER')") public List<EmployeePortalDtos.RequestView> pending(Authentication a){return service.pending(a.getName());}
 @PutMapping("/management/requests/{id}") @PreAuthorize("hasAnyRole('MANAGER','EMPLOYER')") public EmployeePortalDtos.RequestView review(Authentication a,@PathVariable Long id,@Valid @RequestBody EmployeePortalDtos.ReviewRequest r){return service.review(a.getName(),id,r);}
}
