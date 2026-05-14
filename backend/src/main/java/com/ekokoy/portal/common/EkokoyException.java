package com.ekokoy.portal.common;

public class EkokoyException extends RuntimeException {

    private final String code;
    private final int status;

    public EkokoyException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
