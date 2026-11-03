package com.example.orbit.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orbit.server.auth.AuthDtos.AuthResponse;
import com.example.orbit.server.auth.AuthDtos.LoginRequest;
import com.example.orbit.server.auth.AuthDtos.RegisterRequest;
import com.example.orbit.server.auth.AuthService;
import com.example.orbit.server.common.ApiException;
import com.example.orbit.server.sync.SyncDtos;
import com.example.orbit.server.sync.SyncService;
import com.example.orbit.server.user.UserService;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
class OrbitServerApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private SyncService syncService;

    @Test
    void registerLoginProfileAndAvatar() {
        String username = "alice" + (System.nanoTime() % 100_000_000L);
        AuthResponse registered = authService.register(
            new RegisterRequest(username, "orbit-secret")
        );
        assertThat(registered.token()).hasSize(64);
        assertThat(registered.user().nickname()).isEqualTo(username);

        AuthResponse loggedIn = authService.login(new LoginRequest(username, "orbit-secret"));
        assertThat(loggedIn.user().id()).isEqualTo(registered.user().id());

        assertThatThrownBy(() ->
            authService.register(new RegisterRequest(username, "another-secret"))
        ).isInstanceOf(ApiException.class);

        long userId = registered.user().id();
        assertThat(userService.updateNickname(userId, "  夜航者  ").nickname())
            .isEqualTo("夜航者");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            "image/png",
            new byte[] {1, 2, 3, 4}
        );
        String avatarPath = userService.updateAvatar(userId, file).avatarPath();
        assertThat(avatarPath).startsWith("/media/avatars/").endsWith(".png");

        // JSON base64 uploads keep the Spring backend usable for LAN debugging
        // now that the Android client targets uniCloud URL化.
        String base64 = Base64.getEncoder().encodeToString(new byte[] {9, 8, 7, 6});
        String jsonAvatarPath = userService
            .updateAvatar(userId, base64, "image/png; charset=binary")
            .avatarPath();
        assertThat(jsonAvatarPath).startsWith("/media/avatars/").endsWith(".png");
        assertThatThrownBy(() ->
            userService.updateAvatar(userId, "!!!!", "image/png")
        ).isInstanceOf(ApiException.class);
    }

    @Test
    void snapshotPushAndRevisionConflict() {
        String username = "bob" + (System.nanoTime() % 100_000_000L);
        long userId = authService.register(
            new RegisterRequest(username, "orbit-secret")
        ).user().id();

        SyncDtos.PushRequest first = new SyncDtos.PushRequest(
            0,
            List.of(new SyncDtos.Goal(
                1, "Run a half marathon", "Train consistently", "CYAN", "ACTIVE",
                null, 1_700_000_000_000L, 1_700_000_000_000L, null, 0
            )),
            List.of(new SyncDtos.Stage(
                1, 1, "Base building", 0, null, false, null,
                1_700_000_000_000L, 1_700_000_000_000L
            )),
            List.of(new SyncDtos.Task(
                1, "Easy 5k", null, "MEDIUM", "FITNESS", null, false,
                1_700_000_000_000L, null, 1L, 1L
            )),
            List.of(new SyncDtos.Habit(
                1, "Hydrate", null, "CYAN", 1_700_000_000_000L, null, 0
            )),
            List.of(new SyncDtos.HabitRecord(1, 1, 20_000, 1_700_000_000_000L)),
            List.of(new SyncDtos.GrowthEvent(
                1, "TASK_COMPLETED", "task:1", 12, 1_700_000_000_000L, 20_000
            ))
        );

        var outcome = syncService.push(userId, first);
        assertThat(outcome.accepted()).isTrue();
        assertThat(outcome.snapshot().revision()).isEqualTo(1);
        assertThat(outcome.snapshot().goals()).hasSize(1);
        assertThat(outcome.snapshot().tasks().get(0).title()).isEqualTo("Easy 5k");
        assertThat(outcome.snapshot().habitRecords()).hasSize(1);

        var pulled = syncService.snapshot(userId);
        assertThat(pulled.revision()).isEqualTo(1);
        assertThat(pulled.stages()).hasSize(1);
        assertThat(pulled.habits()).hasSize(1);
        assertThat(pulled.growthEvents()).hasSize(1);

        // A second accepted push rewrites every table with the same client ids.
        // The bulk deletes must run before the inserts, otherwise Hibernate's
        // insert-before-delete flush order violates the (user_id, client_id)
        // unique keys and the whole sync fails with a 500.
        long completedAt = 1_700_000_100_000L;
        SyncDtos.PushRequest second = new SyncDtos.PushRequest(
            1,
            first.goals(),
            first.stages(),
            List.of(new SyncDtos.Task(
                1, "Easy 5k", "Done", "MEDIUM", "FITNESS", null, true,
                1_700_000_000_000L, completedAt, 1L, 1L
            )),
            first.habits(),
            first.habitRecords(),
            List.of(
                first.growthEvents().get(0),
                new SyncDtos.GrowthEvent(
                    2, "TASK_COMPLETED", "task:1:completed", 12, completedAt, 20_001
                )
            )
        );

        var replaced = syncService.push(userId, second);
        assertThat(replaced.accepted()).isTrue();
        assertThat(replaced.snapshot().revision()).isEqualTo(2);
        assertThat(replaced.snapshot().tasks()).hasSize(1);
        assertThat(replaced.snapshot().tasks().get(0).isCompleted()).isTrue();
        assertThat(replaced.snapshot().tasks().get(0).completedAt()).isEqualTo(completedAt);
        assertThat(replaced.snapshot().growthEvents()).hasSize(2);
        assertThat(syncService.snapshot(userId).revision()).isEqualTo(2);

        var stale = syncService.push(userId, first);
        assertThat(stale.accepted()).isFalse();
        assertThat(stale.snapshot().revision()).isEqualTo(2);
        assertThat(stale.snapshot().goals()).hasSize(1);
    }
}
