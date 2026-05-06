package com.ecommerce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvclRlc3RpbmdQdXJwb3Nlc09ubHlOb3RGb3JQcm9kdWN0aW9uMTIz";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86400000L);
    }

    private UserDetails user(String email) {
        return User.withUsername(email).password("password").roles("USER").build();
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken(user("test@test.com"));

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractEmail_returnsEmailEmbeddedInToken() {
        String token = jwtUtil.generateToken(user("alice@test.com"));

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@test.com");
    }

    @Test
    void validateToken_returnsTrue_whenTokenMatchesUser() {
        UserDetails userDetails = user("bob@test.com");
        String token = jwtUtil.generateToken(userDetails);

        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void validateToken_returnsFalse_whenTokenBelongsToDifferentUser() {
        String token = jwtUtil.generateToken(user("user1@test.com"));
        UserDetails otherUser = user("user2@test.com");

        assertThat(jwtUtil.validateToken(token, otherUser)).isFalse();
    }
}