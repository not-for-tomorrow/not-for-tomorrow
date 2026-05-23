package com.superapp.auth.dto;

import javax.validation.constraints.NotBlank;

public class MfaLoginVerifyRequest {

    @NotBlank
    private String challengeTicket;

    @NotBlank
    private String code;

    public String getChallengeTicket() {
        return challengeTicket;
    }

    public void setChallengeTicket(String challengeTicket) {
        this.challengeTicket = challengeTicket;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
