package dsa_hub_backend.dto;

public record GuestProgressMigrationResponse(
        boolean success,
        int migratedProblems,
        int migratedSheetProblems
) {
}
