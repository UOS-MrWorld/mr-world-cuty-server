package com.mrworld.yaho.notification;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;

// Solapi(SMS Gateway)로 실제 문자를 보낸다. 발신번호는 Solapi에 등록된 번호여야 한다.
public class SolapiSmsSender implements SmsSender {

    private final DefaultMessageService messageService;
    private final String sender;

    public SolapiSmsSender(String apiKey, String apiSecret, String sender) {
        this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
        this.sender = sender;
    }

    @Override
    public void send(String to, String text) {
        Message message = new Message();
        message.setFrom(sender);
        message.setTo(to);
        message.setText(text);
        try {
            messageService.send(message, null);
        } catch (Exception e) {
            throw new IllegalStateException("문자 발송에 실패했습니다.", e);
        }
    }
}
