package com.taskflow.api.dto.request;

import com.taskflow.api.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
		@NotBlank
		@Size(max = 100)
		String title,

		@Size(max = 1000)
		String description,

		@NotNull
		TaskStatus status
) {
}
