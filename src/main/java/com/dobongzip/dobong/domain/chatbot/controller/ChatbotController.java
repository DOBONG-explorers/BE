package com.dobongzip.dobong.domain.chatbot.controller;

import com.dobongzip.dobong.domain.chatbot.dto.request.PythonChatRequest;
import com.dobongzip.dobong.domain.chatbot.dto.request.PythonRecommendRequest;
import com.dobongzip.dobong.domain.chatbot.dto.response.PythonChatResponse;
import com.dobongzip.dobong.domain.chatbot.dto.response.PythonRecommendResponse;
import com.dobongzip.dobong.domain.chatbot.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "챗봇 페이지")
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/recommend")
    public ResponseEntity<PythonRecommendResponse> getChatbotResponse(
            @RequestBody PythonRecommendRequest requestDto) {

        PythonRecommendResponse response = chatbotService.getRecommendation(requestDto);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "챗봇 응답을 반환합니다. text는 사용자 말, k는 반환 개수")
    @PostMapping("/chat")
    public ResponseEntity<PythonChatResponse> chat(
            @RequestBody PythonChatRequest request) {
        return ResponseEntity.ok(chatbotService.chat(request));
    }
}
