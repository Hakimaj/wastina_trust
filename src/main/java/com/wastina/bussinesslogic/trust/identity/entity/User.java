package com.wastina.trust.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wastina.trust.identity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PUBLIC_ID")
    private String publicId;

    @Column(name = "USERNAME", unique = true, nullable = false)
    private String userName; // login email

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_STATUS")
    @Builder.Default
    private UserStatus userStatus = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_user",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
    )
    @JsonIgnoreProperties("users")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "IS_DELETED")
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "DELETED_ON")
    private Date deletedOn;

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
