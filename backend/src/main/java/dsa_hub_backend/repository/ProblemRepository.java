package dsa_hub_backend.repository;

import dsa_hub_backend.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, String> {
}
