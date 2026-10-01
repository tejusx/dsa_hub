package dsa_hub_backend.service;

import dsa_hub_backend.dto.UserSettingsResponse;
import dsa_hub_backend.dto.UserSettingsUpdateRequest;
import dsa_hub_backend.entity.UserSettings;
import dsa_hub_backend.repository.UserSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(Long userId) {
        return toResponse(findSettings(userId));
    }

    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
        UserSettings settings = findSettings(userId);
        settings.setSyncProgressAcrossSheets(request.syncProgressAcrossSheets());
        return toResponse(userSettingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public boolean shouldSyncProgress(Long userId) {
        return findSettings(userId).isSyncProgressAcrossSheets();
    }

    private UserSettings findSettings(Long userId) {
        return userSettingsRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User settings not found"));
    }

    private UserSettingsResponse toResponse(UserSettings settings) {
        return new UserSettingsResponse(settings.isSyncProgressAcrossSheets());
    }
}
