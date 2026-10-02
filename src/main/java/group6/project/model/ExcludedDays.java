package group6.project.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class ExcludedDays {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private LocalDate date;

  public ExcludedDays() {

  }
}
