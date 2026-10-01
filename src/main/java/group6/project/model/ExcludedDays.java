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
  private LocalDate holidayDate;
  private String description;

  
}
