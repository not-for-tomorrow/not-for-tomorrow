package com.superapp.auth;

import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionAuditEventRepository extends JpaRepository<SessionAuditEvent, Long> {
    List<SessionAuditEvent> findTop100ByOrderByCreatedAtDesc();

    @Query("select e from SessionAuditEvent e " +
        "where (:eventType is null or e.eventType = :eventType) " +
        "and (:userEmail is null or lower(e.userEmail) = lower(:userEmail)) " +
        "and (:fromTime is null or e.createdAt >= :fromTime) " +
        "and (:toTime is null or e.createdAt <= :toTime)")
    Page<SessionAuditEvent> search(
        @Param("eventType") SessionEventType eventType,
        @Param("userEmail") String userEmail,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        Pageable pageable
    );
}
