package com.mumuk.domain.ocr.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface OcrService {
    Map<String, String> extractText(MultipartFile imageFile);
    void saveOcrResult(Long userId, Map<String, String> ocrResult);
}