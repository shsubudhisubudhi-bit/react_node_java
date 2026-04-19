package com.developer.test.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class TaskStatus {

    public static final String PENDING = "pending";
    public static final String IN_PROGRESS = "in-progress";
    public static final String COMPLETED = "completed";

    public static final Set<String> VALID = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(PENDING, IN_PROGRESS, COMPLETED)));

    private TaskStatus() {
    }

    public static boolean isValid(String status) {
        return status != null && VALID.contains(status);
    }
}
