package com.archery.range.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archery.range.domain.SafetyReview;

public interface SafetyReviewRepository extends JpaRepository<SafetyReview, Long> {

    List<SafetyReview> findByEventIdOrderByIdAsc(Long eventId);

    boolean existsByEventIdAndReviewType(Long eventId, String reviewType);
}
