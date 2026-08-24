package com.taskflow.api.config;

import com.taskflow.api.entity.Role;
import com.taskflow.api.entity.User;
import com.taskflow.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.seed-demo-users:false}")
	private boolean seedDemoUsers;

	@Override
	public void run(String... args) {
		if (!seedDemoUsers) {
			return;
		}
		createIfMissing("admin", "admin@taskflow.local", "Admin123!", Role.ADMIN);
		createIfMissing("demo", "demo@taskflow.local", "Demo123!", Role.USER);
	}

	private void createIfMissing(String username, String email, String rawPassword, Role role) {
		if (userRepository.existsByUsername(username)) {
			return;
		}
		userRepository.save(User.builder()
				.username(username)
				.email(email)
				.password(passwordEncoder.encode(rawPassword))
				.role(role)
				.build());
		log.info("Seeded demo user '{}' with role {}", username, role);
	}
}
