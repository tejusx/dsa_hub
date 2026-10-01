package dsa_hub_backend.dto;

import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;

public record UserSettingsUpdateRequest(
        @NotNull @JsonAlias("sync_progress_across_sheets") Boolean syncProgressAcrossSheets
) {
}
