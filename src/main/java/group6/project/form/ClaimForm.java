// Collects the completed course, personal-payment confirmation and evidence files.
package group6.project.form;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

// The claim amount comes from the completed course, never from this form.
@Getter
@Setter
public class ClaimForm {
    @NotNull private Integer courseId;
    private boolean paidPersonally;
    @NotNull private MultipartFile receipt;
    @NotNull private MultipartFile certificate;
    private Integer reviewerId;
}
