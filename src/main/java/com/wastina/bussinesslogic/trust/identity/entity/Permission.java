package com.wastina.trust.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "PUBLIC_ID", unique = true)
    private String publicId;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "CREATE_USER_ID")
    private Long createUserId;

    @Column(name = "CREATE_TIME")
    private LocalDateTime createTime;

    @Column(name = "UPDATE_USER_ID")
    private Long updateUserId;

    @Column(name = "UPDATE_TIME")
    private LocalDateTime updateTime;

    @Column(name = "STATUS")
    private Integer status; // e.g. 1 = active, 0 = disqualified

    @Lob
    @Column(name = "REMARKS")
    private String remarks;
}
