package com.superapp.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class SessionMetricsService {

    private final AtomicLong created = new AtomicLong(0);
    private final AtomicLong revoked = new AtomicLong(0);
    private final AtomicLong pruned = new AtomicLong(0);

    public void incCreated() {
        created.incrementAndGet();
    }

    public void incRevoked(long count) {
        if (count > 0) {
            revoked.addAndGet(count);
        }
    }

    public void incPruned(long count) {
        if (count > 0) {
            pruned.addAndGet(count);
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("sessionCreatedCount", created.get());
        body.put("sessionRevokedCount", revoked.get());
        body.put("sessionPrunedCount", pruned.get());
        return body;
    }
}

