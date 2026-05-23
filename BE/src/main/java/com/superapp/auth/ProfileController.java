package com.superapp.auth;

import com.superapp.auth.dto.ProfileResponse;
import com.superapp.auth.dto.UpdateProfileRequest;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        UserAccount user = authService.getCurrentUserByEmail(authentication.getName());
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new ProfileResponse(
            user.getEmail(),
            user.getPhone(),
            user.getDisplayName(),
            user.isMfaEnabled(),
            user.isMfaTotpEnabled(),
            roles
        );
    }

    @PatchMapping("/me")
    public ProfileResponse updateMe(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        UserAccount user = authService.getCurrentUserByEmail(authentication.getName());
        user.setDisplayName(request.getDisplayName().trim());
        authService.save(user);
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new ProfileResponse(
            user.getEmail(),
            user.getPhone(),
            user.getDisplayName(),
            user.isMfaEnabled(),
            user.isMfaTotpEnabled(),
            roles
        );
    }
}
