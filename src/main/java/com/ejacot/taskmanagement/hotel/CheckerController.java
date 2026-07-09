package com.ejacot.taskmanagement.hotel;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/checker") @PreAuthorize("hasAnyRole('CHECKER','MANAGER','EMPLOYER')")
public class CheckerController {
 private final CheckerService service;public CheckerController(CheckerService service){this.service=service;}
 @GetMapping("/rooms") public CheckerDtos.DayRooms rooms(Authentication a,@RequestParam LocalDate date){return service.rooms(a.getName(),date);}
 @PutMapping("/rooms/{id}") public ManagementDtos.RoomAssignmentView review(Authentication a,@PathVariable Long id,@Valid @RequestBody CheckerDtos.RoomReview r){return service.review(a.getName(),id,r);}
 @PutMapping("/rooms/{id}/defect") public ManagementDtos.RoomAssignmentView defect(Authentication a,@PathVariable Long id,@Valid @RequestBody CheckerDtos.DefectUpdate r){return service.defect(a.getName(),id,r);}
}
