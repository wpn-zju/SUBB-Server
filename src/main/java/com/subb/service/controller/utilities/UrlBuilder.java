package com.subb.service.controller.utilities;

public class UrlBuilder {
    public static String buildThreadUrl(int threadId) {
        return String.format("https://www.peinanweng.com/bulletin-board/%d", threadId);
    }
}
