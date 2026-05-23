package com.superapp.auth.oauth.dto;

import com.superapp.auth.dto.TokenPairResponse;

public class OAuthCallbackResponse {
    private String provider;
    private TokenPairResponse tokens;
    private boolean created;

    public OAuthCallbackResponse(String provider, TokenPairResponse tokens, boolean created) {
        this.provider = provider;
        this.tokens = tokens;
        this.created = created;
    }

    public String getProvider() {
        return provider;
    }

    public TokenPairResponse getTokens() {
        return tokens;
    }

    public boolean isCreated() {
        return created;
    }
}
