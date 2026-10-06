package com.emmzy.dto.response;

import com.emmzy.domain.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {

    private long id;
    private String fullName;
    private String email;
    private String phone;
    private String profileImage;

    private String role;

    private UserStatus status;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}