package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.entity.LoginType;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    void signUp(AuthRequest.SignUpReq request);
    boolean isLoginIdAvailable(String loginId);
    boolean isNicknameAvailable(String nickname);
    boolean isPhoneNumberAvailable(String phoneNumber);
    TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response);
    void logout(String refreshToken, LoginType loginType);
    void withdraw(Long userId);
    TokenResponse reissue(String refreshToken, LoginType loginType);
    void checkCurrentPassword(AuthRequest.CheckCurrentPasswordReq request, Long userId);
    void reissueUserPassword(AuthRequest.RecoverPassWordReq request, Long userId);
}
