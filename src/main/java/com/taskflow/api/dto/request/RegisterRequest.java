package com.taskflow.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank
		@Size(min = 3, max = 50)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username may contain letters, digits, dots, underscores and hyphens")
		String username,

		@NotBlank
		@Email
		@Size(max = 120)
		String email,

		@NotBlank
		@Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
		String password
) {
}
