package com.archery.range.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archery.range.domain.Round;

public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findAllByOrderByStartTimeDesc();

    List<Round> findByMemberIdOrderByStartTimeDesc(Long memberId);

    List<Round> findByLaneIdOrderByStartTimeDesc(Long laneId);

    List<Round> findByStatusOrderByStartTimeDesc(String status);

    List<Round> findByMemberIdAndStatusOrderByStartTimeDesc(Long memberId, String status);

    /** 停射联锁用：捞出一批箭道上所有进行中的应用回合（用于暂停） */
    List<Round> findByLaneIdInAndStatus(Collection<Long> laneIds, String status);

    boolean existsByRoundNo(String roundNo);

    long countByMemberId(Long memberId);

    long countByStatus(String status);
}
