package com.insurmatch.repository;

import com.insurmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<User> findAllByOrderByIdDesc();

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :name, '%'))")
    java.util.List<User> searchByName(@org.springframework.data.repository.query.Param("name") String name);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :name, '%'))")
    java.util.List<User> searchByNameOrEmail(@org.springframework.data.repository.query.Param("name") String name);
}
