package com.superapp.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank
    @Size(min = 2, max = 120)
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

