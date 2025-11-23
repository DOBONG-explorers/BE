package com.dobongzip.dobong.domain.mainpage.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class SeoulEventResponse {
    @JsonProperty("culturalEventInfo") private Body culturalEventInfo;

    @Getter
    public static class Body {
        @JsonProperty("row") private List<EventDto> row;
    }
}
