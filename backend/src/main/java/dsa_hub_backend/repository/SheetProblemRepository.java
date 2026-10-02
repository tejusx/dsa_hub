package dsa_hub_backend.repository;

import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.entity.SheetProblem;
import dsa_hub_backend.entity.SheetProblemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SheetProblemRepository extends JpaRepository<SheetProblem, SheetProblemId> {

    @Query(value = """
            SELECT p.*
            FROM problems p
            JOIN sheet_problems sp ON sp.problem_id = p.problem_id
            LEFT JOIN LATERAL (
                SELECT o.position, o.occurrence_id
                FROM sheet_problem_occurrences o
                WHERE o.sheet_id = sp.sheet_id
                  AND o.problem_id = sp.problem_id
                  AND o.is_active = TRUE
                ORDER BY o.position ASC NULLS LAST, o.occurrence_id ASC
                LIMIT 1
            ) first_occurrence ON TRUE
            WHERE sp.sheet_id = :sheetId
              AND sp.is_active = TRUE
            ORDER BY first_occurrence.position ASC NULLS LAST,
                     first_occurrence.occurrence_id ASC NULLS LAST,
                     sp.problem_id ASC
            """, nativeQuery = true)
    List<Problem> findActiveProblemsBySheetIdOrderByOccurrencePosition(@Param("sheetId") String sheetId);

    @Query(value = """
            SELECT o.occurrence_id, o.position, o.section, o.subsection,
                   p.problem_id, p.title, p.leetcode_id, p.leetcode_slug, p.difficulty,
                   p.leetcode_paid_only, p.leetcode_has_solution, p.leetcode_has_video_solution
            FROM sheet_problems sp
            JOIN problems p ON p.problem_id = sp.problem_id
            LEFT JOIN sheet_problem_occurrences o
                   ON o.sheet_id = sp.sheet_id
                  AND o.problem_id = sp.problem_id
                  AND o.is_active = TRUE
            WHERE sp.sheet_id = :sheetId
              AND sp.is_active = TRUE
            ORDER BY o.position ASC NULLS LAST, o.occurrence_id ASC NULLS LAST, p.problem_id ASC
            """, nativeQuery = true)
    List<Object[]> findActiveOccurrencesBySheetId(@Param("sheetId") String sheetId);
}
