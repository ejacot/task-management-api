package com.ejacot.taskmanagement.hotel;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotel")
public class HotelWorkController {
    private final HotelWorkService service;
    public HotelWorkController(HotelWorkService service) { this.service = service; }

    @GetMapping("/bootstrap")
    public HotelDtos.Bootstrap bootstrap(Authentication auth) { return service.bootstrap(auth.getName()); }

    @PostMapping("/logs")
    @ResponseStatus(HttpStatus.CREATED)
    public HotelDtos.LogView createLog(Authentication auth, @Valid @RequestBody HotelDtos.CreateLog request) {
        return service.createLog(auth.getName(), request);
    }

    @PutMapping("/logs/{id}/submit")
    public HotelDtos.LogView submit(Authentication auth, @PathVariable Long id) { return service.submit(auth.getName(), id); }

    @PutMapping("/logs/{id}/correction")
    public HotelDtos.LogView correct(Authentication auth,@PathVariable Long id,@Valid @RequestBody HotelDtos.CorrectPlannedLog request){return service.correctPlannedLog(auth.getName(),id,request);}

    @PutMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(Authentication auth,@PathVariable Long id){service.readNotification(auth.getName(),id);}
}
