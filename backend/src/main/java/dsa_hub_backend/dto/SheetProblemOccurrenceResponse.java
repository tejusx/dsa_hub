package dsa_hub_backend.dto;

import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.entity.ProblemLink;

import java.util.List;

/** One appearance of a canonical problem in a sheet. */
public record SheetProblemOccurrenceResponse(
        Long occurrenceId,
        Integer position,
        String section,
        String subsection,
        Problem problem,
        List<ProblemLink> links
) {
}
