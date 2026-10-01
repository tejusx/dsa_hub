package dsa_hub_backend.dto;

import java.time.Instant;

public record AttemptResponse(Long attemptId,
                              String problemId,
                              String sheetId,
                              String codingPlatform,
                              String outcome,
                              Instant attemptedAt,
                              String notes) {
}
