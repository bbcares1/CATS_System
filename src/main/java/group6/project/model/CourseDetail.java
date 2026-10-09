package group6.project.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    private String title;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal courseFee = BigDecimal.ZERO;

    @Column(length = 2000)
    private String courseDescription;

    private String trainingProvider;
    private boolean customDatesAllowed;
    private boolean active = true;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private CourseCategory courseCategory;
}
