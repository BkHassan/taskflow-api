package com.taskflow.api.dto.response;

import com.taskflow.api.entity.Task;
import com.taskflow.api.entity.TaskStatus;

import java.time.Instant;

public record TaskResponse(
		Long id,
		String title,
		String description,
		TaskStatus status,
		String ownerUsername,
		Instant createdAt,
		Instant updatedAt
) {
	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getOwner().getUsername(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}
}
