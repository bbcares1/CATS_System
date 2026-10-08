package group6.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter 
@NoArgsConstructor 
public abstract class User {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;
    
   // @Column(name = "user_name",nullable = false, unique = true)
    private String userName;

   // @Column(name = "password",nullable = false)
    private String password;

   // @Column(name = "name",nullable = false)
    private String name;

   // @Column(name = "designation")
    private String designation; 
    
    @Enumerated(EnumType.STRING)
   // @Column(name = "role",nullable = false)
    private Roles role; 

	public User( String userName, String password, String name, String designation, Roles role,
			User manager) {
		this.userName = userName;
		this.password = password;
		this.name = name;
		this.designation = designation;
		this.role = role;
	}

}