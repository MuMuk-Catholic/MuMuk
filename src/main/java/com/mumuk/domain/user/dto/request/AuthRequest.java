package com.mumuk.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

public class AuthRequest {


    @Getter
    @Setter
    @NoArgsConstructor
    public static class SignUpReq {
        @NotNull
        private String name;

        @NotNull
        private String nickname;

        @NotNull
        private String phoneNumber;

        @NotNull
        private String loginId;

        @NotNull
        private String password;

        @NotNull
        private String confirmPassword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LogInReq{
        @NotNull
        private String loginId;

        @NotNull
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FindIdReq{
        @NotNull
        private String name;

        @NotNull
        private String phoneNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FindPassWordReq{
        @NotNull
        private String loginId;

        @NotNull
        private String name;

        @NotNull
        private String phoneNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CheckCurrentPasswordReq {

        @NotNull
        private String currentPassword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecoverPassWordReq {

        @NotNull
        private String newPassWord;

        @NotNull
        private String confirmPassWord;
    }
}
