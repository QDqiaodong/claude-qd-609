package com.archery.range.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.archery.range.domain.SafetyEvent;

import jakarta.persistence.LockModeType;

public interface SafetyEventRepository extends JpaRepository<SafetyEvent, Long> {

    List<SafetyEvent> findAllByOrderByCreatedAtDesc();

    List<SafetyEvent> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByEventNo(String eventNo);

    long countByStatusNot(String status);

    /**
     * 复核与放行前的事件行排他锁（SELECT ... FOR UPDATE）：
     * 把同一事件上的并发复核、并发放行串行化，
     * 保证「同一复核项只有一份结论」「复射只成功一次」。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from SafetyEvent e where e.id = :id")
    Optional<SafetyEvent> findForUpdate(@Param("id") Long id);
}
