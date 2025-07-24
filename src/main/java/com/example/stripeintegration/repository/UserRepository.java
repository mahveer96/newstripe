package com.example.stripeintegration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stripeintegration.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
}
