package com.mumuk.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.global.util.FileResourceUtil;
import com.mumuk.global.util.ImagePreprocessingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class ClovaOcrClient {

    @Value("${naver.clova.ocr.invoke-url}")
    private String invokeUrl;

    @Value("${naver.clova.ocr.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate;
    private final ImagePreprocessingUtil imagePreprocessingUtil;
    private final ObjectMapper objectMapper;

    public ClovaOcrClient(RestTemplate restTemplate,
                          ImagePreprocessingUtil imagePreprocessingUtil) {
        this.restTemplate = restTemplate;
        this.imagePreprocessingUtil = imagePreprocessingUtil;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 기본 OCR 호출
     */
    public String callClovaOcr(MultipartFile imageFile) {
        return callClovaOcrWithOptimization(imageFile, false);
    }

    /**
     * 최적화된 OCR 호출 (이미지 전처리 포함)
     */
    public String callClovaOcrWithOptimization(MultipartFile imageFile, boolean enablePreprocessing) {
        try {
            log.info("🔄 Clova OCR 요청 시작: {}", imageFile.getOriginalFilename());

            MultipartFile processedImage = imageFile;

            // 이미지 전처리 수행 (옵션)
            if (enablePreprocessing && imagePreprocessingUtil.needsPreprocessing(imageFile)) {
                log.info("이미지 전처리 수행");
                byte[] preprocessedBytes = imagePreprocessingUtil.preprocessForOcr(imageFile);
                processedImage = createMultipartFileFromBytes(preprocessedBytes, imageFile.getOriginalFilename());
            }

            // OCR 요청 수행
            String result = performOcrRequest(processedImage);

            // 결과 품질 검증
            if (!isValidOcrResult(result) && !enablePreprocessing) {
                log.warn("⚠️ OCR 결과 품질 불량, 전처리 후 재시도");
                return callClovaOcrWithOptimization(imageFile, true);
            }

            log.info("✅ Clova OCR 응답 수신 완료");
            return result;

        } catch (IOException e) {
            log.error("❌ CLOVA OCR 요청 실패: {}", e.getMessage());
            throw new RuntimeException("CLOVA OCR 요청 실패", e);
        }
    }

    /**
     * 다중 템플릿을 사용한 OCR (더 높은 정확도)
     */
    public String callClovaOcrWithMultipleTemplates(MultipartFile imageFile) {
        try {
            log.info("다중 템플릿 OCR 시작");

            // 인바디 관련 여러 템플릿 시도
            int[] templateIds = {38491, 0}; // 38491: 기본 템플릿, 0: 범용 템플릿

            String bestResult = null;
            int maxFieldCount = 0;

            for (int templateId : templateIds) {
                log.info("템플릿 {} 시도", templateId);
                String result = callClovaOcrWithTemplate(imageFile, templateId);
                int fieldCount = countExtractedFields(result);

                log.info("템플릿 {} 결과: {} 필드 추출", templateId, fieldCount);

                if (fieldCount > maxFieldCount) {
                    maxFieldCount = fieldCount;
                    bestResult = result;
                }
            }

            log.info("최적 결과 선택: {} 필드", maxFieldCount);
            return bestResult;

        } catch (Exception e) {
            log.warn("다중 템플릿 실패, 기본 템플릿 사용");
            return callClovaOcr(imageFile);
        }
    }

    /**
     * 재시도 로직이 포함된 OCR 호출
     */
    public String callClovaOcrWithRetry(MultipartFile imageFile, int maxRetries) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("OCR 시도 {}/{}", attempt, maxRetries);

                // 첫 번째 시도는 기본, 이후는 전처리 적용
                boolean enablePreprocessing = attempt > 1;
                String result = callClovaOcrWithOptimization(imageFile, enablePreprocessing);

                // 결과 품질 검증
                if (isValidOcrResult(result)) {
                    log.info("OCR 성공 (시도 {})", attempt);
                    return result;
                } else {
                    log.warn("OCR 결과 품질 불량 (시도 {})", attempt);
                    if (attempt < maxRetries) {
                        Thread.sleep(1000 * attempt); // 점진적 대기
                    }
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("OCR 실패 (시도 {}): {}", attempt, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000 * attempt); // 점진적 대기
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new RuntimeException("OCR 최대 재시도 횟수 초과", lastException);
    }

    /**
     * 실제 OCR 요청 수행
     */
    private String performOcrRequest(MultipartFile imageFile) throws IOException {
        HttpHeaders headers = createOptimizedHeaders();
        String messageJson = buildOptimizedClovaRequestMessage(imageFile);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", FileResourceUtil.toResource(imageFile));
        body.add("message", messageJson);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(invokeUrl, requestEntity, String.class);

        return response.getBody();
    }

    /**
     * 특정 템플릿으로 OCR 요청
     */
    private String callClovaOcrWithTemplate(MultipartFile imageFile, int templateId) throws IOException {
        HttpHeaders headers = createOptimizedHeaders();
        String messageJson = buildTemplateSpecificMessage(imageFile, templateId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", FileResourceUtil.toResource(imageFile));
        body.add("message", messageJson);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(invokeUrl, requestEntity, String.class);

        return response.getBody();
    }

    /**
     * 최적화된 헤더 생성
     */
    private HttpHeaders createOptimizedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-OCR-SECRET", secretKey);
        headers.set("Content-Type", "multipart/form-data");
        headers.set("Connection", "keep-alive");
        headers.set("Accept", "application/json");
        return headers;
    }

    /**
     * 최적화된 요청 메시지 생성
     */
    private String buildOptimizedClovaRequestMessage(MultipartFile imageFile) {
        return """
        {
            "version": "V2",
            "requestId": "%s",
            "timestamp": %d,
            "images": [
                {
                    "format": "%s",
                    "name": "%s",
                    "templateIds": [38491]
                }
            ],
            "lang": "ko",
            "resultType": "string",
            "enableTableDetection": false
        }
        """.formatted(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                getFileExtension(imageFile.getOriginalFilename()),
                imageFile.getOriginalFilename()
        );
    }

    /**
     * 템플릿별 요청 메시지 생성
     */
    private String buildTemplateSpecificMessage(MultipartFile imageFile, int templateId) {
        return """
        {
            "version": "V2",
            "requestId": "%s",
            "timestamp": %d,
            "images": [
                {
                    "format": "%s",
                    "name": "%s",
                    "templateIds": [%d]
                }
            ],
            "lang": "ko",
            "resultType": "string"
        }
        """.formatted(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                getFileExtension(imageFile.getOriginalFilename()),
                imageFile.getOriginalFilename(),
                templateId
        );
    }

    /**
     * OCR 결과에서 추출된 필드 개수 계산
     */
    private int countExtractedFields(String ocrResult) {
        try {
            JsonNode root = objectMapper.readTree(ocrResult);
            JsonNode fields = root.path("images").get(0).path("fields");

            if (fields.size() > 0) {
                String text = fields.get(0).path("inferText").asText();
                // 인바디 관련 키워드 개수 계산
                String[] keywords = {"체중", "체지방", "골격근", "BMI", "단백질", "체수분", "무기질"};
                int count = 0;
                for (String keyword : keywords) {
                    if (text.contains(keyword)) count++;
                }
                return count;
            }
            return 0;
        } catch (Exception e) {
            log.warn("필드 개수 계산 실패: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * OCR 결과 품질 검증
     */
    private boolean isValidOcrResult(String result) {
        try {
            JsonNode root = objectMapper.readTree(result);
            JsonNode fields = root.path("images").get(0).path("fields");

            if (fields.size() > 0) {
                String text = fields.get(0).path("inferText").asText();
                // 최소한의 텍스트가 있고, 숫자가 포함되어 있는지 확인
                boolean hasMinLength = text.length() > 10;
                boolean hasNumbers = text.matches(".*\\d+.*");
                boolean hasInBodyKeywords = text.contains("체중") || text.contains("BMI") ||
                        text.contains("Weight") || text.contains("체지방");

                return hasMinLength && hasNumbers && hasInBodyKeywords;
            }
            return false;
        } catch (Exception e) {
            log.warn("OCR 결과 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "jpg";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex != -1) ? filename.substring(dotIndex + 1).toLowerCase() : "jpg";
    }

    /**
     * byte[]를 MultipartFile로 변환
     */
    private MultipartFile createMultipartFileFromBytes(byte[] bytes, String originalFilename) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return originalFilename;
            }

            @Override
            public String getContentType() {
                return "image/jpeg";
            }

            @Override
            public boolean isEmpty() {
                return bytes == null || bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return bytes;
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new java.io.ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    fos.write(bytes);
                }
            }
        };
    }
}
