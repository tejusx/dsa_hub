package dsa_hub_backend.repository;

import dsa_hub_backend.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
