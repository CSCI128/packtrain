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
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Column(name="canvas_id", unique = true)
    private String canvasId;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Column(name="cwid", unique = true, nullable = false)
    private String cwid;

    @Column(name="name")
    private String name;

    @Column(name="email", unique = true, nullable = false)
    private String email;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="credential_id", referencedColumnName="id")
    private Set<Credential> credential;

}