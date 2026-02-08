package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "professions")
@Data
@NoArgsConstructor
public class Profession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    public Profession(String name) {
        this.name = name;
    }
}
