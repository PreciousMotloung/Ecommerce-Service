package com.ecommerce.service;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserIdByEmail_returnsUserId_whenUserExists() {
        User user = User.builder()
                .id(1L).username("alice").email("alice@test.com")
                .role(Role.USER).createdAt(LocalDateTime.now()).build();
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        Long result = userService.getUserIdByEmail("alice@test.com");

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void getUserIdByEmail_throwsUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserIdByEmail("nobody@test.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("nobody@test.com");
    }
}