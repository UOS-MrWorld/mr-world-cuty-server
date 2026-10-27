package com.mrworld.yaho.notification;

import lombok.extern.slf4j.Slf4j;

// Solapi 키가 없을 때(테스트·CI·키 없는 로컬) 실제로 보내지 않고 로그만 남긴다.
@Slf4j
public class LogSmsSender implements SmsSender {

    @Override
    public void send(String to, String text) {
        log.info("[SMS 미발송] to={}, text={}", to, text);
    }
}
