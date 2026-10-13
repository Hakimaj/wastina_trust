package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "occupation_classifications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"classification", "code"})
})
@Data
@NoArgsConstructor
public class OccupationClassification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String classification;

    @Column(nullable = false)
    private String code;

    @Column(name = "classification_version")
    private String classificationVersion;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "title_am")
    private String titleAm;

    @Column(name = "description_en")
    private String descriptionEn;

    @Column(name = "description_am")
    private String descriptionAm;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(name = "imported_at")
    private LocalDateTime importedAt = LocalDateTime.now();

    public OccupationClassification(String classification, String code, String titleEn) {
        this.classification = classification;
        this.code = code;
        this.titleEn = titleEn;
    }
}
