package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@NoArgsConstructor 
public class CourseCategory {
   @Id 
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Integer categoryId;
   private String categoryName;
   
}
