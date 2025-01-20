package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name="credential")
public class Crendential{

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Column(name="api key")
    private String apiKey;

    @Column(name="private")
    // I assumed this is a boolean type, maybe it should be a string???
    private boolean is_private;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="user_id")
    // how do we link the credential class to point to the user class
    private String userId;
}