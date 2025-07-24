package com.example.stripeintegration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.stripeintegration.entity.User;
import com.example.stripeintegration.repository.UserRepository;

@Service
public class Userservice {

    @Autowired
    private UserRepository userRepository;

    public User addUser(User user) {
        return userRepository.save(user);
    }
    
}
