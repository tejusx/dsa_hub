package dsa_hub_backend.repository;

import dsa_hub_backend.entity.UserProblemProgress;
import dsa_hub_backend.entity.UserProblemProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserProblemProgressRepository extends JpaRepository<UserProblemProgress, UserProblemProgressId> {

    @Query("select p from UserProblemProgress p where p.id.userId = :userId order by p.updatedAt desc")
    List<UserProblemProgress> findAllForUser(@Param("userId") Long userId);

    @Query("select p from UserProblemProgress p where p.id.userId = :userId and p.id.problemId in :problemIds")
    List<UserProblemProgress> findForUserAndProblems(@Param("userId") Long userId,
                                                     @Param("problemIds") Collection<String> problemIds);
}
