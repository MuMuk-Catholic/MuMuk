package com.mumuk.domain.user.service;

import com.mumuk.domain.user.converter.MypageConverter;
import com.mumuk.domain.user.dto.request.UserRequest;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserResponse.ProfileInfoDTO profileInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        return MypageConverter.toProfileInfoDTO(user);
    }

    @Override
    @Transactional
    public void editProfile(Long userId, UserRequest.EditProfileReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(
                request.getName(),
                request.getNickName(),
                request.getProfileImage(),
                request.getStatusMessage()
        );
    }
}
