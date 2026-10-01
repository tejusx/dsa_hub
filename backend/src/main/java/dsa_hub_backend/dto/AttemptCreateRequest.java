package dsa_hub_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonAlias;

public record AttemptCreateRequest(
        @JsonAlias("sheet_id") String sheetId,
        @JsonAlias("coding_platform") String codingPlatform,
        @NotBlank @Pattern(regexp = "attempted|solved|failed") String outcome,
        String notes
) {
}
