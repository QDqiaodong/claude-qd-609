package com.archery.range.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 安全停射联锁台的入参与出参。
 */
public final class SafetyDtos {

    private SafetyDtos() {
    }

    /** 建立停射事件：值班经理选择受影响箭道与器材，写明原因、严重级别与现场说明 */
    public record EventCreateReq(
            @NotBlank(message = "请填写操作人姓名") String operatorName,
            @NotBlank(message = "请选择当前角色") String operatorRole,
            @NotBlank(message = "请选择事件类型") String eventType,
            @NotBlank(message = "请选择严重级别") String severity,
            @NotBlank(message = "请填写停射原因") @Size(max = 128, message = "停射原因不能超过 128 字") String reason,
            @NotBlank(message = "请填写现场说明") @Size(max = 512, message = "现场说明不能超过 512 字") String description,
            List<Long> laneIds,
            List<Long> equipmentIds) {
    }

    /** 仅需要操作人身份的动作：转入分项复核、复射放行 */
    public record OperatorReq(
            @NotBlank(message = "请填写操作人姓名") String operatorName,
            @NotBlank(message = "请选择当前角色") String operatorRole) {
    }

    /** 分项复核：角色决定复核项（教练 / 器材管理员），结论 + 说明 */
    public record ReviewReq(
            @NotBlank(message = "请填写复核人姓名") String operatorName,
            @NotBlank(message = "请选择当前角色") String operatorRole,
            @NotBlank(message = "请选择复核结论") String conclusion,
            @Size(max = 256, message = "复核说明不能超过 256 字") String note) {
    }

    public record EventView(
            Long id,
            String eventNo,
            String eventType,
            String eventTypeName,
            String severity,
            String severityName,
            String reason,
            String description,
            String status,
            String statusName,
            String createdBy,
            LocalDateTime createdAt,
            String releasedBy,
            LocalDateTime releasedAt,
            int laneCount,
            int equipmentCount,
            int roundCount) {
    }

    /** 受影响箭道：含锁定前业务状态与是否已恢复 */
    public record LaneLockView(
            Long laneId,
            String laneNo,
            String prevStatus,
            String prevStatusName,
            Boolean restored) {
    }

    /** 受影响器材：含锁定前状态与租借人 */
    public record EquipLockView(
            Long equipmentId,
            String equipCode,
            String prevStatus,
            String prevStatusName,
            String prevRenterName,
            Boolean restored) {
    }

    /** 被暂停的回合：保留进度信息（已记箭支 / 本组支数） */
    public record RoundLockView(
            Long roundId,
            String roundNo,
            String memberName,
            String laneNo,
            String prevStatus,
            String prevStatusName,
            Integer shotCount,
            Integer arrowCount,
            Boolean restored) {
    }

    public record ReviewView(
            String reviewType,
            String reviewTypeName,
            String conclusion,
            String conclusionName,
            String reviewerName,
            String note,
            LocalDateTime reviewedAt) {
    }

    public record LogView(
            String action,
            String fromStatus,
            String fromStatusName,
            String toStatus,
            String toStatusName,
            String operatorName,
            String operatorRole,
            String operatorRoleName,
            String detail,
            LocalDateTime createdAt) {
    }

    /** 事件详情：事件本体 + 受影响资源 + 复核结论 + 完整状态轨迹 */
    public record EventDetailView(
            EventView event,
            List<LaneLockView> lanes,
            List<EquipLockView> equipment,
            List<RoundLockView> rounds,
            List<ReviewView> reviews,
            List<LogView> logs) {
    }
}
