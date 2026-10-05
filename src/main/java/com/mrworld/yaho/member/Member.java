package com.mrworld.yaho.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Member {

    public enum Role {
        CUSTOMER,
        STAFF
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNum;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Member create(
            String loginId,
            String password,
            String name,
            String phoneNum,
            String address,
            Role role
    ) {
        return Member.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .phoneNum(phoneNum)
                .address(address)
                .role(role)
                .build();
    }

    public void updateProfile(String phoneNum, String address) {
        if (phoneNum != null) this.phoneNum = phoneNum;
        if (address != null) this.address = address;
    }
}
