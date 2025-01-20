package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name="user")
public class User{

    @Id
    @GeneratedValue(stragety=GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Column(name="canvas_id")
    private String canvasId;

    @Column(name"is_admin")
    private boolean isAdmin;

    @Column(name="CWID")
    private String CWID;

    @Column(name="name")
    private String name;

    @Column(name="email")
    private String emial;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="credential_id")
    // how do we link the user class to point to the credential class
    private String credentialId;

    @ManyToMany(fetch=FetchType.EAGER)
    // not sure this relationship is correct
    @JoinColumn(name="section_id")
    private String sectionId;
}