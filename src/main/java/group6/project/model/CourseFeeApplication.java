package group6.project.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_fee_application")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CourseFeeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Double courseFee;

    private String applicationStatus;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;
}
