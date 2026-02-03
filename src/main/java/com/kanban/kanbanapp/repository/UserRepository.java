package com.kanban.kanbanapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.User;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndPassword(String email, String password);

    Optional<User> findBySecret(String secret);
}
