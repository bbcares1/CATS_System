package group6.project.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseCategoryForm {
    private Long version;
    @NotBlank @Size(max = 100)
    private String categoryName;
    @NotNull
    private group6.project.model.CourseCategoryType kind;
}
