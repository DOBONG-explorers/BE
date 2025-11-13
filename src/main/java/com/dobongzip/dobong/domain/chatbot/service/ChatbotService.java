package com.dobongzip.dobong.domain.chatbot.service;

import com.dobongzip.dobong.domain.chatbot.dto.request.PythonChatRequest;
import com.dobongzip.dobong.domain.chatbot.dto.request.PythonRecommendRequest;
import com.dobongzip.dobong.domain.chatbot.dto.response.PythonChatResponse;
import com.dobongzip.dobong.domain.chatbot.dto.response.PythonRecommendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final WebClient pythonWebClient;

    public PythonRecommendResponse getRecommendation(PythonRecommendRequest requestDto) {
        int kValue = requestDto.k() != null ? requestDto.k() : 5;

        var pythonRequest = new PythonRecommendRequest(
                requestDto.keyword(),
                kValue,
                requestDto.userLocation()
        );

        return pythonWebClient.post()
                .uri("/api/dobong/recommend")
                .bodyValue(pythonRequest)
                .retrieve()
                .bodyToMono(PythonRecommendResponse.class)
                .block();
    }

    // 추가: 대화형 프록시
    public PythonChatResponse chat(PythonChatRequest request) {
        int kValue = request.k() != null ? request.k() : 5; // k의 기본값 5

        var fixed = new PythonChatRequest(
                request.text(),  // 사용자 입력
                kValue           // 한 번에 받을 개수
        );

        // Python에서 응답을 받기
        PythonChatResponse response = pythonWebClient.post()
                .uri("/api/chatbot")
                .bodyValue(fixed)
                .retrieve()
                .bodyToMono(PythonChatResponse.class)
                .block();

        // 결과가 적으면 안내 메시지 추가
        if (response != null && response.results().size() < kValue) {
            String message = response.message() + "\n원하시는 장소가 없다면 '다시 추천'을 입력해주세요.\n자세히 보고 싶다면 '번호(1~5)'를 입력해주세요.";
            response = new PythonChatResponse(
                    response.status(),
                    response.parsed(),
                    response.k(),
                    response.offset(),
                    response.results(),
                    message // 수정된 메시지 포함
            );
        }
        return response;
    }
}
