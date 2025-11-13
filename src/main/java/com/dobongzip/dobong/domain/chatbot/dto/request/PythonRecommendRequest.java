package com.dobongzip.dobong.domain.chatbot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PythonRecommendRequest(
        String keyword,
        Integer k,
        @JsonProperty("user_location") UserLocation userLocation
) {
    public record UserLocation(double lat, double lon) {}
}

