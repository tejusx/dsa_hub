package dsa_hub_backend.controller;

import dsa_hub_backend.dto.ProgressResponse;
import dsa_hub_backend.dto.ProgressUpdateRequest;
import dsa_hub_backend.dto.GuestProgressMigrationRequest;
import dsa_hub_backend.dto.GuestProgressMigrationResponse;
import dsa_hub_backend.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping("/migrate-guest")
    public GuestProgressMigrationResponse migrateGuestProgress(@AuthenticationPrincipal Long userId,
                                                               @Valid @RequestBody GuestProgressMigrationRequest request) {
        return progressService.migrateGuestProgress(userId, request);
    }

    @GetMapping
    public List<ProgressResponse> getAllProgress(@AuthenticationPrincipal Long userId) {
        return progressService.getAllGlobalProgress(userId);
    }

    @GetMapping("/{problemId}")
    public ProgressResponse getProgress(@AuthenticationPrincipal Long userId, @PathVariable String problemId) {
        return progressService.getGlobalProgress(userId, problemId);
    }

    @PutMapping("/{problemId}")
    public ProgressResponse updateProgress(@AuthenticationPrincipal Long userId,
                                           @PathVariable String problemId,
                                           @Valid @RequestBody ProgressUpdateRequest request) {
        return progressService.updateGlobalProgress(userId, problemId, request);
    }
}
