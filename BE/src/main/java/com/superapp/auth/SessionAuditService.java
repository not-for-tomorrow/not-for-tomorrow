package com.superapp.auth;

import com.superapp.auth.dto.ClientInfo;
import com.superapp.auth.dto.SessionAuditEventPageResponse;
import com.superapp.auth.dto.SessionAuditEventResponse;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SessionAuditService {

    private final SessionAuditEventRepository repository;

    public SessionAuditService(SessionAuditEventRepository repository) {
        this.repository = repository;
    }

    public void log(SessionEventType type, String userEmail, ClientInfo clientInfo, String detail) {
        SessionAuditEvent event = new SessionAuditEvent();
        event.setEventType(type);
        event.setUserEmail(trimTo(userEmail, 120));
        event.setIpAddress(trimTo(clientInfo != null ? clientInfo.getIpAddress() : null, 64));
        event.setDeviceLabel(trimTo(clientInfo != null ? clientInfo.getDeviceLabel() : null, 120));
        event.setDetail(trimTo(detail, 300));
        repository.save(event);
    }

    public List<SessionAuditEventResponse> latestEvents() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream()
            .map(e -> new SessionAuditEventResponse(
                e.getId(),
                e.getEventType().name(),
                e.getUserEmail(),
                e.getIpAddress(),
                e.getDeviceLabel(),
                e.getDetail(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    public SessionAuditEventPageResponse search(
        String eventTypeText,
        String userEmail,
        Instant fromTime,
        Instant toTime,
        int page,
        int size
    ) {
        SessionEventType eventType = null;
        if (eventTypeText != null && !eventTypeText.trim().isEmpty()) {
            eventType = SessionEventType.valueOf(eventTypeText.trim().toUpperCase());
        }
        String normalizedEmail = (userEmail == null || userEmail.trim().isEmpty()) ? null : userEmail.trim();
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 200));

        Page<SessionAuditEvent> result = repository.search(
            eventType,
            normalizedEmail,
            fromTime,
            toTime,
            PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<SessionAuditEventResponse> items = result.getContent().stream()
            .map(e -> new SessionAuditEventResponse(
                e.getId(),
                e.getEventType().name(),
                e.getUserEmail(),
                e.getIpAddress(),
                e.getDeviceLabel(),
                e.getDetail(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
        return new SessionAuditEventPageResponse(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private String trimTo(String value, int maxLen) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        return v.length() <= maxLen ? v : v.substring(0, maxLen);
    }
}
