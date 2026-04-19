package com.mumuk.global.apiPayload.code;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseCode {

    // Common Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러입니다. 관리자에게 문의하세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "찾을 수 없는 요청입니다."),

    // User Error
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_401", "로그인 정보가 없습니다."),
    USER_NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "USER_401", "로그인 하지 않았습니다."),
    USER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "USER_403", "권한이 없습니다."),

    JWT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "JWT_500", "JWT 생성에 실패했습니다."),
    JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401", "유효하지 않은 JWT 토큰입니다."),
    JWT_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401_EX", "만료된 JWT 토큰입니다."),
    JWT_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWT_401", "JWT 토큰을 찾을 수 없습니다."),



    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "이미 사용 중인 아이디 입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "이미 사용 중인 이메일 입니다."),
    ID_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_ID_MISMATCH", "아이디가 일치하지 않습니다."),
    PHONE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "이미 가입된 휴대폰 번호 입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_PW_MISMATCH", "비밀번호가 일치하지 않습니다."),
    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_LOGIN_ID", "아이디 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_PW", "비밀번호는 8-15자이며 영문, 숫자, 특수문자를 포함해야 합니다."),
    INVALID_CURRENT_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_PW", "로그인 한 비밀번호랑 일치하지 않습니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_NICKNAME", "닉네임은 10자 이내만 가능합니다."),

    ////Ingredient Error
    INVALID_EXPIREDATE(HttpStatus.BAD_REQUEST, "INGREDIENT_400", "유통기한이 유효하지 않습니다."),
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "INGREDIENT_404", "해당 재료가 존재하지 않습니다."),
    USER_NOT_EQUAL(HttpStatus.BAD_REQUEST, "INGREDIENT_403", "해당 사용자의 재료가 아닙니다."),
    INVALID_D_DAY_SETTING(HttpStatus.BAD_REQUEST, "INGREDIENT_400", "올바른 알림설정이 아닙니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "INGREDIENT_400", "올바른 수량이 아닙니다."),
    ;


    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
