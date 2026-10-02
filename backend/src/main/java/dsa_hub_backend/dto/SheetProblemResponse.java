package dsa_hub_backend.dto;

import dsa_hub_backend.entity.ProblemLink;

import java.util.List;

/** A canonical problem together with its occurrence-specific placement in a sheet. */
public record SheetProblemResponse(
        String problemId,
        String title,
        String leetcodeId,
        String leetcodeSlug,
        String difficulty,
        Boolean leetcodePaidOnly,
        Boolean leetcodeHasSolution,
        Boolean leetcodeHasVideoSolution,
        Long occurrenceId,
        Integer position,
        String section,
        List<String> sectionPath,
        String topic,
        List<ProblemLink> links
) {
}
