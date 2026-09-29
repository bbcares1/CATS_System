package group6.project.model;

import jakarta.persistence.Entity;

@Entity
public class Manager extends Staff {

  private Integer id;
  private String staffNo;

  public Manager() {

  }

}
