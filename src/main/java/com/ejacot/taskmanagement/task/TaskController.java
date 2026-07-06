package com.ejacot.taskmanagement.task;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) { this.service = service; }

    @GetMapping
    public Page<TaskDtos.Response> findAll(
            Authentication auth,
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.findAll(auth.getName(), status, pageable);
    }

    @GetMapping("/{id}")
    public TaskDtos.Response findOne(Authentication auth, @PathVariable Long id) {
        return service.findOne(auth.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDtos.Response create(Authentication auth, @Valid @RequestBody TaskDtos.CreateRequest request) {
        return service.create(auth.getName(), request);
    }

    @PutMapping("/{id}")
    public TaskDtos.Response update(Authentication auth, @PathVariable Long id,
                                    @Valid @RequestBody TaskDtos.UpdateRequest request) {
        return service.update(auth.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        service.delete(auth.getName(), id);
    }
}
