package com.mumuk.domain.user.entity;


import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name="users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LoginType loginType = LoginType.LOCAL;

    @Column(name = "알림 동의 여부", nullable = false)
    private Boolean fcmAgreed = false;

    private String profileImage;

    private String email;     // 자체&소셜 Id

    @Column(unique = true)
    private String loginId;   // 자체 로그인 id

    private String password;  // 자체 로그인 pw

    private String name;

    private String nickName;

    private String phoneNumber;

    private String statusMessage;

    @Column(name = "agreed_to_health_data", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean agreedToHealthData = false;

    @Column(name = "내부 식별을 위한 소셜 id")
    private String socialId;

    @Column(length = 300)
    private String refreshToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ingredient> ingredients = new ArrayList<>();

    public User() {

    }

    public User(String name, String nickName, String phoneNumber, String loginId, String password) {
        this.name = name;
        this.nickName = nickName;
        this.phoneNumber = phoneNumber;
        this.loginId = loginId;
        this.password = password;
    }

    public static User of(String name, String nickname, String phoneNumber, String loginId, String encodedPassword) {
        return new User(name, nickname, phoneNumber, loginId, encodedPassword);
    }

    public static User of(String email, String nickName, String profileImage, LoginType loginType, String socialId) {
        User user = new User();
        user.email = email;
        user.nickName = nickName;
        user.profileImage = profileImage;
        user.loginType = loginType;
        user.socialId = socialId;
        return user;
    }

    public static User of(String name,String email, String nickName, String profileImage, LoginType loginType, String socialId) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.nickName = nickName;
        user.profileImage = profileImage;
        user.loginType = loginType;
        user.socialId = socialId;
        return user;
    }

    public void updateProfile(String name, String nickName, String profileImage, String statusMessage) {
        this.name = name;
        this.nickName = nickName;
        this.profileImage = profileImage;
        this.statusMessage = statusMessage;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public LoginType getLoginType() {
        return loginType;
    }

    public Boolean getFcmAgreed() {
        return fcmAgreed;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getNickName() {
        return nickName;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getSocialId() {
        return socialId;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setLoginType(LoginType loginType) {
        this.loginType = loginType;
    }

    public void setFcmAgreed(Boolean fcmAgreed) {
        this.fcmAgreed = fcmAgreed;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNickName(String nickName) {this.nickName = nickName;}


    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setSocialId(String socialId) {
        this.socialId = socialId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public void agreeToHealthData() {
        this.agreedToHealthData = true;
    }

    public boolean isAgreedToHealthData() {
        return agreedToHealthData;
    }

    public void setAgreedToHealthData(boolean agreedToHealthData) {
        this.agreedToHealthData = agreedToHealthData;
    }
}
