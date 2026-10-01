package dsa_hub_backend.repository;

import dsa_hub_backend.entity.Sheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SheetRepository extends JpaRepository<Sheet, String> {

    List<Sheet> findByEnabledTrueOrderByDisplayOrderAscSheetIdAsc();

    @Query(value = """
            SELECT s.*
            FROM sheets s
            JOIN sheet_problems sp ON sp.sheet_id = s.sheet_id
            WHERE sp.problem_id = :problemId
              AND sp.is_active = TRUE
              AND s.is_enabled = TRUE
            ORDER BY s.display_order ASC, s.sheet_id ASC
            """, nativeQuery = true)
    List<Sheet> findEnabledByProblemIdOrderByDisplayOrder(@Param("problemId") String problemId);
}
