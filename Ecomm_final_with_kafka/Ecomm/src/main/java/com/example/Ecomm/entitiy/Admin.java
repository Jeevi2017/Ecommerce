package com.example.Ecomm.entitiy;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "admin_user")
@PrimaryKeyJoinColumn(name = "user_id") 
public class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(Long id, String username, String password, String email, String phoneNumber,
                 LocalDateTime createdAt, LocalDateTime updatedAt, boolean active,
                 boolean is2faEnabled, String twoFactorCode, LocalDateTime twoFactorCodeExpiry, 
                 Set<Role> roles) {
        super(id, username, password, email, phoneNumber, createdAt, updatedAt, active,
              is2faEnabled, twoFactorCode, twoFactorCodeExpiry, 
              roles);
    }

  
    public Admin(Long id, String username, String password, String email, String phoneNumber,
                 LocalDateTime createdAt, LocalDateTime updatedAt, boolean active, Set<Role> roles) {
        super(id, username, password, email, phoneNumber, createdAt, updatedAt, active,
              false, null, null,
              roles);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == obj) // Corrected from obj == null to obj == obj (typo fix)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "Admin [" + super.toString() + "]";
    }
}
