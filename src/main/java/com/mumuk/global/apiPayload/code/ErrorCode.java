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

    // Recipe Error
    RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "RECIPE_404", "레시피를 찾을 수 없습니다."),
    RECIPE_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECIPE_409", "이미 존재하는 레시피입니다."),
    RECIPE_INVALID_DATA(HttpStatus.BAD_REQUEST, "RECIPE_400", "잘못된 레시피 데이터입니다."),
    RECIPE_CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "RECIPE_400_CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),
    RECIPE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RECIPE_500_SAVE", "레시피 저장에 실패했습니다."),
    RECIPE_DUPLICATE_TITLE(HttpStatus.CONFLICT, "RECIPE_409_DUPLICATE", "이미 존재하는 레시피 제목입니다."),
    RECIPE_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "RECIPE_400_INVALID_CATEGORY", "유효하지 않은 레시피 카테고리입니다."),
    RECIPE_EMPTY_INGREDIENTS(HttpStatus.BAD_REQUEST, "RECIPE_400_EMPTY_INGREDIENTS", "재료 정보가 비어있습니다."),

    // UserRecipe Error
    USER_RECIPE_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_RECIPE_409", "이미 존재하는 정보입니다."),
    RECENT_RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND,"USER_RECIPE_404", "최근 레시피가 존재하지 않습니다"),
    PAGE_INVALID(HttpStatus.BAD_REQUEST,"PAGE_INVALID_400", "페이지 값이 유효하지 않습니다"),

    // AI (Provider-agnostic)
    OPENAI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_500", "AI API에서 오류가 발생했습니다."),
    OPENAI_INVALID_RESPONSE(HttpStatus.BAD_REQUEST, "OPENAI_400_INVALID_RESPONSE", "AI API의 응답 포맷이 잘못되었습니다."),
    OPENAI_NO_CHOICES(HttpStatus.BAD_REQUEST, "OPENAI_400_NO_CHOICES", "AI API 응답에서 선택지가 없습니다."),
    OPENAI_MISSING_CONTENT(HttpStatus.BAD_REQUEST, "OPENAI_400_MISSING_CONTENT", "AI API 응답 메시지에 내용이 없습니다."),
    OPENAI_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "OPENAI_408_TIMEOUT", "AI API 호출 시간이 초과되었습니다."),
    OPENAI_INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "OPENAI_401_INVALID_API_KEY", "유효하지 않은 AI API 키입니다."),
    OPENAI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "OPENAI_503_SERVICE_UNAVAILABLE", "AI 서비스가 일시적으로 사용 불가능합니다."),
    OPENAI_EMPTY_RECOMMENDATIONS(HttpStatus.BAD_REQUEST, "OPENAI_400_EMPTY_RECOMMENDATIONS", "AI 추천 결과가 비어있습니다."),
    OPENAI_JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST, "OPENAI_400_JSON_PARSE", "AI 응답 JSON 파싱에 실패했습니다."),

    // Naver
    NAVER_JSON_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500_JSON", "네이버 프로필 파싱 중 오류가 발생했습니다."),
    NAVER_API_ERROR(HttpStatus.BAD_GATEWAY, "NAVER_502_API", "네이버 서버와의 통신 중 오류가 발생했습니다."),
    NAVER_INVALID_GRANT(HttpStatus.UNAUTHORIZED, "NAVER_401_INVALID_GRANT", "유효하지 않거나 만료된 인가 코드입니다."),
    NAVER_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "NAVER_401_AUTH_FAILED", "네이버 인증에 실패했습니다."),
    // KaKao
    KAKAO_JSON_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "KAKAO_500_JSON", "카카오 프로필 파싱 중 오류가 발생했습니다."),
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_502_API", "카카오 서버와의 통신 중 오류가 발생했습니다."),
    KAKAO_INVALID_GRANT(HttpStatus.UNAUTHORIZED, "KAKAO_401_INVALID_GRANT", "유효하지 않거나 만료된 인가 코드입니다."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_401_AUTH_FAILED", "카카오 인증에 실패했습니다."),
    ALREADY_REGISTERED_WITH_OTHER_LOGIN(HttpStatus.CONFLICT, "AUTH_409_ALREADY_REGISTERED", "해당 이메일은 다른 로그인 방식으로 이미 가입되어 있습니다."),
    SOCIAL_LOGIN_INVALID_STATE(HttpStatus.UNAUTHORIZED, "SOCIAL_401_INVALID_GRANT", "유효하지 않거나 만료된 STATE 값입니다.."),

    // Search Error
    KEYWORD_NOT_FOUND(HttpStatus.BAD_REQUEST, "SEARCH_400", "검색하려는 단어가 존재하지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "SEARCH_400", "단어를 한 글자 이상 입력해야 합니다."),

    SEARCH_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "SEARCH_404", "사용자의 검색 기록이 존재하지 않습니다." ),
    SEARCH_LOG_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "SEARCH_404", "해당 사용자가 존재하지 않습니다." ),
    SEARCH_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "SEARCH_404", "검색 결과가 존재하지 않습니다."),

    NAVER_API_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_500", "네이버 API 응답을 파싱하는 도중 오류가 발생했습니다."),
    NAVER_SHOPPING_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_501", "네이버 쇼핑 API 호출중 오류가 발생했습니다."),

    //HealthManagement Error
    ALLERGY_NOT_FOUND(HttpStatus.BAD_REQUEST,"ALLERGY_404", "해당 알러지 타입은 존재하지 않습니다,"),
    ALLERGY_NONE_WITH_OTHERS(HttpStatus.BAD_REQUEST, "ALLERGY_400", "알러지 없음과 다른 알러지를 동시에 선택할 수 없습니다"),

    HEALTHGOAL_NOT_FOUND(HttpStatus.BAD_REQUEST,"HEALTHGOAL_404", "해당 건강목표는 존재하지 않습니다."),
    HEALTHGOAL_NONE_WITH_OTHERS(HttpStatus.BAD_REQUEST,"HEALTHGOAL_400", "건강목표 없음과 다른 건강목표를 동시에 선택할 수 없습니다."),


    USERINFO_NOT_FOUND(HttpStatus.BAD_REQUEST,"USERINFO_404","사용자 정보가 존재하지 않습니다"),
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
