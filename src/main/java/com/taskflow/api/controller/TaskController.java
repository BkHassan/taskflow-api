package com.taskflow.api.controller;

import com.taskflow.api.dto.request.CreateTaskRequest;
import com.taskflow.api.dto.request.UpdateTaskRequest;
import com.taskflow.api.dto.response.TaskResponse;
import com.taskflow.api.entity.User;
import com.taskflow.api.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks")
public class TaskController {

	private final TaskService taskService;

	@GetMapping
	@Operation(summary = "List tasks (own tasks for USER, all tasks for ADMIN)")
	public ResponseEntity<List<TaskResponse>> findAll(@AuthenticationPrincipal User currentUser) {
		return ResponseEntity.ok(taskService.findAllFor(currentUser));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a task by id")
	public ResponseEntity<TaskResponse> findById(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
		return ResponseEntity.ok(taskService.findById(id, currentUser));
	}

	@PostMapping
	@Operation(summary = "Create a task owned by the current user")
	public ResponseEntity<TaskResponse> create(
			@Valid @RequestBody CreateTaskRequest request,
			@AuthenticationPrincipal User currentUser
	) {
		TaskResponse created = taskService.create(request, currentUser);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Replace a task")
	public ResponseEntity<TaskResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateTaskRequest request,
			@AuthenticationPrincipal User currentUser
	) {
		return ResponseEntity.ok(taskService.update(id, request, currentUser));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a task")
	public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
		taskService.delete(id, currentUser);
		return ResponseEntity.noContent().build();
	}
}
