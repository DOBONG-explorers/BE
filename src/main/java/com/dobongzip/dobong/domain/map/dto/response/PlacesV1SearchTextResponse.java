package com.dobongzip.dobong.domain.map.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacesV1SearchTextResponse {
    private List<Place> places;

    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        private String id;
        private DisplayName displayName;
        private String formattedAddress;
        private String googleMapsUri;
        private Location location;
        private OpeningHours currentOpeningHours;
        private EditorialSummary editorialSummary;
        private GenerativeSummary generativeSummary;

        private LocalizedText areaSummary;
        private AddressDescriptor addressDescriptor;

        private Integer priceLevel;
        private List<Photo> photos;
        private Double rating;
        private Integer userRatingCount;
    }

    // AddressDescriptor 구조 (검색 응답용)
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

    @Getter @JsonIgnoreProperties(ignoreUnknown = true) public static class DisplayName { private String text; }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true) public static class Location { private double latitude; private double longitude; }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true) public static class OpeningHours { private List<String> weekdayDescriptions; }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true) public static class EditorialSummary { private String overview; }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true) public static class Photo { private String name; }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true) public static class GenerativeSummary { private LocalizedText overview; private LocalizedText description; }
    @Getter @JsonIgnoreProperties(ignoreUnknown = true) public static class LocalizedText { private String text; private String languageCode; }
}
