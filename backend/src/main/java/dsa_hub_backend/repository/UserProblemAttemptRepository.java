package dsa_hub_backend.repository;

import dsa_hub_backend.entity.UserProblemAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProblemAttemptRepository extends JpaRepository<UserProblemAttempt, Long> {

    List<UserProblemAttempt> findByUserIdAndProblemIdOrderByAttemptedAtDescAttemptIdDesc(Long userId,
                                                                                         String problemId);
}
