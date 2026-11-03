package com.example.orbit.server.sync;

import com.example.orbit.server.auth.AuthInterceptor;
import com.example.orbit.server.sync.SyncDtos.PushRequest;
import com.example.orbit.server.sync.SyncDtos.Snapshot;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping
    public Snapshot snapshot(
        @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) long userId
    ) {
        return syncService.snapshot(userId);
    }

    @PutMapping
    public ResponseEntity<Snapshot> push(
        @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) long userId,
        @RequestBody PushRequest request
    ) {
        var outcome = syncService.push(userId, request);
        if (outcome.accepted()) {
            return ResponseEntity.ok(outcome.snapshot());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(outcome.snapshot());
    }
}
