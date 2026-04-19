package com.developer.test.service;

import com.developer.test.model.Task;
import com.developer.test.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);

    private final ObjectMapper mapper;
    private final Path dataFile;

    public PersistenceService(@Value("${app.data-file:data/data.json}") String dataFilePath) {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.dataFile = Paths.get(dataFilePath);
    }

    public Optional<Snapshot> load() {
        if (!Files.exists(dataFile)) {
            return Optional.empty();
        }
        try {
            Map<String, Object> raw = mapper.readValue(dataFile.toFile(),
                    new TypeReference<Map<String, Object>>() {});

            List<User> users = mapper.convertValue(
                    raw.getOrDefault("users", Collections.emptyList()),
                    new TypeReference<List<User>>() {});
            List<Task> tasks = mapper.convertValue(
                    raw.getOrDefault("tasks", Collections.emptyList()),
                    new TypeReference<List<Task>>() {});
            int nextUserId = ((Number) raw.getOrDefault("nextUserId", 1)).intValue();
            int nextTaskId = ((Number) raw.getOrDefault("nextTaskId", 1)).intValue();

            log.info("Loaded {} users, {} tasks from {}",
                    users.size(), tasks.size(), dataFile.toAbsolutePath());
            return Optional.of(new Snapshot(users, tasks, nextUserId, nextTaskId));
        } catch (IOException e) {
            log.error("Failed to read data file {}", dataFile.toAbsolutePath(), e);
            return Optional.empty();
        }
    }

    public void save(Snapshot snapshot) {
        try {
            File parent = dataFile.toAbsolutePath().getParent().toFile();
            if (!parent.exists() && !parent.mkdirs()) {
                log.warn("Could not create data directory {}", parent);
            }

            Map<String, Object> out = new HashMap<>();
            out.put("users", snapshot.users);
            out.put("tasks", snapshot.tasks);
            out.put("nextUserId", snapshot.nextUserId);
            out.put("nextTaskId", snapshot.nextTaskId);

            Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            mapper.writeValue(tmp.toFile(), out);
            Files.move(tmp, dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Failed to persist data to {}", dataFile.toAbsolutePath(), e);
        }
    }

    public static class Snapshot {
        public final List<User> users;
        public final List<Task> tasks;
        public final int nextUserId;
        public final int nextTaskId;

        public Snapshot(List<User> users, List<Task> tasks, int nextUserId, int nextTaskId) {
            this.users = users;
            this.tasks = tasks;
            this.nextUserId = nextUserId;
            this.nextTaskId = nextTaskId;
        }
    }
}
