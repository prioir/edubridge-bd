package com.edubridge.repository;

import com.edubridge.entity.Role;
import com.edubridge.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    java.util.List<User> findByRoleOrderByCreatedAtDesc(Role role);
}
