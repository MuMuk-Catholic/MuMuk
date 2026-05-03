package com.mumuk.domain.user.controller;

import com.mumuk.domain.user.dto.request.UserRequest;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.service.UserService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Tag(name = "프로필 기능 관련")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "프로필 조회", description = "사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/profile")
    public Response<UserResponse.ProfileInfoDTO> profileInfo(@AuthUser Long userId) {
        return Response.ok(userService.profileInfo(userId));
    }

    @Operation(summary ="프로필 수정", description = "사용자의 프로필 정보를 수정합니다.")
    @PatchMapping("/profile")
    public Response<String> editProfile(@AuthUser Long userId, @Valid @RequestBody UserRequest.EditProfileReq req) {
        userService.editProfile(userId, req);
        return Response.ok(ResultCode.EDIT_PROFILE_OK, "프로필이 수정되었습니다.");
    }
}
