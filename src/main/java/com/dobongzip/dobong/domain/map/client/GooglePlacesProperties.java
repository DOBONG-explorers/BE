package com.dobongzip.dobong.domain.map.client;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "google.places")
public class GooglePlacesProperties {
    private String apiKey;
    private String language;
    private String region;
    private int radiusM;

    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setLanguage(String language) { this.language = language; }
    public void setRegion(String region) { this.region = region; }
    public void setRadiusM(int radiusM) { this.radiusM = radiusM; }
}
