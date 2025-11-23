package com.dobongzip.dobong.domain.chatbot.dto.response;

import com.dobongzip.dobong.domain.map.dto.response.PlaceDto;

import java.util.List;

public record PythonChatResponse(
        String status,
        Object parsed,     // 파싱된 정보 (예: 카테고리, 키워드 등)
        Integer k,
        Integer offset,
        List<PlaceDto> results,
        String message    // 추가된 메시지 (안내 문구 포함)
) {}
