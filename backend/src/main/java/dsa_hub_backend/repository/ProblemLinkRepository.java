package dsa_hub_backend.repository;

import dsa_hub_backend.entity.ProblemLink;
import dsa_hub_backend.entity.ProblemLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemLinkRepository extends JpaRepository<ProblemLink, ProblemLinkId> {

    List<ProblemLink> findByProblemIdOrderByLinkTypeAsc(String problemId);
}
