package group6.project.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class CourseProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer providerId;

    @Column(nullable = false, unique = true)
    private String name;

    private String website;
    private String email;
    private boolean active = true;
    @Version private Long version;
}
