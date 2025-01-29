package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Entity(name="user")
@Table(name="users", indexes = {@Index(columnList = "oauth_id"), @Index(columnList = "cwid")})
public class User{
    @Id
    @Column(name="cwid", unique = true, nullable = false)
    private String cwid;

    @Column(name="oauth_id", unique = true)
    private UUID oauthId;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Column(name = "is_user", nullable = false)
    private boolean isUser = false;

    @Column(name="name")
    private String name;

    @Column(name="email", unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "owningUser")
    private Set<Credential> credential;

    @OneToMany(mappedBy = "user")
    private Set<CourseMember> courseMemberships;

}