package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserSheetProblemProgressId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sheet_id")
    private String sheetId;

    @Column(name = "problem_id")
    private String problemId;

    public UserSheetProblemProgressId() {
    }

    public UserSheetProblemProgressId(Long userId, String sheetId, String problemId) {
        this.userId = userId;
        this.sheetId = sheetId;
        this.problemId = problemId;
    }

    public Long getUserId() { return userId; }
    public String getSheetId() { return sheetId; }
    public String getProblemId() { return problemId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserSheetProblemProgressId that)) return false;
        return Objects.equals(userId, that.userId)
                && Objects.equals(sheetId, that.sheetId)
                && Objects.equals(problemId, that.problemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, sheetId, problemId);
    }
}
