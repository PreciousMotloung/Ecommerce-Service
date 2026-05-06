package com.ecommerce.service;

import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}