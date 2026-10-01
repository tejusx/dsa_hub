package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserProblemProgressId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "problem_id")
    private String problemId;

    public UserProblemProgressId() {
    }

    public UserProblemProgressId(Long userId, String problemId) {
        this.userId = userId;
        this.problemId = problemId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProblemId() {
        return problemId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserProblemProgressId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(problemId, that.problemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, problemId);
    }
}
