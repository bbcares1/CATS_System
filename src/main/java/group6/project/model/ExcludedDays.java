package group6.project.model;

import jakarta.persistence.Entity;

@Entity 
public class ExcludedDays {

  private Integer id;
  private LocalDate date;

  public ExcludedDays() {

  }
}
