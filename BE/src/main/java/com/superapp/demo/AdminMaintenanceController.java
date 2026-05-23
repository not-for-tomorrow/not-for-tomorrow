package com.superapp.demo;

import com.superapp.auth.SessionMaintenanceService;
import com.superapp.auth.SessionMetricsService;
import com.superapp.auth.SessionAuditService;
import com.superapp.auth.dto.SessionAuditEventPageResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/maintenance")
public class AdminMaintenanceController {

    private final SessionMaintenanceService sessionMaintenanceService;
    private final SessionMetricsService sessionMetricsService;
    private final SessionAuditService sessionAuditService;

    public AdminMaintenanceController(
        SessionMaintenanceService sessionMaintenanceService,
        SessionMetricsService sessionMetricsService,
        SessionAuditService sessionAuditService
    ) {
        this.sessionMaintenanceService = sessionMaintenanceService;
        this.sessionMetricsService = sessionMetricsService;
        this.sessionAuditService = sessionAuditService;
    }

    @PostMapping("/sessions/prune")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Map<String, Object> pruneSessions() {
        int deletedCount = sessionMaintenanceService.pruneNow();
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "expired sessions pruned");
        body.put("deletedCount", deletedCount);
        return body;
    }

    @PostMapping("/sessions/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Map<String, Object> sessionMetrics() {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "session metrics snapshot");
        body.putAll(sessionMetricsService.snapshot());
        return body;
    }

    @PostMapping("/sessions/events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public SessionAuditEventPageResponse sessionEvents(
        @RequestParam(value = "eventType", required = false) String eventType,
        @RequestParam(value = "userEmail", required = false) String userEmail,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Instant fromTime = parseInstantOrNull(from);
        Instant toTime = parseInstantOrNull(to);
        return sessionAuditService.search(eventType, userEmail, fromTime, toTime, page, size);
    }

    private Instant parseInstantOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Instant.parse(value.trim());
    }
}
