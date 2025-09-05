package edu.mines.packtrain.models;

import edu.mines.packtrain.models.converters.EncryptionConverter;
import edu.mines.packtrain.models.enums.CredentialType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;

@Data
@Entity(name = "credential")
@Table(name = "credentials")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "api_key")
    @Convert(converter = EncryptionConverter.class)
    private String apiKey;

    @Column(name = "private")
    private boolean isPrivate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid")
    @ToString.Exclude
    private User owningUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type")
    private CredentialType type;
}