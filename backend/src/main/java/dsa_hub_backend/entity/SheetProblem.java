package dsa_hub_backend.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "sheet_problems")
public class SheetProblem {

    @EmbeddedId
    private SheetProblemId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("sheetId")
    @JoinColumn(name = "sheet_id", nullable = false)
    private Sheet sheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("problemId")
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public SheetProblem() {
    }

    public SheetProblemId getId() {
        return id;
    }

    public void setId(SheetProblemId id) {
        this.id = id;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public void setSheet(Sheet sheet) {
        this.sheet = sheet;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
