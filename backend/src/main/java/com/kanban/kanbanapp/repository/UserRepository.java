package com.kanban.kanbanapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.User;

public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find a user by their email address.
     *
     * @param email the email address
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

}
