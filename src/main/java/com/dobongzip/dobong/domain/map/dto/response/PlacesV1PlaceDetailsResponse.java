package com.dobongzip.dobong.domain.map.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacesV1PlaceDetailsResponse {
    private String id;
    private DisplayName displayName;
    private String internationalPhoneNumber;
    private String nationalPhoneNumber;
    private EditorialSummary editorialSummary;
    private GenerativeSummary generativeSummary;

    private String formattedAddress;
    private OpeningHours currentOpeningHours;
    private Integer priceLevel;
    private Double rating;
    private Integer userRatingCount;
    private List<Photo> photos;

    // 보조 요약 필드들
    private LocalizedText areaSummary;
    private AddressDescriptor addressDescriptor;

    // 위키 geosearch용 좌표
    private Location location;

    // reviews
    private List<Review> reviews;

    @Getter public static class DisplayName { private String text; }

    @Getter public static class EditorialSummary { private String overview; }

    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenerativeSummary {
        private LocalizedText overview;
        private LocalizedText description;
    }

    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours { private List<String> weekdayDescriptions; }

    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo { private String name; }

    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocalizedText {
        private String text;
        private String languageCode;
    }

    // AddressDescriptor 구조
    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressDescriptor {
        private List<Landmark> landmarks;
        private List<Area> areas;
    }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Landmark {
        private String placeId;
        private LocalizedText displayName;
    }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Area {
        private String placeId;
        private LocalizedText displayName;
    }

    // 위치
    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double latitude;
        private double longitude;
    }

    // 리뷰
    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Review {
        private AuthorAttribution authorAttribution;
        private Double rating;
        private LocalizedText text;
        private String relativePublishTimeDescription;

        @Getter @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AuthorAttribution {
            private String displayName;
            private String uri;
            private String photoUri;
        }
    }
}
