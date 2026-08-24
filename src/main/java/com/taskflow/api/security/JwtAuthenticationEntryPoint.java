package com.taskflow.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Authentication required", request.getRequestURI());
	}

	static void writeJson(HttpServletResponse response, int status, String error, String message, String path) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String body = """
				{"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}
				""".formatted(Instant.now(), status, error, message, path);
		response.getWriter().write(body);
	}
}
