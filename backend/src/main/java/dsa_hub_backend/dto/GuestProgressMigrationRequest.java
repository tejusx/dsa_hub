package dsa_hub_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GuestProgressMigrationRequest(
        @NotNull Boolean syncProgressAcrossSheets,
        @NotNull List<@NotNull @Valid GuestProgressEntry> problemProgress,
        @NotNull List<@NotNull @Valid GuestSheetProgressEntry> sheetProblemProgress
) {
}
