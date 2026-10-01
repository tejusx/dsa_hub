package dsa_hub_backend.repository;

import dsa_hub_backend.entity.ProblemTopic;
import dsa_hub_backend.entity.ProblemTopicId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemTopicRepository extends JpaRepository<ProblemTopic, ProblemTopicId> {

    List<ProblemTopic> findByProblemIdOrderByTopicAsc(String problemId);
}
