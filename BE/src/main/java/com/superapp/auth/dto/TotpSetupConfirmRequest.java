package com.superapp.auth.dto;

import javax.validation.constraints.NotBlank;

public class TotpSetupConfirmRequest {

    @NotBlank
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
