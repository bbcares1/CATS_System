package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import group6.project.model.User;

@Entity 
@Table(name= "admin")
@Getter 
@Setter 
@NoArgsConstructor 
@EqualsAndHashCode(callSuper = true) 
public class Admin extends User{
    
    
    private String staffNo;
}
