package com.emmzy.model;

import com.emmzy.domain.UserRole;
import com.emmzy.domain.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "users")
public class User {

    public static final String DEFAULT_PROFILE_IMAGE =
            "/default-profile.png";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.ROLE_JOB_SEEKER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastLogin;

    private LocalDateTime suspendedAt;

    private LocalDateTime deletedAt;

    // =========================================================
    // DEFAULT PROFILE IMAGE
    // =========================================================
    //
    // This handles:
    // - New users
    // - Existing users created without a profile image
    // - JPA-created entities
    //
    // The user's own image is NEVER overwritten.
    //

    @PrePersist
    @PreUpdate
    private void ensureDefaultProfileImage() {

        if (profileImage == null ||
                profileImage.trim().isEmpty()) {

            profileImage = DEFAULT_PROFILE_IMAGE;
        }
    }
}