package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_sheet_problem_progress")
public class UserSheetProblemProgress {

    @EmbeddedId
    private UserSheetProblemProgressId id;

    @Column(nullable = false)
    private boolean solved;

    @Column(name = "solved_at")
    private Instant solvedAt;

    private String notes;

    @Column(name = "is_bookmarked", nullable = false)
    private boolean bookmarked;

    @Column(name = "revision_status")
    private String revisionStatus;

    @Column(name = "user_difficulty")
    private String userDifficulty;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserSheetProblemProgress() {
    }

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public UserSheetProblemProgressId getId() { return id; }
    public void setId(UserSheetProblemProgressId id) { this.id = id; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
    public Instant getSolvedAt() { return solvedAt; }
    public void setSolvedAt(Instant solvedAt) { this.solvedAt = solvedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
    public String getRevisionStatus() { return revisionStatus; }
    public void setRevisionStatus(String revisionStatus) { this.revisionStatus = revisionStatus; }
    public String getUserDifficulty() { return userDifficulty; }
    public void setUserDifficulty(String userDifficulty) { this.userDifficulty = userDifficulty; }
    public Instant getLastAttemptedAt() { return lastAttemptedAt; }
    public void setLastAttemptedAt(Instant lastAttemptedAt) { this.lastAttemptedAt = lastAttemptedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
