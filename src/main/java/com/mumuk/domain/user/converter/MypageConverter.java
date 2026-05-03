package com.mumuk.domain.user.converter;

import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;

public class MypageConverter {

    public static UserResponse.ProfileInfoDTO toProfileInfoDTO(User user) {
        return new UserResponse.ProfileInfoDTO(
                user.getName(),
                user.getNickName(),
                user.getProfileImage(),
                user.getStatusMessage()
        );
    }
}
