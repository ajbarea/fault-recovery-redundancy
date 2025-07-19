package com.swen755.fault_recovery_redundancy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swen755.fault_recovery_redundancy.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByStreamKey(String streamKey);

    Optional<User> findByEmail(String email);
}
