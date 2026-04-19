package com.mumuk.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResultCode implements BaseCode {

    OK(HttpStatus.OK, "COMMON_200", "성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "COMMON_201", "성공적으로 생성되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON_204", "성공적으로 삭제되었습니다."),


    // User Success
    USER_FETCH_OK(HttpStatus.OK, "USER_200", "유저 정보 조회 성공"),
    TOKEN_REISSUE_OK(HttpStatus.OK, "TOKEN_200", "토큰 재발급 성공"),
    PW_REISSUE_OK(HttpStatus.OK, "USER_200", "유저 비밀번호 변경 성공"),
    USER_LOGOUT_OK(HttpStatus.OK, "USER_200", "유저 로그아웃 성공"),
    USER_WITHDRAW_OK(HttpStatus.NO_CONTENT, "USER_204", "유저 탈퇴 성공"),
    USER_LOGIN_OK(HttpStatus.OK, "USER_200", "유저 로그인 성공"),
    USER_SIGNUP_OK(HttpStatus.CREATED, "USER_201", "유저 회원가입 성공"),
    USER_CHECK_OK(HttpStatus.OK, "USER_200", "중복 여부 검증 성공"),
    PASSWORD_CHECK_OK(HttpStatus.OK, "USER_200", "올바른 비밀번호를 입력하였습니다."),

    //Ingredient Success
    INGREDIENT_REGISTER_OK(HttpStatus.OK, "INGREDIENT_200","재료등록 성공"),
    INGREDIENT_RETRIEVE_OK(HttpStatus.OK, "INGREDIENT_200","등록하신 재료조회 성공"),
    INGREDIENT_UPDATE_OK(HttpStatus.OK, "INGREDIENT_200","재료 수정 성공"),
    INGREDIENT_DELETE_OK(HttpStatus.OK, "INGREDIENT_200","재료 삭제 성공"),
    CLOSED_DATE_INGREDIENT_RETRIEVE_OK(HttpStatus.OK, "INGREDIENT_200","유통기한 임박 재료 조회 성공"),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    // 인터페이스 구현
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
