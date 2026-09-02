package com.emmzy.service;

import com.emmzy.dto.response.UserResponse;
import com.emmzy.model.User;
import com.emmzy.payload.UpdateUserRequest;

import java.util.List;

public interface UserService {

    User getUserByEmail(String email) throws Exception;

    User getUserById(Long id) throws Exception;

    List<User> getAllUsers();

    UserResponse updateProfile(String email, UpdateUserRequest req) throws Exception;

    //Admin action
    UserResponse suspendUser(Long id) throws Exception;
    UserResponse activateUser(Long id) throws Exception;
    UserResponse deleteUser(Long id) throws Exception;
}
