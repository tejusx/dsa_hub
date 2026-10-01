package dsa_hub_backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProblemTopicId implements Serializable {

    private String problemId;
    private String topic;

    public ProblemTopicId() {
    }

    public ProblemTopicId(String problemId, String topic) {
        this.problemId = problemId;
        this.topic = topic;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProblemTopicId that)) return false;
        return Objects.equals(problemId, that.problemId) && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemId, topic);
    }
}
