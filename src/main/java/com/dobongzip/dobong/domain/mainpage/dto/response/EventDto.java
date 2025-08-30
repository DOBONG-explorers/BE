package com.dobongzip.dobong.domain.mainpage.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
public class EventDto {
    @JsonProperty("DATE")      private String date;       // 단일 날짜
    @JsonProperty("STRTDATE")  private String startDate;  // 기간 시작
    @JsonProperty("END_DATE")  private String endDate;    // 기간 종료
    @JsonProperty("GUNAME")    private String guName;     // 자치구

    @JsonProperty("TITLE")     private String title;
    @JsonProperty("PLACE")     private String place;
    @JsonProperty("ORG_LINK")  private String orgLink;
    @JsonProperty("USE_TRGT")  private String useTarget;
    @JsonProperty("USE_FEE")   private String useFee;
    @JsonProperty("MAIN_IMG")  private String mainImg;

    @JsonProperty("CODENAME")  private String codeName;
    @JsonProperty("PROGRAM")   private String program;
    @JsonProperty("ETC_DESC")  private String etcDesc;
}

