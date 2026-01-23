package com.yassine.donationplatform.entity.settings;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;

@Entity
@Table(name = "app_settings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settings_key", nullable = false, unique = true, length = 50)
    private String settingsKey;


    @Column(name = "settings_json", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(
            read = "settings_json::text",
            write = "?::jsonb"
    )
    private String settingsJson;


    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
