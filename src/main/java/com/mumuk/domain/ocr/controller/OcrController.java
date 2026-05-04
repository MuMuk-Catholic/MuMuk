package com.mumuk.domain.ocr.controller;


import com.mumuk.domain.ocr.service.OcrService;
import com.mumuk.domain.user.service.UserService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Tag(name = "건강 데이터 동의 및 OCR 관련")
@RequestMapping("/api/health")
public class OcrController {

    private final UserService userService;
    private final OcrService ocrService;

    public OcrController(UserService userService, OcrService ocrService) {
        this.userService = userService;
        this.ocrService = ocrService;
    }

    @Operation(summary = "건강 데이터 수집 동의")
    @PostMapping("/consent")
    public Response<String> agreeToHealthData(@AuthUser Long userId) {
        userService.agreeToHealthData(userId);
        return Response.ok(ResultCode.AGREE_HEALTH_DATA_OK, "건강 데이터 수집에 동의하였습니다.");
    }

    @Operation(summary = "OCR 이미지 텍스트 추출")
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<Map<String, String>> extractTextFromImage(@AuthUser Long userId, @RequestParam("image") MultipartFile image) {
        Map<String, String> result = ocrService.extractText(image);
        ocrService.saveOcrResult(userId, result);
        return Response.ok(ResultCode.OCR_HEALTH_TEXT_EXTRACT_OK, result);
    }
}
