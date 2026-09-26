package com.archery.range.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 停射事件-器材快照：记录事件纳入的器材及其锁定前的业务状态与租借人。
 * 放行时按快照恢复，原本租出的器材仍归原会员。
 */
@Entity
@Table(name = "safety_event_equipment")
public class SafetyEventEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "equip_code", nullable = false, length = 24)
    private String equipCode;

    /** 锁定前业务状态：INSTOCK / RENTED / REPAIR */
    @Column(name = "prev_status", nullable = false, length = 16)
    private String prevStatus;

    @Column(name = "prev_renter_id")
    private Long prevRenterId;

    @Column(name = "prev_renter_name", length = 32)
    private String prevRenterName;

    @Column(name = "prev_rented_at")
    private LocalDateTime prevRentedAt;

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

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipCode() {
        return equipCode;
    }

    public void setEquipCode(String equipCode) {
        this.equipCode = equipCode;
    }

    public String getPrevStatus() {
        return prevStatus;
    }

    public void setPrevStatus(String prevStatus) {
        this.prevStatus = prevStatus;
    }

    public Long getPrevRenterId() {
        return prevRenterId;
    }

    public void setPrevRenterId(Long prevRenterId) {
        this.prevRenterId = prevRenterId;
    }

    public String getPrevRenterName() {
        return prevRenterName;
    }

    public void setPrevRenterName(String prevRenterName) {
        this.prevRenterName = prevRenterName;
    }

    public LocalDateTime getPrevRentedAt() {
        return prevRentedAt;
    }

    public void setPrevRentedAt(LocalDateTime prevRentedAt) {
        this.prevRentedAt = prevRentedAt;
    }

    public Boolean getRestored() {
        return restored;
    }

    public void setRestored(Boolean restored) {
        this.restored = restored;
    }
}
