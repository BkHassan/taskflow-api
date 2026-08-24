package com.taskflow.api.controller;

import com.taskflow.api.dto.response.UserResponse;
import com.taskflow.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminController {

	private final UserService userService;

	@GetMapping("/users")
	@Operation(summary = "List all users (ADMIN only)")
	public ResponseEntity<List<UserResponse>> findAllUsers() {
		return ResponseEntity.ok(userService.findAll());
	}
}
