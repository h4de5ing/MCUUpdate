package com.code19.mcuupdate.eventbus;

/**
 * Created by Gh0st on 2017/7/5.
 */

public class MessageEvent {
    public MessageEvent() {
    }

    public MessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String message;
}
