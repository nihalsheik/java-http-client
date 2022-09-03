package com.nihalsoft.java.http;

import java.io.UnsupportedEncodingException;

public class ApiResponse {

    private int status = -1;
    private byte[] content = null;

    public ApiResponse() {

    }

    public ApiResponse(byte[] content, int status) {
        this.content = content;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public byte[] getContent() {
        return content;
    }

    public int getContentLength() {
        return content.length;
    }

    public String getContentAsString() {
        try {
            return new String(content, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

}
