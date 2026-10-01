package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_problem_attempts")
public class UserProblemAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private String problemId;

    @Column(name = "sheet_id")
    private String sheetId;

    @Column(name = "coding_platform")
    private String codingPlatform;

    @Column(nullable = false)
    private String outcome;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    private String notes;

    public UserProblemAttempt() {
    }

    public Long getAttemptId() { return attemptId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }
    public String getSheetId() { return sheetId; }
    public void setSheetId(String sheetId) { this.sheetId = sheetId; }
    public String getCodingPlatform() { return codingPlatform; }
    public void setCodingPlatform(String codingPlatform) { this.codingPlatform = codingPlatform; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Instant getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Instant attemptedAt) { this.attemptedAt = attemptedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
