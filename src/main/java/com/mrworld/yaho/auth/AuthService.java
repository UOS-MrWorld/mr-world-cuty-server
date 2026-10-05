package com.mrworld.yaho.auth;

import com.mrworld.yaho.auth.dto.*;
import com.mrworld.yaho.common.exception.AuthenticationFailedException;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.security.JwtProvider;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {
    private static final String LOGIN_ID_DUPLICATED = "이미 사용 중인 로그인 아이디입니다.";
    private static final String INVALID_CREDENTIALS = "아이디 또는 비밀번호가 올바르지 않습니다.";
    private static final String INVALID_REFRESH_TOKEN = "유효하지 않은 refresh token입니다.";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignUpResponse signup(SignUpRequest request) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if(memberRepository.existsByLoginId(request.getLoginId())) {
            throw new ConflictException(LOGIN_ID_DUPLICATED);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Member member = Member.create(
                request.getLoginId(),
                encodedPassword,
                request.getName(),
                request.getPhoneNum(),
                request.getAddress(),
                request.getRole()
        );

        Member savedMember;
        try {
            savedMember = memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(LOGIN_ID_DUPLICATED);
        }

        return new SignUpResponse(
                savedMember.getId(),
                savedMember.getName(),
                savedMember.getLoginId(),
                savedMember.getPhoneNum(),
                savedMember.getAddress(),
                savedMember.getRole()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String loginId = request.getLoginId();

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));

        if(!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        return issueLoginToken(member);
    }

    private LoginResponse issueLoginToken(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member);
        String refreshToken = jwtProvider.generateRefreshToken(member);

        RefreshToken savedRefreshToken = refreshTokenRepository.findByMemberId(member.getId())
                .orElse(null);

        if(savedRefreshToken == null) {
            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .memberId(member.getId())
                            .token(refreshToken)
                            .expiresAt(jwtProvider.getExpiresAt(refreshToken))
                    .build()
            );
        } else {
            savedRefreshToken.updateToken(
                    refreshToken,
                    jwtProvider.getExpiresAt(refreshToken)
            );
        }

        return new LoginResponse(
                member.getId(),
                member.getName(),
                member.getRole(),
                accessToken,
                refreshToken,
                "Bearer"
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        RefreshToken savedToken = findValidRefreshToken(refreshToken);

        Member member = memberRepository.findById(savedToken.getMemberId())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN));

        return new TokenResponse(jwtProvider.generateAccessToken(member));
    }

    @Transactional
    public void logout(Long memberId, String refreshToken) {
        RefreshToken savedToken = findValidRefreshToken(refreshToken);

        if(!savedToken.getMemberId().equals(memberId)) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        refreshTokenRepository.delete(savedToken);
    }

    private RefreshToken findValidRefreshToken(String refreshToken) {
        if(!jwtProvider.validateToken(refreshToken)
                || !"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN));

        if(savedToken.isExpired()) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        return savedToken;
    }
}
