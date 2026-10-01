package dsa_hub_backend.controller;

import dsa_hub_backend.dto.UserSettingsResponse;
import dsa_hub_backend.dto.UserSettingsUpdateRequest;
import dsa_hub_backend.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public UserSettingsResponse getSettings(@AuthenticationPrincipal Long userId) {
        return userSettingsService.getSettings(userId);
    }

    @PatchMapping
    public UserSettingsResponse updateSettings(@AuthenticationPrincipal Long userId,
                                               @Valid @RequestBody UserSettingsUpdateRequest request) {
        return userSettingsService.updateSettings(userId, request);
    }
}
