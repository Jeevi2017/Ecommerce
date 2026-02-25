package com.example.Ecomm.entitiy;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;




@Entity
@Table(name = "admin_user")
@PrimaryKeyJoinColumn(name = "user_id")
public class Admin extends User {

    // Required by JPA
    public Admin() {
        super();
    }

    /*
     * NOTE:
     * Removed long parameterized constructors to comply with SonarQube:
     * "Constructor has more than 7 parameters".
     *
     * This does NOT affect functionality:
     * - JPA uses the no-arg constructor
     * - Business logic should construct User/Admin via services or factories
     */

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
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
