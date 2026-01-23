package com.yassine.donationplatform.repository;

import com.yassine.donationplatform.entity.settings.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
    Optional<AppSettings> findBySettingsKey(String settingsKey);
}
