package group6.project.model;

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
public class TrainingEntitlement {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private Integer year;

  public TrainingEntitlement() {

  }

}
