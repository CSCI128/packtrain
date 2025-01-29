package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Entity(name="user")
@Table(name="users")
public class User{

    @Id
    @Column(name="id")
    // This is set from the Oauth token
    private UUID id;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Column(name="cwid", unique = true, nullable = false)
    private String cwid;

    @Column(name="name", nullable = false)
    private String name;

    @Column(name="email", unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "owningUser")
    private Set<Credential> credential;

    @OneToMany(mappedBy = "user")
    private Set<CourseMember> courseMemberships;

}