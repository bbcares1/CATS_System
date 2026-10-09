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
public class Staff extends User {}
