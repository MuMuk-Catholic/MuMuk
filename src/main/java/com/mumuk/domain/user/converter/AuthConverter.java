package com.mumuk.domain.user.converter;


import com.mumuk.domain.user.entity.User;

public class AuthConverter {

    public static User toUser(String name, String nickname, String phoneNumber, String loginId, String encodedPassword) {
        return User.of(name, nickname, phoneNumber, loginId, encodedPassword);
    }
}
