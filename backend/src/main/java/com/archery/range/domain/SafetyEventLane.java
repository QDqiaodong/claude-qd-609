package com.archery.range.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 停射事件-箭道快照：记录事件纳入的箭道及其锁定前的业务状态。
 * 放行时按 prevStatus 逐条恢复，保证不覆盖箭道原有状态。
 */
@Entity
@Table(name = "safety_event_lane")
public class SafetyEventLane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "lane_id", nullable = false)
    private Long laneId;

    @Column(name = "lane_no", nullable = false, length = 16)
    private String laneNo;

    /** 锁定前业务状态：OPEN / OCCUPIED / MAINTENANCE */
    @Column(name = "prev_status", nullable = false, length = 16)
    private String prevStatus;

    /** 放行恢复后置 1 */
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

    public Long getLaneId() {
        return laneId;
    }

    public void setLaneId(Long laneId) {
        this.laneId = laneId;
    }

    public String getLaneNo() {
        return laneNo;
    }

    public void setLaneNo(String laneNo) {
        this.laneNo = laneNo;
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
