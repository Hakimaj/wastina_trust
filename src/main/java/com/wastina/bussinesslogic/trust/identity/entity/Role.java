package com.wastina.trust.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "roles")
@Builder
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", unique = true)
    private String name;

    @Column(name = "PUBLIC_ID", unique = true)
    private String publicId;

    @Column(name = "DESCRIPTION")
    private String description;

    @ManyToOne
    @JoinColumn(name = "PARENT_ROLE_ID")
    private Role parentRole;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "ROLE_ID"),
            inverseJoinColumns = @JoinColumn(name = "PERMISSION_ID"))
    private Set<Permission> permissions;

    @JsonIgnore
    public List<SimpleGrantedAuthority> getAuthorities() {
        Set<Permission> allPermissions = getAllPermissions();
        List<SimpleGrantedAuthority> authorities = allPermissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name));
        return authorities;
    }

    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>(this.permissions == null ? Set.of() : this.permissions);
        if (this.parentRole != null) {
            allPermissions.addAll(this.parentRole.getAllPermissions());
        }
        return allPermissions;
    }

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
