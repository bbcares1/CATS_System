package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_detail")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CourseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId;

    @NotBlank(message = "Course title is required")
    @Size(max = 255, message = "Course title must be 255 characters or fewer")
    private String title;

    @NotNull(message = "Course fee is required")
    @DecimalMin(value = "0.0", message = "Course fee must be zero or greater")
    private Double courseFee;

    @Size(max = 2000, message = "Description must be 2,000 characters or fewer")
    private String courseDescription;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @NotNull(message = "Course category is required")
    private CourseCategory courseCategory;
}
