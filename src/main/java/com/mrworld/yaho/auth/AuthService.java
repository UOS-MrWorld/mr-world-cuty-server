package com.mrworld.yaho.auth;

import com.mrworld.yaho.auth.dto.*;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.exception.AuthenticationFailedException;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.member.Staff;
import com.mrworld.yaho.member.StaffRepository;
import com.mrworld.yaho.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private static final String LOGIN_ID_DUPLICATED = "이미 사용 중인 로그인 아이디입니다.";
    private static final String INVALID_CREDENTIALS = "아이디 또는 비밀번호가 올바르지 않습니다.";
    private static final String INVALID_REFRESH_TOKEN = "유효하지 않은 refresh token입니다.";

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("비밀번호가 일치하지 않습니다.");
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

        // 역할별 정보는 member의 id를 그대로 식별자로 쓰며, 회원 저장과 같은 트랜잭션에서 함께 저장한다.
        if(savedMember.getRole() == Role.CUSTOMER) {
            customerRepository.save(Customer.create(savedMember));
        } else if(savedMember.getRole() == Role.STAFF) {
            staffRepository.save(Staff.create(savedMember));
        }

        return SignupResponse.from(savedMember);
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
        String accessToken = jwtTokenProvider.createToken(member.getMemberId(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());

        RefreshToken savedRefreshToken = refreshTokenRepository.findByMemberId(member.getMemberId())
                .orElse(null);

        if(savedRefreshToken == null) {
            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .memberId(member.getMemberId())
                            .token(refreshToken)
                            .expiresAt(jwtTokenProvider.getExpiresAt(refreshToken))
                    .build()
            );
        } else {
            savedRefreshToken.updateToken(
                    refreshToken,
                    jwtTokenProvider.getExpiresAt(refreshToken)
            );
        }

        return LoginResponse.of(member, accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        RefreshToken savedToken = findValidRefreshToken(refreshToken);

        Member member = memberRepository.findById(savedToken.getMemberId())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH_TOKEN));

        return TokenResponse.of(jwtTokenProvider.createToken(member.getMemberId(), member.getRole()));
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
        if(!jwtTokenProvider.validateToken(refreshToken)
                || !"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
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
