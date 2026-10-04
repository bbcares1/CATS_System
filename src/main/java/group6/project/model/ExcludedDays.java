package group6.project.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;

@Entity 
public class ExcludedDays {

  private Integer id;
  private LocalDate date;

  public ExcludedDays() {

  }
}