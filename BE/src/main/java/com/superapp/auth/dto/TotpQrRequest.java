package com.superapp.auth.dto;

import javax.validation.constraints.NotBlank;

public class TotpQrRequest {

    @NotBlank
    private String otpauthUrl;

    public String getOtpauthUrl() {
        return otpauthUrl;
    }

    public void setOtpauthUrl(String otpauthUrl) {
        this.otpauthUrl = otpauthUrl;
    }
}
