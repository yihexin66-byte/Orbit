package com.example.orbit.server.sync;

import java.util.List;

public final class SyncDtos {

    private SyncDtos() {
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    public record Goal(
        long id,
        String title,
        String description,
        String tone,
        String status,
        Long targetDate,
        long createdAt,
        long updatedAt,
        Long completedAt,
        int sortOrder
    ) {
    }

    public record Stage(
        long id,
        long goalId,
        String title,
        int position,
        Long targetDate,
        boolean isCompleted,
        Long completedAt,
        long createdAt,
        long updatedAt
    ) {
    }

    public record Task(
        long id,
        String title,
        String description,
        String priority,
        String category,
        Long dueDate,
        boolean isCompleted,
        long createdAt,
        Long completedAt,
        Long goalId,
        Long stageId
    ) {
    }

    public record Habit(
        long id,
        String title,
        String note,
        String tone,
        long createdAt,
        Long archivedAt,
        int sortOrder
    ) {
    }

    public record HabitRecord(
        long id,
        long habitId,
        long epochDay,
        long checkedAt
    ) {
    }

    public record GrowthEvent(
        long id,
        String type,
        String sourceKey,
        int xp,
        long occurredAt,
        long epochDay
    ) {
    }

    public record Snapshot(
        long revision,
        List<Goal> goals,
        List<Stage> stages,
        List<Task> tasks,
        List<Habit> habits,
        List<HabitRecord> habitRecords,
        List<GrowthEvent> growthEvents
    ) {
        public Snapshot {
            goals = safe(goals);
            stages = safe(stages);
            tasks = safe(tasks);
            habits = safe(habits);
            habitRecords = safe(habitRecords);
            growthEvents = safe(growthEvents);
        }

        public static Snapshot empty() {
            return new Snapshot(0, null, null, null, null, null, null);
        }
    }

    public record PushRequest(
        long baseRevision,
        List<Goal> goals,
        List<Stage> stages,
        List<Task> tasks,
        List<Habit> habits,
        List<HabitRecord> habitRecords,
        List<GrowthEvent> growthEvents
    ) {
        public PushRequest {
            goals = safe(goals);
            stages = safe(stages);
            tasks = safe(tasks);
            habits = safe(habits);
            habitRecords = safe(habitRecords);
            growthEvents = safe(growthEvents);
        }
    }

    public record PushOutcome(boolean accepted, Snapshot snapshot) {
    }
}
