package dsa_hub_backend.service;

import dsa_hub_backend.dto.ProgressResponse;
import dsa_hub_backend.dto.ProgressUpdateRequest;
import dsa_hub_backend.dto.GuestProgressEntry;
import dsa_hub_backend.dto.GuestProgressMigrationRequest;
import dsa_hub_backend.dto.GuestProgressMigrationResponse;
import dsa_hub_backend.dto.GuestSheetProgressEntry;
import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.entity.SheetProblem;
import dsa_hub_backend.entity.SheetProblemId;
import dsa_hub_backend.entity.UserProblemProgress;
import dsa_hub_backend.entity.UserProblemProgressId;
import dsa_hub_backend.entity.UserSheetProblemProgress;
import dsa_hub_backend.entity.UserSheetProblemProgressId;
import dsa_hub_backend.repository.ProblemRepository;
import dsa_hub_backend.repository.SheetProblemRepository;
import dsa_hub_backend.repository.SheetRepository;
import dsa_hub_backend.repository.UserProblemProgressRepository;
import dsa_hub_backend.repository.UserSettingsRepository;
import dsa_hub_backend.repository.UserSheetProblemProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProgressService {

    private final UserProblemProgressRepository globalProgressRepository;
    private final UserSheetProblemProgressRepository sheetProgressRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ProblemRepository problemRepository;
    private final SheetRepository sheetRepository;
    private final SheetProblemRepository sheetProblemRepository;

    public ProgressService(UserProblemProgressRepository globalProgressRepository,
                           UserSheetProblemProgressRepository sheetProgressRepository,
                           UserSettingsRepository userSettingsRepository,
                           ProblemRepository problemRepository,
                           SheetRepository sheetRepository,
                           SheetProblemRepository sheetProblemRepository) {
        this.globalProgressRepository = globalProgressRepository;
        this.sheetProgressRepository = sheetProgressRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.problemRepository = problemRepository;
        this.sheetRepository = sheetRepository;
        this.sheetProblemRepository = sheetProblemRepository;
    }

    /** Imports one guest progress mode atomically. Existing progress is merged without erasing account data. */
    @Transactional
    public GuestProgressMigrationResponse migrateGuestProgress(Long userId, GuestProgressMigrationRequest request) {
        boolean syncAcrossSheets = request.syncProgressAcrossSheets();
        if (syncAcrossSheets != isSyncEnabled(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Guest sync mode must match the account sync setting before migration");
        }
        if (syncAcrossSheets && !request.sheetProblemProgress().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sheetProblemProgress must be empty when syncProgressAcrossSheets is true");
        }
        if (!syncAcrossSheets && !request.problemProgress().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "problemProgress must be empty when syncProgressAcrossSheets is false");
        }

        // Validate every reference before saving anything, so invalid requests can be safely retried.
        Set<String> globalProblemIds = new HashSet<>();
        for (GuestProgressEntry entry : request.problemProgress()) {
            if (!problemRepository.existsById(entry.problemId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown problemId: " + entry.problemId());
            }
            if (!globalProblemIds.add(entry.problemId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate global progress for problemId: " + entry.problemId());
            }
        }

        Set<String> sheetKeys = new HashSet<>();
        for (GuestSheetProgressEntry entry : request.sheetProblemProgress()) {
            String key = entry.sheetId().length() + ":" + entry.sheetId() + entry.problemId();
            if (!sheetKeys.add(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate sheet progress for sheetId/problemId: " + entry.sheetId() + "/" + entry.problemId());
            }
            if (!problemRepository.existsById(entry.problemId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown problemId: " + entry.problemId());
            }
            if (sheetProblemRepository.findById(new SheetProblemId(entry.sheetId(), entry.problemId())).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown sheet/problem relationship: " + entry.sheetId() + "/" + entry.problemId());
            }
        }

        if (syncAcrossSheets) {
            List<UserProblemProgress> toSave = new ArrayList<>();
            for (GuestProgressEntry entry : request.problemProgress()) {
                UserProblemProgress progress = globalProgressRepository
                        .findById(new UserProblemProgressId(userId, entry.problemId()))
                        .orElseGet(() -> {
                            UserProblemProgress created = new UserProblemProgress();
                            created.setId(new UserProblemProgressId(userId, entry.problemId()));
                            return created;
                        });
                merge(progress, entry);
                toSave.add(progress);
            }
            globalProgressRepository.saveAll(toSave);
            return new GuestProgressMigrationResponse(true, toSave.size(), 0);
        }

        List<UserSheetProblemProgress> toSave = new ArrayList<>();
        for (GuestSheetProgressEntry entry : request.sheetProblemProgress()) {
            UserSheetProblemProgress progress = sheetProgressRepository
                    .findById(new UserSheetProblemProgressId(userId, entry.sheetId(), entry.problemId()))
                    .orElseGet(() -> {
                        UserSheetProblemProgress created = new UserSheetProblemProgress();
                        created.setId(new UserSheetProblemProgressId(userId, entry.sheetId(), entry.problemId()));
                        return created;
                    });
            merge(progress, entry);
            toSave.add(progress);
        }
        sheetProgressRepository.saveAll(toSave);
        return new GuestProgressMigrationResponse(true, 0, toSave.size());
    }

    private void merge(UserProblemProgress target, GuestProgressEntry guest) {
        target.setSolved(target.isSolved() || guest.solved());
        target.setSolvedAt(later(target.getSolvedAt(), guest.solvedAt()));
        target.setNotes(keepExisting(target.getNotes(), guest.notes()));
        target.setBookmarked(target.isBookmarked() || guest.isBookmarked());
        target.setRevisionStatus(keepExisting(target.getRevisionStatus(), guest.revisionStatus()));
        target.setUserDifficulty(keepExisting(target.getUserDifficulty(), guest.userDifficulty()));
        target.setLastAttemptedAt(later(target.getLastAttemptedAt(), guest.lastAttemptedAt()));
        if (target.isSolved() && target.getSolvedAt() == null) target.setSolvedAt(Instant.now());
    }

    private void merge(UserSheetProblemProgress target, GuestSheetProgressEntry guest) {
        target.setSolved(target.isSolved() || guest.solved());
        target.setSolvedAt(later(target.getSolvedAt(), guest.solvedAt()));
        target.setNotes(keepExisting(target.getNotes(), guest.notes()));
        target.setBookmarked(target.isBookmarked() || guest.isBookmarked());
        target.setRevisionStatus(keepExisting(target.getRevisionStatus(), guest.revisionStatus()));
        target.setUserDifficulty(keepExisting(target.getUserDifficulty(), guest.userDifficulty()));
        target.setLastAttemptedAt(later(target.getLastAttemptedAt(), guest.lastAttemptedAt()));
        if (target.isSolved() && target.getSolvedAt() == null) target.setSolvedAt(Instant.now());
    }

    private Instant later(Instant existing, Instant incoming) {
        if (existing == null) return incoming;
        if (incoming == null) return existing;
        return existing.isAfter(incoming) ? existing : incoming;
    }

    private String keepExisting(String existing, String incoming) {
        return existing == null || existing.isBlank() ? incoming : existing;
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getAllGlobalProgress(Long userId) {
        return globalProgressRepository.findAllForUser(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressResponse getGlobalProgress(Long userId, String problemId) {
        requireProblem(problemId);
        return globalProgressRepository.findById(new UserProblemProgressId(userId, problemId))
                .map(this::toResponse)
                .orElseGet(() -> emptyProgress(problemId));
    }

    @Transactional
    public ProgressResponse updateGlobalProgress(Long userId, String problemId, ProgressUpdateRequest request) {
        requireProblem(problemId);
        UserProblemProgress progress = globalProgressRepository
                .findById(new UserProblemProgressId(userId, problemId))
                .orElseGet(() -> {
                    UserProblemProgress created = new UserProblemProgress();
                    created.setId(new UserProblemProgressId(userId, problemId));
                    return created;
                });
        applyUpdate(progress, request);
        return toResponse(globalProgressRepository.save(progress));
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getSheetProgress(Long userId, String sheetId) {
        List<Problem> problems = getActiveSheetProblems(sheetId);
        if (problems.isEmpty()) {
            return List.of();
        }

        List<String> problemIds = problems.stream().map(Problem::getProblemId).toList();
        Map<String, ProgressResponse> progressByProblem = new HashMap<>();
        if (isSyncEnabled(userId)) {
            globalProgressRepository.findForUserAndProblems(userId, problemIds).stream()
                    .map(this::toResponse)
                    .forEach(progress -> progressByProblem.put(progress.problemId(), progress));
        } else {
            sheetProgressRepository.findForUserSheetAndProblems(userId, sheetId, problemIds).stream()
                    .map(this::toResponse)
                    .forEach(progress -> progressByProblem.put(progress.problemId(), progress));
        }

        List<ProgressResponse> result = new ArrayList<>(problems.size());
        for (Problem problem : problems) {
            result.add(progressByProblem.getOrDefault(problem.getProblemId(), emptyProgress(problem.getProblemId())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ProgressResponse getSheetProblemProgress(Long userId, String sheetId, String problemId) {
        requireActiveSheetProblem(sheetId, problemId);
        if (isSyncEnabled(userId)) {
            return globalProgressRepository.findById(new UserProblemProgressId(userId, problemId))
                    .map(this::toResponse)
                    .orElseGet(() -> emptyProgress(problemId));
        }
        return sheetProgressRepository.findById(new UserSheetProblemProgressId(userId, sheetId, problemId))
                .map(this::toResponse)
                .orElseGet(() -> emptyProgress(problemId));
    }

    @Transactional
    public ProgressResponse updateSheetProblemProgress(Long userId,
                                                      String sheetId,
                                                      String problemId,
                                                      ProgressUpdateRequest request) {
        requireActiveSheetProblem(sheetId, problemId);
        if (isSyncEnabled(userId)) {
            UserProblemProgress progress = globalProgressRepository
                    .findById(new UserProblemProgressId(userId, problemId))
                    .orElseGet(() -> {
                        UserProblemProgress created = new UserProblemProgress();
                        created.setId(new UserProblemProgressId(userId, problemId));
                        return created;
                    });
            applyUpdate(progress, request);
            return toResponse(globalProgressRepository.save(progress));
        }

        UserSheetProblemProgress progress = sheetProgressRepository
                .findById(new UserSheetProblemProgressId(userId, sheetId, problemId))
                .orElseGet(() -> {
                    UserSheetProblemProgress created = new UserSheetProblemProgress();
                    created.setId(new UserSheetProblemProgressId(userId, sheetId, problemId));
                    return created;
                });
        applyUpdate(progress, request);
        return toResponse(sheetProgressRepository.save(progress));
    }

    private List<Problem> getActiveSheetProblems(String sheetId) {
        requireSheet(sheetId);
        return sheetProblemRepository.findActiveProblemsBySheetIdOrderByOccurrencePosition(sheetId);
    }

    private void requireActiveSheetProblem(String sheetId, String problemId) {
        requireSheet(sheetId);
        boolean activeMembership = sheetProblemRepository.findById(new SheetProblemId(sheetId, problemId))
                .filter(SheetProblem::isActive)
                .isPresent();
        if (!activeMembership) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active sheet/problem relationship not found");
        }
    }

    private void requireSheet(String sheetId) {
        sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
    }

    private boolean isSyncEnabled(Long userId) {
        return userSettingsRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User settings not found"))
                .isSyncProgressAcrossSheets();
    }

    private void requireProblem(String problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }
    }

    private void applyUpdate(UserProblemProgress progress, ProgressUpdateRequest request) {
        progress.setSolved(request.solved());
        progress.setSolvedAt(request.solvedAt() == null && request.solved() ? Instant.now() : request.solvedAt());
        progress.setNotes(request.notes());
        progress.setBookmarked(request.isBookmarked());
        progress.setRevisionStatus(request.revisionStatus());
        progress.setUserDifficulty(request.userDifficulty());
        progress.setLastAttemptedAt(request.lastAttemptedAt());
    }

    private void applyUpdate(UserSheetProblemProgress progress, ProgressUpdateRequest request) {
        progress.setSolved(request.solved());
        progress.setSolvedAt(request.solvedAt() == null && request.solved() ? Instant.now() : request.solvedAt());
        progress.setNotes(request.notes());
        progress.setBookmarked(request.isBookmarked());
        progress.setRevisionStatus(request.revisionStatus());
        progress.setUserDifficulty(request.userDifficulty());
        progress.setLastAttemptedAt(request.lastAttemptedAt());
    }

    private ProgressResponse toResponse(UserProblemProgress progress) {
        return new ProgressResponse(progress.getId().getProblemId(), progress.isSolved(), progress.getSolvedAt(),
                progress.getNotes(), progress.isBookmarked(), progress.getRevisionStatus(),
                progress.getUserDifficulty(), progress.getLastAttemptedAt(), progress.getUpdatedAt());
    }

    private ProgressResponse toResponse(UserSheetProblemProgress progress) {
        return new ProgressResponse(progress.getId().getProblemId(), progress.isSolved(), progress.getSolvedAt(),
                progress.getNotes(), progress.isBookmarked(), progress.getRevisionStatus(),
                progress.getUserDifficulty(), progress.getLastAttemptedAt(), progress.getUpdatedAt());
    }

    private ProgressResponse emptyProgress(String problemId) {
        return new ProgressResponse(problemId, false, null, null, false, null, null, null, null);
    }
}
