package com.archery.range.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archery.range.domain.Lane;

public interface LaneRepository extends JpaRepository<Lane, Long> {

    List<Lane> findAllByOrderByLaneNoAsc();

    List<Lane> findByStatusOrderByLaneNoAsc(String status);

    List<Lane> findByDistanceOrderByLaneNoAsc(Integer distance);

    boolean existsByLaneNo(String laneNo);
}
