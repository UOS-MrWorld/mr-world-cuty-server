package com.mrworld.yaho.security;

import com.mrworld.yaho.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // 테스트 실행 시 임의로 만든 키만 사용한다.
        String secret = Base64.getEncoder().encodeToString(
                (UUID.randomUUID().toString() + UUID.randomUUID()).getBytes());
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessExpirationTime", 60_000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshAccessExpirationTime", 120_000L);
        ReflectionTestUtils.invokeMethod(jwtTokenProvider, "init");
    }

    @Test
    void 접근_토큰에는_회원_ID와_역할이_담긴다() {
        String token = jwtTokenProvider.createToken(7L, Role.STAFF);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("access");

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        assertThat(authentication.getName()).isEqualTo("7");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_STAFF");
    }

    @Test
    void 재발급용_토큰은_refresh_타입이고_만료_시각을_가진다() {
        String token = jwtTokenProvider.createRefreshToken(7L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("refresh");
        assertThat(jwtTokenProvider.getExpiresAt(token)).isAfter(java.time.LocalDateTime.now());
    }
}
