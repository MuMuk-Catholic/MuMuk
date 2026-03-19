package com.mumuk.domain.healthManagement.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name="health_goal")
public class HealthGoal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name="goal_name", nullable = false)
    private HealthGoalType goalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    //생성자
    public HealthGoal() {}
    public HealthGoal(HealthGoalType goalName, User user) {
        this.goalName = goalName;
        this.user = user;
    }

    //getter
    public Long getId() {return id;}
    public HealthGoalType getGoalName() {return goalName;}
    public User getUser() {return user;}

    //setter
    public void setId(Long id) {this.id = id;}
    public void setGoalName(HealthGoalType goalName) {this.goalName = goalName;}
    public void setUser(User user) {this.user = user;}

}
