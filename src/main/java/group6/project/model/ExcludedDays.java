package group6.project.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.Id;

@Entity 
@Table(name = "excluded_days")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ExcludedDays {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
@NotNull(message = "Holiday date is required")
  private LocalDate holidayDate;
@NotBlank(message = "Description is required")
  private String description;

  
}
 