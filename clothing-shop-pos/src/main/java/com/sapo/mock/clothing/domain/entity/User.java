package com.sapo.mock.clothing.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import com.sapo.mock.clothing.util.constant.GenderEnum;

@Entity
@Setter
@Getter
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "name không được để trống")
    private String name;

    @NotBlank(message = "email không được để trống")
    private String email;

    @NotBlank(message = "password không được để trống")
    private String password;

    private String address;
    private int age;

    @Enumerated(EnumType.STRING)
    private GenderEnum gender;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    @JsonIgnore
    private String refreshToken;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
