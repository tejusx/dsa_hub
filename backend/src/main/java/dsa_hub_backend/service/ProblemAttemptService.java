package dsa_hub_backend.service;

import dsa_hub_backend.dto.AttemptCreateRequest;
import dsa_hub_backend.dto.AttemptResponse;
import dsa_hub_backend.entity.SheetProblemId;
import dsa_hub_backend.entity.UserProblemAttempt;
import dsa_hub_backend.repository.ProblemRepository;
import dsa_hub_backend.repository.SheetProblemRepository;
import dsa_hub_backend.repository.UserProblemAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ProblemAttemptService {

    private final UserProblemAttemptRepository attemptRepository;
    private final ProblemRepository problemRepository;
    private final SheetProblemRepository sheetProblemRepository;

    public ProblemAttemptService(UserProblemAttemptRepository attemptRepository,
                                 ProblemRepository problemRepository,
                                 SheetProblemRepository sheetProblemRepository) {
        this.attemptRepository = attemptRepository;
        this.problemRepository = problemRepository;
        this.sheetProblemRepository = sheetProblemRepository;
    }

    @Transactional
    public AttemptResponse addAttempt(Long userId, String problemId, AttemptCreateRequest request) {
        requireProblem(problemId);
        if (request.sheetId() != null
                && !sheetProblemRepository.existsById(new SheetProblemId(request.sheetId(), problemId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet/problem relationship not found");
        }

        UserProblemAttempt attempt = new UserProblemAttempt();
        attempt.setUserId(userId);
        attempt.setProblemId(problemId);
        attempt.setSheetId(request.sheetId());
        attempt.setCodingPlatform(request.codingPlatform());
        attempt.setOutcome(request.outcome());
        attempt.setNotes(request.notes());
        attempt.setAttemptedAt(Instant.now());
        return toResponse(attemptRepository.save(attempt));
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getAttempts(Long userId, String problemId) {
        requireProblem(problemId);
        return attemptRepository.findByUserIdAndProblemIdOrderByAttemptedAtDescAttemptIdDesc(userId, problemId)
                .stream().map(this::toResponse).toList();
    }

    private void requireProblem(String problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
    }

    private AttemptResponse toResponse(UserProblemAttempt attempt) {
        return new AttemptResponse(attempt.getAttemptId(), attempt.getProblemId(), attempt.getSheetId(),
                attempt.getCodingPlatform(), attempt.getOutcome(), attempt.getAttemptedAt(), attempt.getNotes());
    }
}
