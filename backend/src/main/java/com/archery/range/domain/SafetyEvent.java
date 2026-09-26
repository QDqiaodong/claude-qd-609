package com.archery.range.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 安全停射联锁事件：值班经理发现异常时建立，锁定受影响箭道与器材、
 * 暂停进行中的回合；经教练与器材管理员双人复核通过后才能复射放行。
 *
 * 状态机：PENDING 待处置 → REVIEWING 分项复核 → RELEASED 已放行。
 */
@Entity
@Table(name = "safety_event")
public class SafetyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_no", nullable = false, unique = true, length = 24)
    private String eventNo;

    /** LANE_FAULT 箭道设备异常 / INTRUSION 人员闯入射线 / EQUIP_FAULT 疑似器材故障 / OTHER 其他 */
    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    /** LOW 一般 / MEDIUM 较重 / HIGH 紧急 */
    @Column(nullable = false, length = 16)
    private String severity;

    /** 停射原因（一句话） */
    @Column(nullable = false, length = 128)
    private String reason;

    /** 现场说明 */
    @Column(nullable = false, length = 512)
    private String description;

    /** PENDING 待处置 / REVIEWING 分项复核 / RELEASED 已放行 */
    @Column(nullable = false, length = 16)
    private String status;

    /** 创建人（值班经理姓名），创建人不能代替两个复核角色签字 */
    @Column(name = "created_by", nullable = false, length = 32)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "released_by", length = 32)
    private String releasedBy;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventNo() {
        return eventNo;
    }

    public void setEventNo(String eventNo) {
        this.eventNo = eventNo;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(String releasedBy) {
        this.releasedBy = releasedBy;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }
}
