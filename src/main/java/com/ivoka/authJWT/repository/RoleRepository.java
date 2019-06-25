package com.ivoka.authJWT.repository;

import com.ivoka.authJWT.model.Role;
import com.ivoka.authJWT.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName roleName);
}
