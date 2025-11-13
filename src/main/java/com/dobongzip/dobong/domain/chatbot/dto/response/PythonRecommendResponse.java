package com.dobongzip.dobong.domain.chatbot.dto.response;


import com.dobongzip.dobong.domain.map.dto.response.PlaceDto;

import java.util.List;

public record PythonRecommendResponse(
        String status,
        Integer count,
        List<PlaceDto> results,
        String explain,
        ReaskDto reask,
        String message,
        String detail
) {}