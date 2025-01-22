package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity(name="user")
@Table(name="users")
public class User{

    @Id
    @GeneratedValue(stragety=GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Column(name="canvas_id")
    private String canvasId;

    @Column(name"is_admin")
    private boolean isAdmin;

    @Column(name="cwid")
    private String cwid;

    @Column(name="name")
    private String name;

    @Column(name="email")
    private String emial;

    @OneToMany(optional=false, fetch=FetchType.EAGER)
    @JoinColumn(name="credential_id", referencedColumnName="id")
    private String credentialId;

    @ManyToOne(optional=false , fetch=FetchType.EAGER)
    @JoinColumn(name="external_source_id", referencedColumnName="id")
    private String externalSourceId;
}
}