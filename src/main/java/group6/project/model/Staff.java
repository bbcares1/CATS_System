package group6.project.model;

import jakarta.persistence.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Staff extends User {

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "manager_id")
    private Manager manager;


}