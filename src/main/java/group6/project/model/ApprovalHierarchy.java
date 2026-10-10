package group6.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

 

  @NotNull(message = "Approval level is required")
  @Min(value = 1, message = "Approval level must be at least 1")
  private Integer level;

  @Enumerated(EnumType.STRING)
  @Column (name = "approval_role")
  @NotNull(message = "Approver role is required")
  private Roles role;

}
