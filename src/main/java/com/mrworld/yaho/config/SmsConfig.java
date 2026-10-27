package com.mrworld.yaho.config;

import com.mrworld.yaho.notification.LogSmsSender;
import com.mrworld.yaho.notification.SmsSender;
import com.mrworld.yaho.notification.SolapiSmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// SMS 환경변수(키·시크릿·발신번호)가 모두 있으면 Solapi로 실제 발송하고, 하나라도 없으면 로그만 남긴다.
@Slf4j
@Configuration
public class SmsConfig {

    @Bean
    public SmsSender smsSender(
            @Value("${sms.api.key:}") String apiKey,
            @Value("${sms.api.secret:}") String apiSecret,
            @Value("${sms.sender:}") String sender
    ) {
        if (apiKey.isBlank() || apiSecret.isBlank() || sender.isBlank()) {
            log.warn("SMS 설정이 없어 문자를 실제로 보내지 않고 로그만 남깁니다.");
            return new LogSmsSender();
        }
        return new SolapiSmsSender(apiKey, apiSecret, sender);
    }
}
