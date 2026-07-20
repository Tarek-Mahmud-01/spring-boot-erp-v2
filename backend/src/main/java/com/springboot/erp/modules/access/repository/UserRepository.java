package com.springboot.erp.modules.access.repository;

import com.springboot.erp.modules.access.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Load a user with roles + permissions eagerly (single query, no N+1) for
     * authentication — the JWT needs the flattened permission set.
     */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("select u from User u where u.username = :username")
    Optional<User> findByUsernameWithAuthorities(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("select u from User u where u.publicId = :publicId")
    Optional<User> findByPublicIdWithAuthorities(String publicId);

    /** Paged list with roles fetched, for the users list endpoint. */
    @EntityGraph(attributePaths = {"roles"})
    @Query(value = "select u from User u", countQuery = "select count(u) from User u")
    Page<User> findAllWithRoles(Pageable pageable);
}
