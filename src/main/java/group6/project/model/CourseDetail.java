package group6.project.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "course_detail")
@Getter
@Setter
@NoArgsConstructor
public class CourseDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId;

    @Column(nullable = false)
    private String title;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal courseFee = BigDecimal.ZERO;

    @Column(length = 2000)
    private String courseDescription;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private CourseCategory courseCategory;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    private CourseProvider provider;

    private boolean customDatesAllowed;
    private boolean active = true;
    @Version private Long version;
}
