package com.dapai.game;

public class RoomException extends RuntimeException {
    public enum Reason {
        NOT_FOUND, FORBIDDEN, CONFLICT
    }

    private final Reason reason;

    public RoomException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}