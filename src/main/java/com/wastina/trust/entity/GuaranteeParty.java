package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "guarantee_parties",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"guarantee_id", "user_id", "role"})
    }
)
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GuaranteeParty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guarantee_id", nullable = false)
    private Guarantee guarantee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuaranteePartyRole role;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
