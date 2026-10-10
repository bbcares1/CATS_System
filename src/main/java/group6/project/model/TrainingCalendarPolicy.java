// We lock this single row while changing holidays or saving dates to keep their checks consistent.
package group6.project.model;

import jakarta.persistence.*;

import lombok.*;

// The single calendar row coordinates holiday edits with schedule validation.
@Entity
@Table(name = "training_calendar_policy")
@Getter
@NoArgsConstructor
public class TrainingCalendarPolicy {
    @Id private Integer id;
}
