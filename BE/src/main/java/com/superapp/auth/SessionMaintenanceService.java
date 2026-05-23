package com.superapp.auth;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionMaintenanceService {
    private static final Logger log = LoggerFactory.getLogger(SessionMaintenanceService.class);

    private final SessionTokenRepository sessionTokenRepository;
    private final SessionMetricsService sessionMetricsService;
    private final SessionAuditService sessionAuditService;

    public SessionMaintenanceService(
        SessionTokenRepository sessionTokenRepository,
        SessionMetricsService sessionMetricsService,
        SessionAuditService sessionAuditService
    ) {
        this.sessionTokenRepository = sessionTokenRepository;
        this.sessionMetricsService = sessionMetricsService;
        this.sessionAuditService = sessionAuditService;
    }

    @Scheduled(fixedDelayString = "${app.auth.session-prune-delay-ms:3600000}")
    public void pruneExpiredSessions() {
        int deleted = pruneNow();
        if (deleted > 0) {
            log.info("Session prune job deleted {} expired sessions", deleted);
        } else {
            log.debug("Session prune job deleted 0 expired sessions");
        }
    }

    @Transactional
    public int pruneNow() {
        int deleted = sessionTokenRepository.deleteExpiredBefore(Instant.now());
        sessionMetricsService.incPruned(deleted);
        if (deleted > 0) {
            sessionAuditService.log(
                SessionEventType.SESSION_PRUNED,
                "SYSTEM",
                null,
                "expired sessions pruned count=" + deleted
            );
        }
        return deleted;
    }
}
