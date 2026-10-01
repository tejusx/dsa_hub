package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "problem_topics")
@IdClass(ProblemTopicId.class)
public class ProblemTopic {

    @Id
    @Column(name = "problem_id", nullable = false)
    private String problemId;

    @Id
    @Column(nullable = false)
    private String topic;

    public ProblemTopic() {
    }

    public String getProblemId() {
        return problemId;
    }

    public String getTopic() {
        return topic;
    }
}
