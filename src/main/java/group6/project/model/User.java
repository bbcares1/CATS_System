// Stores shared account details and reporting links with a stable account ID.
package group6.project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", length = 20)
@Getter
@Setter
@NoArgsConstructor
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_name", nullable = false, unique = true)
    @NotBlank(message = "Username is required")
    private String userName;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    private String designation;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String staffId;

    private boolean active = true;
    @Version private Long version;

    // A stable identity keeps reporting links and old history valid after a role change.
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    // The discriminator is the only stored role; normal profile updates cannot change it.
    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false, updatable = false, length = 20)
    private Roles role;

    // New accounts must agree with their Java subtype before being inserted.
    @PrePersist
    private void setAccountRole() {
        Roles expected =
                this instanceof Admin
                        ? Roles.ADMIN
                        : this instanceof Manager ? Roles.MANAGER : Roles.STAFF;
        if (role != null && role != expected)
            throw new IllegalArgumentException("Account role does not match its type.");
        role = expected;
    }
}
