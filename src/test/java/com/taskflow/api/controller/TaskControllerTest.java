package com.taskflow.api.controller;

import com.taskflow.api.entity.Role;
import com.taskflow.api.entity.User;
import com.taskflow.api.repository.TaskRepository;
import com.taskflow.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clean() {
		taskRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void userCanCrudOwnTask() throws Exception {
		String token = register("owner1", "owner1@test.com");

		String created = mockMvc.perform(post("/api/tasks")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Write tests","description":"Cover auth and tasks","status":"TODO"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Write tests"))
				.andExpect(jsonPath("$.ownerUsername").value("owner1"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		long id = extractId(created);

		mockMvc.perform(get("/api/tasks")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(put("/api/tasks/" + id)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Write tests","description":"Done locally","status":"DONE"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DONE"));

		mockMvc.perform(delete("/api/tasks/" + id)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
	}

	@Test
	void userCannotAccessAnotherUsersTask() throws Exception {
		String aliceToken = register("alice2", "alice2@test.com");
		String bobToken = register("bob2", "bob2@test.com");

		String created = mockMvc.perform(post("/api/tasks")
						.header("Authorization", "Bearer " + aliceToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Alice private task"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long id = extractId(created);

		mockMvc.perform(get("/api/tasks/" + id)
						.header("Authorization", "Bearer " + bobToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/tasks")
						.header("Authorization", "Bearer " + bobToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void adminCanListAllTasksAndUsers() throws Exception {
		String userToken = register("normal", "normal@test.com");
		mockMvc.perform(post("/api/tasks")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"User task"}
								"""))
				.andExpect(status().isCreated());

		String adminToken = createAdminAndLogin();

		mockMvc.perform(get("/api/tasks")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get("/api/admin/users")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get("/api/admin/users")
						.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void missingTaskReturns404() throws Exception {
		String token = register("ghost", "ghost@test.com");
		mockMvc.perform(get("/api/tasks/999")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
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
		return extractToken(body);
	}

	private String createAdminAndLogin() throws Exception {
		userRepository.save(User.builder()
				.username("superadmin")
				.email("admin@test.com")
				.password(passwordEncoder.encode("Password1"))
				.role(Role.ADMIN)
				.build());
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"superadmin","password":"Password1"}
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractToken(body);
	}

	private static String extractToken(String body) {
		int start = body.indexOf("\"token\":\"") + 9;
		int end = body.indexOf('"', start);
		return body.substring(start, end);
	}

	private static long extractId(String body) {
		int start = body.indexOf("\"id\":") + 5;
		int end = start;
		while (end < body.length() && Character.isDigit(body.charAt(end))) {
			end++;
		}
		return Long.parseLong(body.substring(start, end));
	}
}
