package com.kopi.kopi.service;

import com.kopi.kopi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnverifiedUserDeletionService {
    private final UserRepository userRepository;

    public UnverifiedUserDeletionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUserById(Integer userId) {
        userRepository.deleteById(userId);
        userRepository.flush();
    }
}
