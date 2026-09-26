package com.archery.range.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 停射事件-回合快照：事件生效时被暂停的进行中回合。
 * 箭支与分数不动（仍由 round_arrow 的 shot_index 保序），只暂停状态，
 * 放行后从原进度继续。
 */
@Entity
@Table(name = "safety_event_round")
public class SafetyEventRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "round_no", nullable = false, length = 24)
    private String roundNo;

    /** 暂停前状态（只会是 ONGOING） */
    @Column(name = "prev_status", nullable = false, length = 16)
    private String prevStatus;

    @Column(nullable = false)
    private Boolean restored;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public String getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(String roundNo) {
        this.roundNo = roundNo;
    }

    public String getPrevStatus() {
        return prevStatus;
    }

    public void setPrevStatus(String prevStatus) {
        this.prevStatus = prevStatus;
    }

    public Boolean getRestored() {
        return restored;
    }

    public void setRestored(Boolean restored) {
        this.restored = restored;
    }
}
