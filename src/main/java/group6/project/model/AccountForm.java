package group6.project.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@EqualsAndHashCode
@NoArgsConstructor 
public class AccountForm {
    
    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must be 100 characters or fewer")
    private String userName;
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "Email must be 255 characters or fewer")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password must be 255 characters or fewer")
    private String password;
    private String designation;
    private String staffId;
    @NotNull(message = "Please select an account type")
    private Roles role;

    private Integer managerId;
    private Double trainingBudget;
    private Integer trainingDays;

    

}
