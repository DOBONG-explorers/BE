package com.dobongzip.dobong.domain.chatbot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReaskDto(
        @JsonProperty("alt_keywords")
        List<String> altKeywords
) {}
