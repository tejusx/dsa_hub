package dsa_hub_backend.controller;

import dsa_hub_backend.dto.AttemptCreateRequest;
import dsa_hub_backend.dto.AttemptResponse;
import dsa_hub_backend.service.ProblemAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problems/{problemId}/attempts")
public class ProblemAttemptController {

    private final ProblemAttemptService problemAttemptService;

    public ProblemAttemptController(ProblemAttemptService problemAttemptService) {
        this.problemAttemptService = problemAttemptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResponse addAttempt(@AuthenticationPrincipal Long userId,
                                      @PathVariable String problemId,
                                      @Valid @RequestBody AttemptCreateRequest request) {
        return problemAttemptService.addAttempt(userId, problemId, request);
    }

    @GetMapping
    public List<AttemptResponse> getAttempts(@AuthenticationPrincipal Long userId,
                                             @PathVariable String problemId) {
        return problemAttemptService.getAttempts(userId, problemId);
    }
}
