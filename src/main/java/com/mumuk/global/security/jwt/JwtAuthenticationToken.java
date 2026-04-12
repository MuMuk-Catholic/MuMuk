package com.mumuk.global.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 *  액세스 토큰 검증 및 인증 여부 판단
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Long user_id;
    private final String token;


    // 인증 후(인증 완료) 토큰용 생성자
    public JwtAuthenticationToken(Long user_id, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.token = null;
        this.user_id = user_id;
        super.setAuthenticated(true);
    }

    @Override
    public Object getPrincipal() {
        return user_id;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
