// We use this User subtype for employees who can apply for training.
package group6.project.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("STAFF")
@Getter
@Setter
@NoArgsConstructor
// Employee capabilities are inherited by Manager; annual limits are stored separately.
public class Staff extends User {}
