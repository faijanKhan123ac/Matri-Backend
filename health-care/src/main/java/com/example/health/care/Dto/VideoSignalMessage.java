package com.example.health.care.Dto;

public class VideoSignalMessage {

    private String type;      // "offer", "answer", "ice-candidate", "join", "leave"
    private String roomId;    // appointment ID = room ID
    private String senderId;
    private String senderName;
    private Object payload;   // SDP offer/answer ya ICE candidate

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}