package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.UserRequest;
import com.mumuk.domain.user.dto.response.UserResponse;


public interface UserService {
    UserResponse.ProfileInfoDTO profileInfo(Long userId);
    void editProfile(Long UserId, UserRequest.EditProfileReq request);
    void agreeToHealthData(Long userId);
}
