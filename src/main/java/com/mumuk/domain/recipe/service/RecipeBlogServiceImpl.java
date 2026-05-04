package com.mumuk.domain.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.recipe.dto.response.RecipeBlogResponse;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.client.NaverBlogClient;
import com.mumuk.global.util.TextUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RecipeBlogServiceImpl implements RecipeBlogService {

    private final NaverBlogClient naverBlogClient;
    private final ObjectMapper objectMapper;

    public RecipeBlogServiceImpl(NaverBlogClient naverBlogClient, ObjectMapper objectMapper) {
        this.naverBlogClient = naverBlogClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RecipeBlogResponse searchBlogByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String json = naverBlogClient.searchBlog(keyword);

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");

            List<RecipeBlogResponse.Blog> blogs = new ArrayList<>();

            // 파싱 결괏값 => DTO 변환
            for (JsonNode item : items) {
                String title = TextUtil.stripTags(item.path("title").asText());
                String rawDescription = Optional.ofNullable(item.path("description").asText()).orElse("");
                String description = TextUtil.smartTruncate(rawDescription, 70);
                String link = item.path("link").asText();

                // 이미지 정보는 현재 사용하지 않음 (레거시 코드 제거)
                blogs.add(new RecipeBlogResponse.Blog(
                        title,
                        description,
                        link,
                        null     // 이미지 URL은 현재 사용하지 않음
                ));
            }
            return new RecipeBlogResponse(blogs);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NAVER_API_PARSE_ERROR);
        }
    }
}
