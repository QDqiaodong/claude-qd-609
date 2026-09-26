package com.archery.range.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 分项复核结论：每个事件的每个复核项（教练 / 器材管理员）只有一份最终结论。
 * 表上 uk_event_review (event_id, review_type) 唯一约束兜底并发重复提交。
 */
@Entity
@Table(name = "safety_review")
public class SafetyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /** COACH 教练复核 / KEEPER 器材管理员复核 */
    @Column(name = "review_type", nullable = false, length = 16)
    private String reviewType;

    /** PASS 通过 / REJECT 不通过 */
    @Column(nullable = false, length = 8)
    private String conclusion;

    @Column(name = "reviewer_name", nullable = false, length = 32)
    private String reviewerName;

    /** 复核说明；不通过时必填，用于保留原因 */
    @Column(length = 256)
    private String note;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

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

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
