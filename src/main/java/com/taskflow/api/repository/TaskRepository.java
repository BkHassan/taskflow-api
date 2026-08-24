package com.taskflow.api.repository;

import com.taskflow.api.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

	List<Task> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

	List<Task> findAllByOrderByCreatedAtDesc();
}
