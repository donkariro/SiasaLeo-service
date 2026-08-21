package com.arriyiaconsulting.siasaleo.service.infrastructure.sms;

/**
 * Sends a text message to a phone number in E.164 form. The only
 * implementation today writes to the log; an SMS gateway implementation
 * (e.g. Africa's Talking) will replace it without touching callers.
 */
public interface SmsSender {

    void send(String phoneE164, String text);
}
