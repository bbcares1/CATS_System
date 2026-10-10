package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Version;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
   @NotBlank(message = "Category name is required")
   @Size(max = 100, message = "Category name must be 100 characters or fewer")
   private String categoryName;

   // Labels can change, but business rules use the three agreed category types.
   @Enumerated(EnumType.STRING)
   private CourseCategoryType kind;
   @Version
   private Long version;

}
