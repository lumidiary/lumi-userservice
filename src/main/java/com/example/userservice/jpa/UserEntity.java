package com.example.userservice.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String encryptedPwd;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Theme theme;

    @Column(length = 1000)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    public void markDeleted(LocalDateTime when) {
        this.deleted = true;
        this.deletedAt = when;
    }

    public void changeName(String newName) {
        this.name = newName;
    }

    public void changeTheme(Theme newTheme) {

        this.theme = newTheme;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfileImageUrl(String url) {
        this.profileImageUrl = url;
    }

    public void changeBirthDate(LocalDate newBirthDate) {
        this.birthDate = newBirthDate;
    }

    public void setEncryptedPwd(String encryptedPwd) {
        this.encryptedPwd = encryptedPwd;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
