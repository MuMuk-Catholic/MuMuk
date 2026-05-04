package com.mumuk.global.client;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final WebClient webClient;
    private final String model;
    private final String accurateModel;

    public GeminiClient(WebClient webClient, @Value("${gemini.api.model}") String model,
                        @Value("${gemini.api.model_accurate:${gemini.api.model}}") String accurateModel) {
        this.webClient = webClient;
        this.model = model;
        this.accurateModel = accurateModel;
    }

    public Mono<String> chat(String prompt) {
        return chatWithModel(prompt, this.model);
    }

    public Mono<String> chatAccurate(String prompt) {
        return chatWithModel(prompt, this.accurateModel);
    }

    public Mono<String> chatWithModel(String prompt, String modelName) {
        Map<String, Object> body = createRequestBody(prompt);

        return webClient.post()
                .uri("/v1beta/models/" + modelName + ":generateContent")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::extractContent);
    }

    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> body = new HashMap<>();

        // Gemini API 요청 구조
        Map<String, Object> contents = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);

        contents.put("parts", parts);

        List<Map<String, Object>> contentsList = new ArrayList<>();
        contentsList.add(contents);

        body.put("contents", contentsList);

        // 안전 설정 추가
        Map<String, Object> safetySettings = new HashMap<>();
        safetySettings.put("category", "HARM_CATEGORY_HARASSMENT");
        safetySettings.put("threshold", "BLOCK_NONE");

        List<Map<String, Object>> safetySettingsList = new ArrayList<>();
        safetySettingsList.add(safetySettings);

        body.put("safetySettings", safetySettingsList);

        return body;
    }

    private String extractContent(Map<String, Object> response) {
        if (response == null || !response.containsKey("candidates")) {
            throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_NO_CHOICES);
        }

        Map<String, Object> candidate = candidates.get(0);
        if (candidate == null || !candidate.containsKey("content")) {
            throw new BusinessException(ErrorCode.OPENAI_MISSING_CONTENT);
        }

        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
        if (content == null || !content.containsKey("parts")) {
            throw new BusinessException(ErrorCode.OPENAI_MISSING_CONTENT);
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts.isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_MISSING_CONTENT);
        }

        Map<String, Object> firstPart = parts.get(0);
        if (firstPart == null || !firstPart.containsKey("text")) {
            throw new BusinessException(ErrorCode.OPENAI_MISSING_CONTENT);
        }

        String text = (String) firstPart.get("text");

        // Gemini 응답에서 JSON 부분만 추출 (코드블록 제거)
        return extractJsonFromGeminiResponse(text);
    }

    /**
     * Gemini 응답에서 JSON 부분만 추출
     * ```json ... ``` 형태의 코드블록을 제거하고 JSON만 반환
     */
    private String extractJsonFromGeminiResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
        }

        String trimmedResponse = response.trim();

        // ```json으로 시작하는 경우
        if (trimmedResponse.startsWith("```json")) {
            int startIndex = trimmedResponse.indexOf("```json") + 7;
            int endIndex = trimmedResponse.lastIndexOf("```");
            if (endIndex > startIndex) {
                return trimmedResponse.substring(startIndex, endIndex).trim();
            }
        }

        // ```으로 시작하는 경우 (json 태그 없음)
        if (trimmedResponse.startsWith("```")) {
            int startIndex = trimmedResponse.indexOf("```") + 3;
            int endIndex = trimmedResponse.lastIndexOf("```");
            if (endIndex > startIndex) {
                return trimmedResponse.substring(startIndex, endIndex).trim();
            }
        }

        // 코드블록이 없는 경우 그대로 반환
        return trimmedResponse;
    }

    /**
     * Gemini API 모델 목록을 확인하는 메서드
     */
    public Mono<Map<String, Object>> getModels() {
        return webClient.get()
                .uri("/v1beta/models")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(ex -> new BusinessException(ErrorCode.OPENAI_API_ERROR));
    }
}
