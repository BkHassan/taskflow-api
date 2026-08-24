package com.taskflow.api.dto.response;

import com.taskflow.api.entity.Role;
import com.taskflow.api.entity.User;

import java.time.Instant;

public record UserResponse(
		Long id,
		String username,
		String email,
		Role role,
		Instant createdAt
) {
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole(),
				user.getCreatedAt()
		);
	}
}
