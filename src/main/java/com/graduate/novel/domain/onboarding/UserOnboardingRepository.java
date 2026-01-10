package com.graduate.novel.domain.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {

    Optional<UserOnboarding> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByCompleted(Boolean completed);
}

