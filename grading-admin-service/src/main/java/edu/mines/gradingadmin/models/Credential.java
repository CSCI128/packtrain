package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity(name="credential")
@Table(name="credentials")
public class Crendential{

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Column(name="api_key")
    private String apiKey;

    @Column(name="private")
    private boolean is_private;

    @ManyToOne(optional=false, fetch=FetchType.EAGER)
    @JoinColumn(name="user_id", referencedColumnName="id")
    private String userId;
}