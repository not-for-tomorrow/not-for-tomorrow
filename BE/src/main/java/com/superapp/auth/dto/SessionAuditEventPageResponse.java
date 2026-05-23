package com.superapp.auth.dto;

import java.util.List;

public class SessionAuditEventPageResponse {
    private List<SessionAuditEventResponse> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;

    public SessionAuditEventPageResponse(
        List<SessionAuditEventResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
    ) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public List<SessionAuditEventResponse> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }
}

