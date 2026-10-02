package group6.project.model;

import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@EqualsAndHashCode 
public class TrainingEntitlement {

  private Integer id;
  private Integer year;

  public TrainingEntitlement() {

  }

}
