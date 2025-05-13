package edu.mines.packtrain.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

@Data
@Entity(name="user")
@Table(name="users", indexes = {@Index(columnList = "oauth_id"), @Index(columnList = "cwid")})
public class User{
    @Id
    @Column(name="cwid", unique = true, nullable = false)
    private String cwid;

    @Column(name="oauth_id", nullable = true)
    private UUID oAuthId;

    @Column(name = "admin", nullable = false)
    private boolean admin = false;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name="name")
    private String name;

    @Column(name="email", unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "owningUser")
    @EqualsAndHashCode.Exclude
    private Set<Credential> credential;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<CourseMember> courseMemberships;


}