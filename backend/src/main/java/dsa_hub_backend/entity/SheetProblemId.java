package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SheetProblemId implements Serializable {

    @Column(name = "sheet_id")
    private String sheetId;

    @Column(name = "problem_id")
    private String problemId;

    public SheetProblemId() {
    }

    public SheetProblemId(String sheetId, String problemId) {
        this.sheetId = sheetId;
        this.problemId = problemId;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getProblemId() {
        return problemId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SheetProblemId that)) return false;
        return Objects.equals(sheetId, that.sheetId) && Objects.equals(problemId, that.problemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sheetId, problemId);
    }
}
