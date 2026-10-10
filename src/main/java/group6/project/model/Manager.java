// We give Managers the Staff capabilities as well as their approval role.
package group6.project.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("MANAGER")
@NoArgsConstructor
public class Manager extends Staff {}
