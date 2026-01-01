package com.graduate.novel.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByActive(Boolean active);

    // Search by email (contains)
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    // Search by display name (contains)
    Page<User> findByDisplayNameContainingIgnoreCase(String displayName, Pageable pageable);

    // Filter by active status
    Page<User> findByActive(Boolean active, Pageable pageable);

    // Search by email or display name
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Advanced search with multiple filters
    @Query("SELECT u FROM User u WHERE " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:displayName IS NULL OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :displayName, '%'))) AND " +
           "(:active IS NULL OR u.active = :active)")
    Page<User> advancedSearch(
            @Param("email") String email,
            @Param("displayName") String displayName,
            @Param("active") Boolean active,
            Pageable pageable);
}
