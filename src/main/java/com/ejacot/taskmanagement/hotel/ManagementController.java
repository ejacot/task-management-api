package com.ejacot.taskmanagement.hotel;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/management") @PreAuthorize("hasAnyRole('MANAGER','EMPLOYER','CHECKER')")
public class ManagementController{
 private final ManagementService service;public ManagementController(ManagementService service){this.service=service;}
 @GetMapping("/overview") public ManagementDtos.Overview overview(Authentication a){return service.overview(a.getName());}
 @PostMapping("/plans") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('MANAGER','EMPLOYER')") public List<HotelDtos.PlanView> create(Authentication a,@Valid @RequestBody ManagementDtos.PlanRequest r){return service.createPlans(a.getName(),r);}
 @PutMapping("/plans/{id}") @PreAuthorize("hasAnyRole('MANAGER','EMPLOYER')") public HotelDtos.PlanView update(Authentication a,@PathVariable Long id,@Valid @RequestBody ManagementDtos.UpdatePlanRequest r){return service.updatePlan(a.getName(),id,r);}
 @PutMapping("/logs/{id}/review") public HotelDtos.LogView review(Authentication a,@PathVariable Long id,@Valid @RequestBody ManagementDtos.ReviewRequest r){return service.review(a.getName(),id,r);}
 @GetMapping("/logs/{id}/attachment") public ManagementDtos.AttachmentView attachment(Authentication a,@PathVariable Long id){return service.attachment(a.getName(),id);}
 @PostMapping("/work-types") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('MANAGER','EMPLOYER')") public HotelDtos.WorkTypeView type(Authentication a,@Valid @RequestBody ManagementDtos.WorkTypeRequest r){return service.createType(a.getName(),r);}
 @PostMapping("/pay-rates") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('EMPLOYER')") public void pay(Authentication a,@Valid @RequestBody ManagementDtos.PayRateRequest r){service.addPayRate(a.getName(),r);}
}
