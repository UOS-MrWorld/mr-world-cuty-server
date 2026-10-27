package com.mrworld.yaho.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 로그인한 회원의 ID(JWT subject)를 컨트롤러 파라미터로 받는다. 예: @LoginMemberId Long customerId
// JwtAuthenticationFilter가 회원 ID를 principal의 username으로 넣어 두므로, 그 값을 Long으로 바꿔 전달한다.
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "T(java.lang.Long).valueOf(username)")
public @interface LoginMemberId {
}
