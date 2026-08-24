package com.taskflow.api.controller;

import com.taskflow.api.repository.TaskRepository;
import com.taskflow.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TaskRepository taskRepository;

	@BeforeEach
	void cleanUsers() {
		taskRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void registerReturnsTokenAndUserRole() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"alice","email":"alice@test.com","password":"Password1"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token", notNullValue()))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.username").value("alice"))
				.andExpect(jsonPath("$.user.role").value("USER"));
	}

	@Test
	void registerRejectsDuplicateUsername() throws Exception {
		register("bob", "bob@test.com");
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"bob","email":"bob2@test.com","password":"Password1"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("Username")));
	}

	@Test
	void registerRejectsShortPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"carol","email":"carol@test.com","password":"short"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.password", notNullValue()));
	}

	@Test
	void loginReturnsToken() throws Exception {
		register("dave", "dave@test.com");
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"dave","password":"Password1"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", notNullValue()));
	}

	@Test
	void loginRejectsBadPassword() throws Exception {
		register("erin", "erin@test.com");
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"erin","password":"WrongPass1"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointRequiresToken() throws Exception {
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meRequiresToken() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturnsCurrentUser() throws Exception {
		String token = register("frank", "frank@test.com");
		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("frank"));
	}

	private String register(String username, String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","email":"%s","password":"Password1"}
								""".formatted(username, email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		int start = body.indexOf("\"token\":\"") + 9;
		int end = body.indexOf('"', start);
		return body.substring(start, end);
	}
}
