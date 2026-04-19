package com.mumuk.global.security.jwt;

import com.mumuk.domain.user.entity.LoginType;
import com.mumuk.domain.user.entity.User;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

/**
 *  Jwt 생성, 검증, 파싱 Provider
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    private JwtParser jwtParser;
    private Key secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build();
    }

    public String createAccessToken(User user, String phoneNumber) {
        try {
            return generateToken(user, phoneNumber, accessTokenValidity,"access");
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    public String createRefreshToken(User user, String phoneNumber) {
        try {
            return generateToken(user, phoneNumber, refreshTokenValidity, "refresh");
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    private String generateToken(User user, String phoneNumber, long validity, String category) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setSubject(phoneNumber)
                .claim("user_id", user.getId())
                .claim("category", category)      // "access" 또는 "refresh"
                .claim("loginType", "LOCAL")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String createAccessTokenByEmail(User user, String email, LoginType loginType) {
        try {
            return generateTokenByEmail(user, email, accessTokenValidity, "access", loginType);
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    public String createRefreshTokenByEmail(User user, String email, LoginType loginType) {
        try {
            return generateTokenByEmail(user, email, refreshTokenValidity, "refresh", loginType);
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    private String generateTokenByEmail(User user, String email, long validity, String category, LoginType loginType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setSubject(email)
                .claim("user_id", user.getId())
                .claim("category", category)
                .claim("loginType", loginType.name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new AuthException(ErrorCode.JWT_EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
    }

    // JWT 의 payload(Claims 객체) 추출
    public Claims parseClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public Claims getClaimsFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
    }

    // JWT 발급 시 필요한 시크릿 키를 Key 객체로 변환 (서명 검증)
    private Key getSigningKey() {
        return this.secretKey;
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthException(ErrorCode.JWT_TOKEN_NOT_FOUND);
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        return jwtParser.parseClaimsJws(token).getBody().get("user_id", Long.class);
    }
}
