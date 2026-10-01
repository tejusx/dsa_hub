package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sync_progress_across_sheets", nullable = false)
    private boolean syncProgressAcrossSheets;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    public UserSettings() {
    }

    @PreUpdate
    public void touchUpdatedAt() {
        updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isSyncProgressAcrossSheets() {
        return syncProgressAcrossSheets;
    }

    public void setSyncProgressAcrossSheets(boolean syncProgressAcrossSheets) {
        this.syncProgressAcrossSheets = syncProgressAcrossSheets;
    }
}
