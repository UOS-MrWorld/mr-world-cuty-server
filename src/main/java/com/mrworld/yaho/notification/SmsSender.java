package com.mrworld.yaho.notification;

// 문자 발송 방식. 실제 발송(Solapi)과 로그만 남기는 방식을 설정으로 바꿔 끼운다.
public interface SmsSender {

    // to는 숫자만 있는 수신번호(예: 01012345678)다.
    void send(String to, String text);
}
