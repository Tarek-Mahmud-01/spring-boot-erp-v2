package com.springboot.erp.modules.access.repository;

import com.springboot.erp.modules.access.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Paged list with permissions fetched, for the roles list endpoint. */
    @EntityGraph(attributePaths = {"permissions"})
    @Query(value = "select r from Role r", countQuery = "select count(r) from Role r")
    Page<Role> findAllWithPermissions(Pageable pageable);
}
