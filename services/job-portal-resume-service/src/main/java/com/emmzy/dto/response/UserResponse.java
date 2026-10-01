package com.emmzy.dto.response;

import lombok.Data;

@Data
public class UserResponse {

    private long id;
    private String fullName;
    private String email;
    private String phone;
    private String profileImage;
    private String role;
    private String status;
}