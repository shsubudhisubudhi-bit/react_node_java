package com.developer.test.service;

import com.developer.test.dto.StatsResponse;
import com.developer.test.exception.ResourceNotFoundException;
import com.developer.test.exception.ValidationException;
import com.developer.test.model.Task;
import com.developer.test.model.TaskStatus;
import com.developer.test.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DataStore {

    private static final Logger log = LoggerFactory.getLogger(DataStore.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger nextUserId = new AtomicInteger(1);
    private final AtomicInteger nextTaskId = new AtomicInteger(1);

    private final PersistenceService persistence;

    public DataStore(PersistenceService persistence) {
        this.persistence = persistence;
    }

    @PostConstruct
    void init() {
        Optional<PersistenceService.Snapshot> loaded = persistence.load();
        if (loaded.isPresent()) {
            PersistenceService.Snapshot snap = loaded.get();
            snap.users.forEach(u -> users.put(u.getId(), u));
            snap.tasks.forEach(t -> tasks.put(t.getId(), t));
            nextUserId.set(snap.nextUserId);
            nextTaskId.set(snap.nextTaskId);
        } else {
            seedDefaultData();
            save();
        }
    }

    private void seedDefaultData() {
        users.put(1, new User(1, "John Doe", "john@example.com", "developer"));
        users.put(2, new User(2, "Jane Smith", "jane@example.com", "designer"));
        users.put(3, new User(3, "Bob Johnson", "bob@example.com", "manager"));

        tasks.put(1, new Task(1, "Implement authentication", TaskStatus.PENDING, 1));
        tasks.put(2, new Task(2, "Design user interface", TaskStatus.IN_PROGRESS, 2));
        tasks.put(3, new Task(3, "Review code changes", TaskStatus.COMPLETED, 3));

        nextUserId.set(4);
        nextTaskId.set(4);
    }

    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public User getUserById(int id) {
        return users.get(id);
    }

    public List<Task> getTasks(String status, String userId) {
        Integer userIdFilter = parseUserIdFilter(userId);
        return tasks.values().stream()
                .filter(task -> status == null || status.isEmpty() || task.getStatus().equals(status))
                .filter(task -> userIdFilter == null || task.getUserId() == userIdFilter)
                .collect(Collectors.toList());
    }

    private Integer parseUserIdFilter(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new ValidationException("userId query parameter must be an integer");
        }
    }

    public int getUserCount() {
        return users.size();
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public StatsResponse getStats() {
        StatsResponse stats = new StatsResponse();
        stats.getUsers().setTotal(users.size());
        stats.getTasks().setTotal(tasks.size());

        for (Task task : tasks.values()) {
            switch (task.getStatus()) {
                case TaskStatus.PENDING:
                    stats.getTasks().setPending(stats.getTasks().getPending() + 1);
                    break;
                case TaskStatus.IN_PROGRESS:
                    stats.getTasks().setInProgress(stats.getTasks().getInProgress() + 1);
                    break;
                case TaskStatus.COMPLETED:
                    stats.getTasks().setCompleted(stats.getTasks().getCompleted() + 1);
                    break;
                default:
                    break;
            }
        }
        return stats;
    }

    public synchronized User addUser(String name, String email, String role) {
        String trimmedName = requireNonBlank(name, "name");
        String trimmedEmail = requireNonBlank(email, "email");
        String trimmedRole = requireNonBlank(role, "role");

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new ValidationException("email must be a valid email address");
        }

        int id = nextUserId.getAndIncrement();
        User user = new User(id, trimmedName, trimmedEmail, trimmedRole);
        users.put(id, user);
        save();
        log.info("Created user id={} email={}", id, trimmedEmail);
        return user;
    }

    public synchronized Task addTask(String title, String status, Integer userId) {
        String trimmedTitle = requireNonBlank(title, "title");
        String trimmedStatus = requireNonBlank(status, "status");

        if (!TaskStatus.isValid(trimmedStatus)) {
            throw new ValidationException("status must be one of " + TaskStatus.VALID);
        }
        if (userId == null) {
            throw new ValidationException("userId is required");
        }
        if (!users.containsKey(userId)) {
            throw new ValidationException("userId " + userId + " does not exist");
        }

        int id = nextTaskId.getAndIncrement();
        Task task = new Task(id, trimmedTitle, trimmedStatus, userId);
        tasks.put(id, task);
        save();
        log.info("Created task id={} userId={} status={}", id, userId, trimmedStatus);
        return task;
    }

    public synchronized Task updateTask(int id, String title, String status, Integer userId) {
        Task existing = tasks.get(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Task " + id + " not found");
        }

        if (title != null && title.trim().isEmpty()) {
            throw new ValidationException("title must not be blank");
        }
        if (status != null && !TaskStatus.isValid(status)) {
            throw new ValidationException("status must be one of " + TaskStatus.VALID);
        }
        if (userId != null && !users.containsKey(userId)) {
            throw new ValidationException("userId " + userId + " does not exist");
        }

        if (title != null) existing.setTitle(title.trim());
        if (status != null) existing.setStatus(status);
        if (userId != null) existing.setUserId(userId);

        save();
        log.info("Updated task id={}", id);
        return existing;
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    private void save() {
        persistence.save(new PersistenceService.Snapshot(
                new ArrayList<>(users.values()),
                new ArrayList<>(tasks.values()),
                nextUserId.get(),
                nextTaskId.get()));
    }
}
