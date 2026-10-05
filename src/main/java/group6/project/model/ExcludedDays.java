package group6.project.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@EqualsAndHashCode 
public class ExcludedDays {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private LocalDate date;

  public ExcludedDays() {

  }
}