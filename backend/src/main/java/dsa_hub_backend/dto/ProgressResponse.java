package dsa_hub_backend.dto;

import java.time.Instant;

public record ProgressResponse(
        String problemId,
        boolean solved,
        Instant solvedAt,
        String notes,
        boolean isBookmarked,
        String revisionStatus,
        String userDifficulty,
        Instant lastAttemptedAt,
        Instant updatedAt
) {
}
