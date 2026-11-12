package com.taskflow;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String title;
    @Column(nullable = false)
    public String status = "PENDING";
    @Column(nullable = false)
    public String priority = "MEDIUM";
    @Column(name = "due_date")
    public LocalDate dueDate;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "completed_at")
    public LocalDateTime completedAt;
}