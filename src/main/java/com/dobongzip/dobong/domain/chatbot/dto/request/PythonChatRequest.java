package com.dobongzip.dobong.domain.chatbot.dto.request;

public record PythonChatRequest(
        String text,       // 사용자가 입력한 문장 (필수 추천)
        Integer k         // 한 번에 받을 개수, 기본 5
) {}
