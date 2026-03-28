package com.kanban.kanbanapp.service.auth;

import java.util.List;

import com.kanban.kanbanapp.Data_Transfer_Object.RegisterRequest;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;

public interface UserService {
    List<User> getAllUsers();
    
    User validateUser(String email, String password);

    User roleUser(String userId, Role newRole);
    
    User registerUser(RegisterRequest request);
    
    void deleteUserById(String userId);  
    
    User updateUser(String userId, User updatedUser);
}
