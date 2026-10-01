package dsa_hub_backend.repository;

import dsa_hub_backend.entity.UserSheetProblemProgress;
import dsa_hub_backend.entity.UserSheetProblemProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserSheetProblemProgressRepository
        extends JpaRepository<UserSheetProblemProgress, UserSheetProblemProgressId> {

    @Query("""
            select p from UserSheetProblemProgress p
            where p.id.userId = :userId
              and p.id.sheetId = :sheetId
              and p.id.problemId in :problemIds
            """)
    List<UserSheetProblemProgress> findForUserSheetAndProblems(@Param("userId") Long userId,
                                                               @Param("sheetId") String sheetId,
                                                               @Param("problemIds") Collection<String> problemIds);
}
