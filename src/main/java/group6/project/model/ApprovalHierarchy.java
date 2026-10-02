package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "approvalhierarchy")
@Getter 
@Setter 
@NoArgsConstructor 
@EqualsAndHashCode 
public class ApprovalHierarchy {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer hierarchyId;

  private String approvalRole;

  private Integer level;

}
