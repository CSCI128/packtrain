package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity(name="credential")
@Table(name="credentials")
public class Credential {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Column(name="name")
    private String name;

    @Column(name="api_key")
    private String apiKey;

    @Column(name="private")
    private boolean isPrivate;

    @Column(name="active")
    private boolean isActive;

    @ManyToOne(optional=false)
    @JoinColumn(name="user_id", referencedColumnName="id")
    private User owningUser;

    @ManyToOne(optional=false)
    @JoinColumn(name="external_source_id", referencedColumnName="id")
    private ExternalSource externalSource;
}