package com.dobongzip.dobong.domain.map.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikipediaClient {

    private final RestTemplate restTemplate;

    // 반경을 넉넉하게 (m)
    private static final int GEOSEARCH_RADIUS_M = 3000;

    /**
     * 이름으로 요약 시도 → 이름 검색(search) → 좌표 기반 geosearch 순서.
     * ko 우선, 실패 시 en 시도.
     */
    public Optional<String> getSummary(String nameKo, Double lat, Double lng) {
        try {
            // 1) 제목 직행
            if (nonEmpty(nameKo)) {
                // ko
                String s = fetchSummary("ko", nameKo);
                if (nonEmpty(s)) return Optional.of(s);
                // en
                s = fetchSummary("en", nameKo);
                if (nonEmpty(s)) return Optional.of(s);
            }

            // 2) 제목 검색 (search)
            if (nonEmpty(nameKo)) {
                // ko
                String t = searchTitle("ko", nameKo);
                if (nonEmpty(t)) {
                    String s = fetchSummary("ko", t);
                    if (nonEmpty(s)) return Optional.of(s);
                }
                // en
                t = searchTitle("en", nameKo);
                if (nonEmpty(t)) {
                    String s = fetchSummary("en", t);
                    if (nonEmpty(s)) return Optional.of(s);
                }
            }

            // 3) 좌표 기반 geosearch
            if (lat != null && lng != null) {
                // ko
                String t = geosearchTitle("ko", lat, lng);
                if (nonEmpty(t)) {
                    String s = fetchSummary("ko", t);
                    if (nonEmpty(s)) return Optional.of(s);
                }
                // en
                t = geosearchTitle("en", lat, lng);
                if (nonEmpty(t)) {
                    String s = fetchSummary("en", t);
                    if (nonEmpty(s)) return Optional.of(s);
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("[Wikipedia] failed: {}", e.toString());
            return Optional.empty();
        }
    }

    /** Wikimedia REST summary */
    private String fetchSummary(String lang, String title) {
        try {
            String enc = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
            URI u = UriComponentsBuilder
                    .fromHttpUrl("https://" + lang + ".wikipedia.org/api/rest_v1/page/summary/" + enc)
                    .build(true).toUri();

            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "DobongZip/1.0 (contact: dev@example.com)");
            ResponseEntity<Map> res = restTemplate.exchange(u, HttpMethod.GET, new HttpEntity<>(h), Map.class);
            Object extract = res.getBody() != null ? res.getBody().get("extract") : null;
            return (extract instanceof String s) ? s.trim() : null;
        } catch (HttpStatusCodeException e) {
            return null; // 404 등 무시
        } catch (Exception e) {
            return null;
        }
    }

    /** MediaWiki search API (제목 검색) */
    private String searchTitle(String lang, String query) {
        try {
            URI u = UriComponentsBuilder
                    .fromHttpUrl("https://" + lang + ".wikipedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("list", "search")
                    .queryParam("srsearch", query)
                    .queryParam("srlimit", 1)
                    .queryParam("format", "json")
                    .build(true).toUri();

            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "DobongZip/1.0 (contact: dev@example.com)");
            ResponseEntity<Map> res = restTemplate.exchange(u, HttpMethod.GET, new HttpEntity<>(h), Map.class);

            Map body = res.getBody();
            if (body == null) return null;
            Map q = (Map) body.get("query");
            if (q == null) return null;
            List s = (List) q.get("search");
            if (s == null || s.isEmpty()) return null;

            Map first = (Map) s.get(0);
            Object title = first.get("title");
            return title != null ? String.valueOf(title) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** MediaWiki geosearch (좌표 주변에서 가장 가까운 문서 제목) */
    private String geosearchTitle(String lang, double lat, double lng) {
        try {
            URI u = UriComponentsBuilder
                    .fromHttpUrl("https://" + lang + ".wikipedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("list", "geosearch")
                    .queryParam("gscoord", lat + "|" + lng)
                    .queryParam("gsradius", GEOSEARCH_RADIUS_M)
                    .queryParam("gslimit", 1)
                    .queryParam("format", "json")
                    .build(true).toUri();

            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "DobongZip/1.0 (contact: dev@example.com)");
            ResponseEntity<Map> res = restTemplate.exchange(u, HttpMethod.GET, new HttpEntity<>(h), Map.class);

            Map body = res.getBody();
            if (body == null) return null;
            Map query = (Map) body.get("query");
            if (query == null) return null;
            List gs = (List) query.get("geosearch");
            if (gs == null || gs.isEmpty()) return null;

            Map first = (Map) gs.get(0);
            Object title = first.get("title");
            return title != null ? String.valueOf(title) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean nonEmpty(String s) { return s != null && !s.isBlank(); }
}
