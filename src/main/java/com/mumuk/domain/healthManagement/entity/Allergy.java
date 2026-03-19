package com.mumuk.domain.healthManagement.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name="allergy", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","allergy_type"}))
public class Allergy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name="allergy_type", nullable=false)
    private AllergyType allergyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    //생성자
    public Allergy() {}

    public Allergy(AllergyType allergyType, User user) {
        this.allergyType = allergyType;
        this.user = user;
    }

    //getter
    public Long getId() {return id;}
    public AllergyType getAllergyType() {return allergyType;}
    public User getUser() {return user;}

    //setter
    public void setId(Long id) {this.id = id;}
    public void setAllergyType(AllergyType allergyType) {this.allergyType = allergyType;}
    public void setUser(User user) {this.user = user;};
}
