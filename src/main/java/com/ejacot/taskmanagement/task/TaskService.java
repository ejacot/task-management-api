package com.ejacot.taskmanagement.task;

import com.ejacot.taskmanagement.user.UserAccount;
import com.ejacot.taskmanagement.user.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TaskService {
    private final TaskRepository tasks;
    private final UserAccountRepository users;

    public TaskService(TaskRepository tasks, UserAccountRepository users) {
        this.tasks = tasks;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<TaskDtos.Response> findAll(String username, TaskStatus status, Pageable pageable) {
        Page<Task> result = status == null
                ? tasks.findAllByOwnerUsername(username, pageable)
                : tasks.findAllByOwnerUsernameAndStatus(username, status, pageable);
        return result.map(TaskDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public TaskDtos.Response findOne(String username, Long id) {
        return TaskDtos.Response.from(ownedTask(username, id));
    }

    public TaskDtos.Response create(String username, TaskDtos.CreateRequest request) {
        UserAccount owner = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Task task = new Task(request.title().trim(), request.description(), request.priority(), request.dueDate(), owner);
        return TaskDtos.Response.from(tasks.save(task));
    }

    public TaskDtos.Response update(String username, Long id, TaskDtos.UpdateRequest request) {
        Task task = ownedTask(username, id);
        task.update(request.title().trim(), request.description(), request.status(), request.priority(), request.dueDate());
        return TaskDtos.Response.from(task);
    }

    public void delete(String username, Long id) {
        tasks.delete(ownedTask(username, id));
    }

    private Task ownedTask(String username, Long id) {
        return tasks.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}

