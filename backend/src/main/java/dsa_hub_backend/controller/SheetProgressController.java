package dsa_hub_backend.controller;

import dsa_hub_backend.dto.ProgressResponse;
import dsa_hub_backend.dto.ProgressUpdateRequest;
import dsa_hub_backend.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sheets/{sheetId}/progress")
public class SheetProgressController {

    private final ProgressService progressService;

    public SheetProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public List<ProgressResponse> getSheetProgress(@AuthenticationPrincipal Long userId,
                                                   @PathVariable String sheetId) {
        return progressService.getSheetProgress(userId, sheetId);
    }

    @GetMapping("/{problemId}")
    public ProgressResponse getProblemProgress(@AuthenticationPrincipal Long userId,
                                               @PathVariable String sheetId,
                                               @PathVariable String problemId) {
        return progressService.getSheetProblemProgress(userId, sheetId, problemId);
    }

    @PutMapping("/{problemId}")
    public ProgressResponse updateProblemProgress(@AuthenticationPrincipal Long userId,
                                                  @PathVariable String sheetId,
                                                  @PathVariable String problemId,
                                                  @Valid @RequestBody ProgressUpdateRequest request) {
        return progressService.updateSheetProblemProgress(userId, sheetId, problemId, request);
    }
}
