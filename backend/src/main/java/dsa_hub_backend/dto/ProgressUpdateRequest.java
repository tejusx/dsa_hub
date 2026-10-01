package dsa_hub_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;

public record ProgressUpdateRequest(
        @NotNull Boolean solved,
        @JsonAlias("solved_at") Instant solvedAt,
        String notes,
        @NotNull @JsonAlias("is_bookmarked") Boolean isBookmarked,
        @Pattern(regexp = "new|learning|review|mastered")
        @JsonAlias("revision_status") String revisionStatus,
        @Pattern(regexp = "Easy|Medium|Hard")
        @JsonAlias("user_difficulty") String userDifficulty,
        @JsonAlias("last_attempted_at") Instant lastAttemptedAt
) {
}
