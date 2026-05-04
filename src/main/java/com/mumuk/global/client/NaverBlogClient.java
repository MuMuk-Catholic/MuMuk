package com.mumuk.global.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;


@Component
public class NaverBlogClient {

    private final RestTemplate restTemplate;

    public NaverBlogClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${naver.blog.client-id}")
    private String clientId;

    @Value("${naver.blog.secret-key}")
    private String clientSecret;

    @Value("${naver.blog.base-url}")
    private String BASE_URL;


    public String searchBlog(String query) {
        String url = BASE_URL + "?query=" + query;

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
}
