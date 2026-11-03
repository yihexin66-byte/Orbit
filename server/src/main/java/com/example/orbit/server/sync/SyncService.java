package com.example.orbit.server.sync;

import com.example.orbit.server.common.ApiException;
import com.example.orbit.server.sync.SyncDtos.Goal;
import com.example.orbit.server.sync.SyncDtos.GrowthEvent;
import com.example.orbit.server.sync.SyncDtos.Habit;
import com.example.orbit.server.sync.SyncDtos.HabitRecord;
import com.example.orbit.server.sync.SyncDtos.PushOutcome;
import com.example.orbit.server.sync.SyncDtos.PushRequest;
import com.example.orbit.server.sync.SyncDtos.Snapshot;
import com.example.orbit.server.sync.SyncDtos.Stage;
import com.example.orbit.server.sync.SyncDtos.Task;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncService {

    private static final int MAX_ROWS_PER_KIND = 5000;

    private final SyncGoalRepository goals;
    private final SyncStageRepository stages;
    private final SyncTaskRepository tasks;
    private final SyncHabitRepository habits;
    private final SyncHabitRecordRepository habitRecords;
    private final SyncGrowthEventRepository growthEvents;
    private final SyncRevisionRepository revisions;

    public SyncService(
        SyncGoalRepository goals,
        SyncStageRepository stages,
        SyncTaskRepository tasks,
        SyncHabitRepository habits,
        SyncHabitRecordRepository habitRecords,
        SyncGrowthEventRepository growthEvents,
        SyncRevisionRepository revisions
    ) {
        this.goals = goals;
        this.stages = stages;
        this.tasks = tasks;
        this.habits = habits;
        this.habitRecords = habitRecords;
        this.growthEvents = growthEvents;
        this.revisions = revisions;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(long userId) {
        long revision = revisions.findById(userId)
            .map(SyncRevision::getRevision)
            .orElse(0L);
        return new Snapshot(
            revision,
            goals.findByUserIdOrderBySortOrderAscClientIdAsc(userId).stream()
                .map(this::toDto)
                .toList(),
            stages.findByUserIdOrderByGoalClientIdAscPositionAsc(userId).stream()
                .map(this::toDto)
                .toList(),
            tasks.findByUserIdOrderByClientIdAsc(userId).stream()
                .map(this::toDto)
                .toList(),
            habits.findByUserIdOrderBySortOrderAscClientIdAsc(userId).stream()
                .map(this::toDto)
                .toList(),
            habitRecords.findByUserIdOrderByClientIdAsc(userId).stream()
                .map(this::toDto)
                .toList(),
            growthEvents.findByUserIdOrderByClientIdAsc(userId).stream()
                .map(this::toDto)
                .toList()
        );
    }

    @Transactional
    public PushOutcome push(long userId, PushRequest request) {
        validate(request);
        SyncRevision revision = revisions.findForUpdate(userId).orElse(null);
        long current = revision == null ? 0L : revision.getRevision();
        if (request.baseRevision() != current) {
            return new PushOutcome(false, snapshot(userId));
        }

        goals.deleteByUserId(userId);
        stages.deleteByUserId(userId);
        tasks.deleteByUserId(userId);
        habits.deleteByUserId(userId);
        habitRecords.deleteByUserId(userId);
        growthEvents.deleteByUserId(userId);

        goals.saveAll(request.goals().stream().map(row -> toEntity(userId, row)).toList());
        stages.saveAll(request.stages().stream().map(row -> toEntity(userId, row)).toList());
        tasks.saveAll(request.tasks().stream().map(row -> toEntity(userId, row)).toList());
        habits.saveAll(request.habits().stream().map(row -> toEntity(userId, row)).toList());
        habitRecords.saveAll(
            request.habitRecords().stream().map(row -> toEntity(userId, row)).toList()
        );
        growthEvents.saveAll(
            request.growthEvents().stream().map(row -> toEntity(userId, row)).toList()
        );

        if (revision == null) {
            revision = new SyncRevision();
            revision.setUserId(userId);
        }
        revision.setRevision(current + 1);
        revision.setUpdatedAt(System.currentTimeMillis());
        revisions.save(revision);

        return new PushOutcome(true, snapshot(userId));
    }

    private void validate(PushRequest request) {
        if (request.baseRevision() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SYNC_REVISION", "同步版本号不合法");
        }
        ensureSize(request.goals(), "目标");
        ensureSize(request.stages(), "阶段");
        ensureSize(request.tasks(), "任务");
        ensureSize(request.habits(), "习惯");
        ensureSize(request.habitRecords(), "打卡");
        ensureSize(request.growthEvents(), "成长记录");
    }

    private void ensureSize(List<?> rows, String label) {
        if (rows.size() > MAX_ROWS_PER_KIND) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "SYNC_TOO_LARGE",
                label + "数量超过单次同步上限"
            );
        }
    }

    private SyncGoal toEntity(long userId, Goal row) {
        SyncGoal entity = new SyncGoal();
        entity.setUserId(userId);
        entity.setClientId(row.id());
        entity.setTitle(row.title());
        entity.setDescription(row.description());
        entity.setTone(row.tone());
        entity.setStatus(row.status());
        entity.setTargetDate(row.targetDate());
        entity.setCreatedAt(row.createdAt());
        entity.setUpdatedAt(row.updatedAt());
        entity.setCompletedAt(row.completedAt());
        entity.setSortOrder(row.sortOrder());
        return entity;
    }

    private SyncStage toEntity(long userId, Stage row) {
        SyncStage entity = new SyncStage();
        entity.setUserId(userId);
        entity.setClientId(row.id());
        entity.setGoalClientId(row.goalId());
        entity.setTitle(row.title());
        entity.setPosition(row.position());
        entity.setTargetDate(row.targetDate());
        entity.setCompleted(row.isCompleted());
        entity.setCompletedAt(row.completedAt());
        entity.setCreatedAt(row.createdAt());
        entity.setUpdatedAt(row.updatedAt());
        return entity;
    }

    private SyncTask toEntity(long userId, Task row) {
        SyncTask entity = new SyncTask();
        entity.setUserId(userId);
        entity.setClientId(row.id());
        entity.setTitle(row.title());
        entity.setDescription(row.description());
        entity.setPriority(row.priority());
        entity.setCategory(row.category());
        entity.setDueDate(row.dueDate());
        entity.setCompleted(row.isCompleted());
        entity.setCreatedAt(row.createdAt());
        entity.setCompletedAt(row.completedAt());
        entity.setGoalClientId(row.goalId());
        entity.setStageClientId(row.stageId());
        return entity;
    }

    private SyncHabit toEntity(long userId, Habit row) {
        SyncHabit entity = new SyncHabit();
        entity.setUserId(userId);
        entity.setClientId(row.id());
        entity.setTitle(row.title());
        entity.setNote(row.note());
        entity.setTone(row.tone());
        entity.setCreatedAt(row.createdAt());
        entity.setArchivedAt(row.archivedAt());
        entity.setSortOrder(row.sortOrder());
        return entity;
    }

    private SyncHabitRecord toEntity(long userId, HabitRecord row) {
        SyncHabitRecord entity = new SyncHabitRecord();
        entity.setUserId(userId);
        entity.setClientId(row.id());
        entity.setHabitClientId(row.habitId());
        entity.setEpochDay(row.epochDay());
        entity.setCheckedAt(row.checkedAt());
        return entity;
    }

    private SyncGrowthEvent toEntity(long userId, GrowthEvent row) {
        SyncGrowthEvent entity = new SyncGrowthEvent();
        entity.setUserId(userId);
        entity.setClientId(row.id());
        entity.setType(row.type());
        entity.setSourceKey(row.sourceKey());
        entity.setXp(row.xp());
        entity.setOccurredAt(row.occurredAt());
        entity.setEpochDay(row.epochDay());
        return entity;
    }

    private Goal toDto(SyncGoal row) {
        return new Goal(
            row.getClientId(),
            row.getTitle(),
            row.getDescription(),
            row.getTone(),
            row.getStatus(),
            row.getTargetDate(),
            row.getCreatedAt(),
            row.getUpdatedAt(),
            row.getCompletedAt(),
            row.getSortOrder()
        );
    }

    private Stage toDto(SyncStage row) {
        return new Stage(
            row.getClientId(),
            row.getGoalClientId(),
            row.getTitle(),
            row.getPosition(),
            row.getTargetDate(),
            row.isCompleted(),
            row.getCompletedAt(),
            row.getCreatedAt(),
            row.getUpdatedAt()
        );
    }

    private Task toDto(SyncTask row) {
        return new Task(
            row.getClientId(),
            row.getTitle(),
            row.getDescription(),
            row.getPriority(),
            row.getCategory(),
            row.getDueDate(),
            row.isCompleted(),
            row.getCreatedAt(),
            row.getCompletedAt(),
            row.getGoalClientId(),
            row.getStageClientId()
        );
    }

    private Habit toDto(SyncHabit row) {
        return new Habit(
            row.getClientId(),
            row.getTitle(),
            row.getNote(),
            row.getTone(),
            row.getCreatedAt(),
            row.getArchivedAt(),
            row.getSortOrder()
        );
    }

    private HabitRecord toDto(SyncHabitRecord row) {
        return new HabitRecord(
            row.getClientId(),
            row.getHabitClientId(),
            row.getEpochDay(),
            row.getCheckedAt()
        );
    }

    private GrowthEvent toDto(SyncGrowthEvent row) {
        return new GrowthEvent(
            row.getClientId(),
            row.getType(),
            row.getSourceKey(),
            row.getXp(),
            row.getOccurredAt(),
            row.getEpochDay()
        );
    }
}
