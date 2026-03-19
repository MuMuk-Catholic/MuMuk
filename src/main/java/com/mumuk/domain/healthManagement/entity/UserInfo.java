package com.mumuk.domain.healthManagement.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name="user_info")
public class UserInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Gender gender=Gender.NONE;

    @Column(nullable = false)
    @NotNull
    @Min(value=1, message = "신장은 1cm 이상이어야 합니다")
    private Double height;
    @Column(nullable = false)
    @NotNull
    @Min(value = 1,message ="체중은 1kg 이상이어야 합니다")
    private Double weight;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;


    //생성자
    public UserInfo() {}
    public UserInfo(Long id, Gender gender, Double height, Double weight, User user) {
        this.id = id;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.user = user;
    }

    //getter
    public Long getId() {return id;}
    public Gender getGender() {return gender;}
    public Double getHeight() {return height;}
    public Double getWeight() {return weight;}
    public User getUser() {return user;}

    //setter
    public void setUser(User user) {this.user = user;}
    public void setGender(Gender gender) {this.gender = gender;}
    public void setHeight(Double height) {this.height = height;}
    public void setWeight(Double weight) {this.weight = weight;}

}
