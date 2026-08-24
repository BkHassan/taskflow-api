package com.taskflow.api.service;

import com.taskflow.api.dto.request.LoginRequest;
import com.taskflow.api.dto.request.RegisterRequest;
import com.taskflow.api.dto.response.AuthResponse;
import com.taskflow.api.dto.response.UserResponse;
import com.taskflow.api.entity.Role;
import com.taskflow.api.entity.User;
import com.taskflow.api.exception.DuplicateResourceException;
import com.taskflow.api.repository.UserRepository;
import com.taskflow.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new DuplicateResourceException("Username is already taken");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateResourceException("Email is already registered");
		}

		User user = User.builder()
				.username(request.username())
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.role(Role.USER)
				.build();
		userRepository.save(user);
		return AuthResponse.of(jwtService.generateToken(user), UserResponse.from(user));
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);
		User user = userRepository.findByUsername(request.username()).orElseThrow();
		return AuthResponse.of(jwtService.generateToken(user), UserResponse.from(user));
	}
}
