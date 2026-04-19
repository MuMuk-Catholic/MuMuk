package com.mumuk.domain.user.service;

import com.mumuk.domain.user.converter.AuthConverter;
import com.mumuk.domain.user.converter.TokenResponseConverter;
import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.entity.LoginType;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.security.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import com.mumuk.global.util.SmsUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.mumuk.global.apiPayload.code.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenResponseConverter tokenResponseConverter;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, TokenResponseConverter tokenResponseConverter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenResponseConverter = tokenResponseConverter;
    }

    @Override
    @Transactional
    public void signUp(AuthRequest.SignUpReq request) {

        validateRequest(request);

        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new AuthException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AuthException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = AuthConverter.toUser(request.getName(), request.getNickname(), request.getPhoneNumber(), request.getLoginId(), encodedPassword);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickName(nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPhoneNumberAvailable(String phoneNumber) {
        return !userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional
    public TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response) {

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user, user.getPhoneNumber());
        String refreshToken = jwtTokenProvider.createRefreshToken(user, user.getPhoneNumber());

        user.updateRefreshToken(refreshToken);
        userRepository.save(user);          // 명시적 저장

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("X-Refresh-Token", refreshToken);

        return tokenResponseConverter.toResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, LoginType loginType) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
        User user = getUserFromToken(refreshToken, loginType);
        user.updateRefreshToken(null);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public TokenResponse reissue(String refreshToken, LoginType loginType) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        Claims claims = jwtTokenProvider.getClaimsFromToken(refreshToken);
        String subject = claims.getSubject(); // email or phoneNumber

        User user = switch (loginType) {
            case LOCAL -> userRepository.findByPhoneNumber(subject)
                    .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
            case KAKAO, NAVER -> userRepository.findByEmail(subject)
                    .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        };

        // 저장된 refreshToken이 없으면 재발급 불가
        String storedRefreshToken = user.getRefreshToken();
        if (storedRefreshToken == null || !jwtTokenProvider.validateToken(storedRefreshToken)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String newAccessToken;
        String newRefreshToken;

        if (loginType == LoginType.LOCAL) {
            newAccessToken = jwtTokenProvider.createAccessToken(user, subject);       // phoneNumber
            newRefreshToken = jwtTokenProvider.createRefreshToken(user, subject);     // phoneNumber
        }else{
            newAccessToken = jwtTokenProvider.createAccessTokenByEmail(user, subject, loginType);       // email
            newRefreshToken = jwtTokenProvider.createRefreshTokenByEmail(user, subject, loginType);     // email
        }

        // refreshToken 갱신
        user.updateRefreshToken(newRefreshToken);
        userRepository.save(user);

        return tokenResponseConverter.toResponse(newAccessToken, newRefreshToken);
    }


    @Override
    @Transactional(readOnly = true)
    public void checkCurrentPassword(AuthRequest.CheckCurrentPasswordReq request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        String currentPassword = request.getCurrentPassword();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_CURRENT_PASSWORD_FORMAT);
        }
    }

    @Override
    @Transactional
    public void reissueUserPassword(AuthRequest.RecoverPassWordReq request, Long userId) {
        String newPassword = request.getNewPassWord();
        String confirmPassword = request.getConfirmPassWord();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (!newPassword.equals(confirmPassword)) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        // 비밀번호 제약 조건 검사
        if (!isValidPassword(newPassword)) {
            throw new AuthException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.updateRefreshToken(null);

        userRepository.save(user);
    }

    private void validateRequest(AuthRequest.SignUpReq request) {

        if (!isValidNickname(request.getNickname())) {
            throw new AuthException(ErrorCode.INVALID_NICKNAME_FORMAT);
        }

        if(!isValidLoginId(request.getLoginId())) {
            throw new AuthException(ErrorCode.INVALID_LOGIN_ID_FORMAT);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (!isValidPassword(request.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    // token -> Claim 객체 -> Subject 을 이용한 사용자 정보 추출
    private User getUserFromToken(String token, LoginType loginType) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        String category = claims.get("category", String.class);
        if (!"access".equals(category) && !"refresh".equals(category)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String subject = claims.getSubject();   // 이메일 or 전화번호

        return switch (loginType) {
            case LOCAL -> userRepository.findByPhoneNumber(subject)
                    .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
            case KAKAO, NAVER -> userRepository.findByEmail(subject)
                    .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        };
    }

    private boolean isValidNickname(String nickname) {
        return nickname != null && nickname.length() <= 10;
    }

    private boolean isValidLoginId(String loginId) {
        String regex = "^(?=.*[A-Za-z])[A-Za-z\\d]{5,15}$";
        return Pattern.matches(regex, loginId);
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,15}$";
        return Pattern.matches(regex, password);
    }

    // id 다 안보여주고, * * 로 마스킹 처리되어 사용자에 반환
    private String maskUserId(String userId) {
        int visibleLength = userId.length() - 3;
        return userId.substring(0, visibleLength) + "***";
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        return IntStream.range(0, 8)
                .map(i -> random.nextInt(10))  // 0 ~ 9
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
}
