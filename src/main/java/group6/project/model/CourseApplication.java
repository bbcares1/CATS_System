
package group6.project.model;

import java.time.LocalDate;

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
@Table(name = "courseApplicaton")
@Getter 
@Setter 
@NoArgsConstructor 
@EqualsAndHashCode 
public class CourseApplication {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer courseId;

    private double CourseFee;

    private LocalDate CourseStartDate;
    
    private LocalDate CourseEndDate;

    private String CourseTitle;

    private Enum CourseCategory;

    private String reason;

    
}
