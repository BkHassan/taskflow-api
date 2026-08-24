package com.taskflow.api.service;

import com.taskflow.api.dto.request.CreateTaskRequest;
import com.taskflow.api.dto.request.UpdateTaskRequest;
import com.taskflow.api.dto.response.TaskResponse;
import com.taskflow.api.entity.Role;
import com.taskflow.api.entity.Task;
import com.taskflow.api.entity.TaskStatus;
import com.taskflow.api.entity.User;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository taskRepository;

	@Transactional(readOnly = true)
	public List<TaskResponse> findAllFor(User currentUser) {
		List<Task> tasks = currentUser.getRole() == Role.ADMIN
				? taskRepository.findAllByOrderByCreatedAtDesc()
				: taskRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId());
		return tasks.stream().map(TaskResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse findById(Long id, User currentUser) {
		Task task = getTask(id);
		assertCanAccess(task, currentUser);
		return TaskResponse.from(task);
	}

	@Transactional
	public TaskResponse create(CreateTaskRequest request, User currentUser) {
		Task task = Task.builder()
				.title(request.title())
				.description(request.description())
				.status(request.status() == null ? TaskStatus.TODO : request.status())
				.owner(currentUser)
				.build();
		return TaskResponse.from(taskRepository.save(task));
	}

	@Transactional
	public TaskResponse update(Long id, UpdateTaskRequest request, User currentUser) {
		Task task = getTask(id);
		assertCanAccess(task, currentUser);
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setStatus(request.status());
		return TaskResponse.from(task);
	}

	@Transactional
	public void delete(Long id, User currentUser) {
		Task task = getTask(id);
		assertCanAccess(task, currentUser);
		taskRepository.delete(task);
	}

	private Task getTask(Long id) {
		return taskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
	}

	private void assertCanAccess(Task task, User currentUser) {
		if (currentUser.getRole() == Role.ADMIN) {
			return;
		}
		if (!task.getOwner().getId().equals(currentUser.getId())) {
			throw new AccessDeniedException("You can only access your own tasks");
		}
	}
}
