package com.kanban.kanbanapp.service.user;

import java.util.List;
import java.util.Map;

import com.kanban.kanbanapp.Data_Transfer_Object.UserCreateRequest;
import com.kanban.kanbanapp.Model.User;

public interface UserService {
    List<User> getAllUsers();
    User validateUser(String email, String password);
    User registerUser(UserCreateRequest request);
    void deleteUserBySecret(String secret);
    User updateUser(String secret, UserCreateRequest request);
    User patchUser(String secret, Map<String, Object> updates);
}
