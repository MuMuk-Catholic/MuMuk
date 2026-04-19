package com.mumuk.domain.user.converter;


import com.mumuk.domain.user.dto.response.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class TokenResponseConverter {

    public TokenResponse toResponse(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}