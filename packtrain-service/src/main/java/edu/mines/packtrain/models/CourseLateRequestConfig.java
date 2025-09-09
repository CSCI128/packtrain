package edu.mines.packtrain.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@Entity(name = "course_late_request_config")
@Table(name = "course_late_request_configs")
public class CourseLateRequestConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "late_passes_enabled", nullable = false)
    private boolean latePassesEnabled = false;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "enabled_extension_reasons",
            joinColumns = @JoinColumn(name = "extension_config_id"))
    @Column(name = "extension_reason")
    private List<String> enabledExtensionReasons;

    @Column(name = "total_late_passes_allowed", nullable = false)
    private int totalLatePassesAllowed = 0;

    @Column(name = "late_pass_name")
    private String latePassName;

}
