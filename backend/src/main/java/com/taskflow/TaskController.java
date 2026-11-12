package com.taskflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    record TaskReq(
    @NotBlank @Size(max = 200) String title,
    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "priority must be LOW, MEDIUM or HIGH") String priority,
    @FutureOrPresent LocalDate dueDate) {}

    private final TaskRepository tasks;

    public TaskController(TaskRepository tasks) { this.tasks = tasks; }

        @GetMapping
    public ResponseEntity<List<Task>> list(@AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Specification<Task> spec = (root, query, cb) -> cb.conjunction(); // "no condition yet"
        if (!"ADMIN".equals(me.role()))
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), me.id()));
        if (status != null && !status.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (q != null && !q.isBlank())
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + q.toLowerCase() + "%"));

        PageRequest paging = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Task> result = tasks.findAll(spec, paging);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @PostMapping
    public ResponseEntity<Task> create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TaskReq r) {
        Task t = new Task();
        t.title = r.title();
        if (r.priority() != null) t.priority = r.priority();
        t.dueDate = r.dueDate();
        t.userId = me.id();
        return ResponseEntity.status(201).body(tasks.save(t));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Task> complete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        Task t = tasks.findById(id).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        if (!canTouch(me, t)) return ResponseEntity.status(403).build();
        t.status = "DONE";
        t.completedAt = LocalDateTime.now();
        return ResponseEntity.ok(tasks.save(t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        Task t = tasks.findById(id).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        if (!canTouch(me, t)) return ResponseEntity.status(403).build();
        tasks.delete(t);
        return ResponseEntity.noContent().build();
    }

    private boolean canTouch(AuthUser me, Task t) {
        return "ADMIN".equals(me.role()) || t.userId.equals(me.id());
    }
}
