package dsa_hub_backend.service;

import dsa_hub_backend.dto.SheetProblemOccurrenceResponse;
import dsa_hub_backend.dto.SheetProblemResponse;
import dsa_hub_backend.entity.Sheet;
import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.entity.ProblemLink;
import dsa_hub_backend.repository.ProblemLinkRepository;
import dsa_hub_backend.repository.SheetRepository;
import dsa_hub_backend.repository.SheetProblemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SheetService {

    private final SheetRepository sheetRepository;
    private final SheetProblemRepository sheetProblemRepository;
    private final ProblemLinkRepository problemLinkRepository;

    public SheetService(SheetRepository sheetRepository,
                        SheetProblemRepository sheetProblemRepository,
                        ProblemLinkRepository problemLinkRepository) {
        this.sheetRepository = sheetRepository;
        this.sheetProblemRepository = sheetProblemRepository;
        this.problemLinkRepository = problemLinkRepository;
    }

    public List<Sheet> getEnabledSheets() {
        return sheetRepository.findByEnabledTrueOrderByDisplayOrderAscSheetIdAsc();
    }

    public Sheet getSheetById(String sheetId) {
        return sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
    }

    public List<Problem> getProblemsForSheet(String sheetId) {
        getSheetById(sheetId);
        return sheetProblemRepository.findActiveProblemsBySheetIdOrderByOccurrencePosition(sheetId);
    }

    public List<SheetProblemResponse> getProblemResponsesForSheet(String sheetId) {
        getSheetById(sheetId);
        List<Object[]> rows = sheetProblemRepository.findActiveOccurrencesBySheetId(sheetId);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> problemIds = rows.stream().map(row -> (String) row[4]).distinct().toList();
        Map<String, List<ProblemLink>> linksByProblemId = problemLinkRepository
                .findByProblemIdInOrderByProblemIdAscLinkTypeAsc(problemIds)
                .stream()
                .collect(Collectors.groupingBy(ProblemLink::getProblemId, java.util.LinkedHashMap::new,
                        Collectors.toList()));

        return rows.stream().map(row -> {
            String section = (String) row[2];
            String topic = (String) row[3];
            List<String> sectionPath = java.util.stream.Stream.of(section, topic)
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
            String problemId = (String) row[4];

            return new SheetProblemResponse(
                    problemId,
                    (String) row[5],
                    (String) row[6],
                    (String) row[7],
                    (String) row[8],
                    (Boolean) row[9],
                    (Boolean) row[10],
                    (Boolean) row[11],
                    row[0] == null ? null : ((Number) row[0]).longValue(),
                    row[1] == null ? null : ((Number) row[1]).intValue(),
                    section,
                    sectionPath,
                    topic,
                    linksByProblemId.getOrDefault(problemId, List.of())
            );
        }).toList();
    }

    public List<SheetProblemOccurrenceResponse> getProblemOccurrencesForSheet(String sheetId) {
        getSheetById(sheetId);
        List<Object[]> rows = sheetProblemRepository.findActiveOccurrencesBySheetId(sheetId);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> problemIds = rows.stream().map(row -> (String) row[4]).distinct().toList();
        Map<String, List<ProblemLink>> linksByProblemId = problemLinkRepository
                .findByProblemIdInOrderByProblemIdAscLinkTypeAsc(problemIds)
                .stream()
                .collect(Collectors.groupingBy(ProblemLink::getProblemId, java.util.LinkedHashMap::new,
                        Collectors.toList()));

        return rows.stream().map(row -> {
            Problem problem = new Problem();
            problem.setProblemId((String) row[4]);
            problem.setTitle((String) row[5]);
            problem.setLeetcodeId((String) row[6]);
            problem.setLeetcodeSlug((String) row[7]);
            problem.setDifficulty((String) row[8]);
            problem.setLeetcodePaidOnly((Boolean) row[9]);
            problem.setLeetcodeHasSolution((Boolean) row[10]);
            problem.setLeetcodeHasVideoSolution((Boolean) row[11]);

            return new SheetProblemOccurrenceResponse(
                    row[0] == null ? null : ((Number) row[0]).longValue(),
                    row[1] == null ? null : ((Number) row[1]).intValue(),
                    (String) row[2],
                    (String) row[3],
                    problem,
                    linksByProblemId.getOrDefault(problem.getProblemId(), List.of())
            );
        }).toList();
    }
}
