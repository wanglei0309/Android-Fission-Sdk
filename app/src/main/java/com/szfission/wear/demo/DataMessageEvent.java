package com.szfission.wear.demo;

public class DataMessageEvent {
    private int     messageType;
    private String messageContent;

    public DataMessageEvent(int messageType, String messageContent) {
        this.messageType = messageType;
        this.messageContent = messageContent;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
}
