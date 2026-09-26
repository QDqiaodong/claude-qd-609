package com.archery.range.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 射箭馆词典与业务规则参数表：箭道状态、会员等级折扣、环数计分值、
 * 课程等级门槛、器材类型与状态等。规则集中放在这里，便于统一调整。
 */
public final class RangeDict {

    private RangeDict() {
    }

    // ---------------- 箭道 ----------------
    public static final List<Integer> DISTANCES = List.of(10, 18, 30, 50);

    private static final Map<String, String> LANE_STATUS = Map.of(
            "OPEN", "开放",
            "OCCUPIED", "占用",
            "MAINTENANCE", "维护",
            "LOCKED", "安全锁定");

    /** 开台单次时长上限（小时） */
    public static final int MAX_OPEN_HOURS = 8;

    // ---------------- 会员 ----------------
    private static final List<String> MEMBER_LEVEL_ORDER = List.of("NORMAL", "SILVER", "GOLD");

    private static final Map<String, String> MEMBER_LEVEL = Map.of(
            "NORMAL", "普通会员",
            "SILVER", "银卡会员",
            "GOLD", "金卡会员");

    /** 会员等级对应折扣率 */
    private static final Map<String, BigDecimal> MEMBER_DISCOUNT = Map.of(
            "NORMAL", BigDecimal.valueOf(1.00),
            "SILVER", BigDecimal.valueOf(0.95),
            "GOLD", BigDecimal.valueOf(0.90));

    /** 累计消费达到此金额可升级到的等级（只升不降） */
    private static final Map<String, BigDecimal> MEMBER_UPGRADE = Map.of(
            "SILVER", BigDecimal.valueOf(3000),
            "GOLD", BigDecimal.valueOf(10000));

    public static final BigDecimal RECHARGE_MIN = BigDecimal.valueOf(100);
    public static final BigDecimal RECHARGE_MAX = BigDecimal.valueOf(10000);

    // ---------------- 环数 ----------------
    /** 允许的环数标记：X（内十）/ 10 ~ 1 / M（脱靶） */
    private static final Map<String, Integer> RING_VALUE = Map.ofEntries(
            Map.entry("X", 10),
            Map.entry("10", 10),
            Map.entry("9", 9),
            Map.entry("8", 8),
            Map.entry("7", 7),
            Map.entry("6", 6),
            Map.entry("5", 5),
            Map.entry("4", 4),
            Map.entry("3", 3),
            Map.entry("2", 2),
            Map.entry("1", 1),
            Map.entry("M", 0));

    /** 计分键盘上出现的环数键 */
    public static final List<String> KEYPAD_RINGS = List.of("X", "10", "9", "8", "7", "6", "M");

    /** 一组箭支数只能是 6 支或 12 支 */
    public static final List<Integer> GROUP_SIZES = List.of(6, 12);

    // ---------------- 课程 ----------------
    private static final Map<String, String> COURSE_LEVEL = Map.of(
            "BASIC", "初级",
            "ADVANCED", "进阶",
            "COMPETITION", "竞技");

    /** 课程等级对会员等级的最低要求 */
    private static final Map<String, String> COURSE_MIN_MEMBER_LEVEL = Map.of(
            "BASIC", "NORMAL",
            "ADVANCED", "SILVER",
            "COMPETITION", "GOLD");

    // ---------------- 器材 ----------------
    private static final Map<String, String> EQUIP_TYPE = Map.of(
            "RECURVE", "反曲弓",
            "COMPOUND", "复合弓",
            "TRADITIONAL", "传统弓",
            "GEAR", "护具",
            "ARROW", "箭支");

    private static final Map<String, String> EQUIP_STATUS = Map.of(
            "INSTOCK", "在库",
            "RENTED", "租出",
            "REPAIR", "维修",
            "LOCKED", "安全锁定");

    // ---------------- 回合状态 ----------------
    private static final Map<String, String> ROUND_STATUS = Map.of(
            "ONGOING", "进行中",
            "PAUSED", "暂停中",
            "SUBMITTED", "已提交");

    // ---------------- 停射联锁事件 ----------------
    /** 事件触发类型 */
    private static final Map<String, String> EVENT_TYPE = Map.of(
            "LANE_FAULT", "箭道设备异常",
            "INTRUSION", "人员闯入射线",
            "EQUIP_FAULT", "疑似器材故障",
            "OTHER", "其他安全隐患");

    /** 严重级别 */
    private static final Map<String, String> EVENT_SEVERITY = Map.of(
            "LOW", "一般",
            "MEDIUM", "较重",
            "HIGH", "紧急");

    /** 事件状态：待处置 → 分项复核 → 已放行 */
    private static final Map<String, String> EVENT_STATUS = Map.of(
            "PENDING", "待处置",
            "REVIEWING", "分项复核",
            "RELEASED", "已放行");

    /** 分项复核项：教练确认射线与人员安全 / 器材管理员确认器材已检查 */
    private static final Map<String, String> REVIEW_TYPE = Map.of(
            "COACH", "教练复核",
            "KEEPER", "器材管理员复核");

    /** 复核结论 */
    private static final Map<String, String> REVIEW_CONCLUSION = Map.of(
            "PASS", "通过",
            "REJECT", "不通过");

    /** 联锁台角色 */
    private static final Map<String, String> SAFETY_ROLE = Map.of(
            "MANAGER", "值班经理",
            "COACH", "教练",
            "KEEPER", "器材管理员");

    // ---------------- 取值方法 ----------------

    public static boolean isValidDistance(Integer distance) {
        return distance != null && DISTANCES.contains(distance);
    }

    public static boolean isValidLaneStatus(String status) {
        return status != null && LANE_STATUS.containsKey(status);
    }

    public static String laneStatusName(String status) {
        return status == null ? "" : LANE_STATUS.getOrDefault(status, status);
    }

    public static boolean isValidMemberLevel(String level) {
        return level != null && MEMBER_LEVEL.containsKey(level);
    }

    public static String memberLevelName(String level) {
        return level == null ? "" : MEMBER_LEVEL.getOrDefault(level, level);
    }

    public static BigDecimal memberDiscount(String level) {
        return MEMBER_DISCOUNT.getOrDefault(level, BigDecimal.valueOf(1.00));
    }

    public static int memberLevelRank(String level) {
        int index = MEMBER_LEVEL_ORDER.indexOf(level);
        return index < 0 ? 0 : index + 1;
    }

    /** 依据累计消费算出应得的最高等级 */
    public static String levelByTotalSpend(BigDecimal totalSpend) {
        BigDecimal total = totalSpend == null ? BigDecimal.ZERO : totalSpend;
        if (total.compareTo(MEMBER_UPGRADE.get("GOLD")) >= 0) {
            return "GOLD";
        }
        if (total.compareTo(MEMBER_UPGRADE.get("SILVER")) >= 0) {
            return "SILVER";
        }
        return "NORMAL";
    }

    public static boolean isValidRing(String ring) {
        return ring != null && RING_VALUE.containsKey(ring);
    }

    public static int ringValue(String ring) {
        return RING_VALUE.getOrDefault(ring, 0);
    }

    public static boolean isValidGroupSize(Integer arrowCount) {
        return arrowCount != null && GROUP_SIZES.contains(arrowCount);
    }

    public static boolean isValidCourseLevel(String level) {
        return level != null && COURSE_LEVEL.containsKey(level);
    }

    public static String courseLevelName(String level) {
        return level == null ? "" : COURSE_LEVEL.getOrDefault(level, level);
    }

    public static String courseMinMemberLevel(String level) {
        return COURSE_MIN_MEMBER_LEVEL.getOrDefault(level, "NORMAL");
    }

    public static boolean isValidEquipType(String type) {
        return type != null && EQUIP_TYPE.containsKey(type);
    }

    public static String equipTypeName(String type) {
        return type == null ? "" : EQUIP_TYPE.getOrDefault(type, type);
    }

    public static boolean isValidEquipStatus(String status) {
        return status != null && EQUIP_STATUS.containsKey(status);
    }

    public static String equipStatusName(String status) {
        return status == null ? "" : EQUIP_STATUS.getOrDefault(status, status);
    }

    public static boolean isValidRoundStatus(String status) {
        return status != null && ROUND_STATUS.containsKey(status);
    }

    public static String roundStatusName(String status) {
        return status == null ? "" : ROUND_STATUS.getOrDefault(status, status);
    }

    // ---------------- 停射联锁取值方法 ----------------

    public static boolean isValidEventType(String type) {
        return type != null && EVENT_TYPE.containsKey(type);
    }

    public static String eventTypeName(String type) {
        return type == null ? "" : EVENT_TYPE.getOrDefault(type, type);
    }

    public static boolean isValidEventSeverity(String severity) {
        return severity != null && EVENT_SEVERITY.containsKey(severity);
    }

    public static String eventSeverityName(String severity) {
        return severity == null ? "" : EVENT_SEVERITY.getOrDefault(severity, severity);
    }

    public static boolean isValidEventStatus(String status) {
        return status != null && EVENT_STATUS.containsKey(status);
    }

    public static String eventStatusName(String status) {
        return status == null ? "" : EVENT_STATUS.getOrDefault(status, status);
    }

    public static boolean isValidReviewType(String type) {
        return type != null && REVIEW_TYPE.containsKey(type);
    }

    public static String reviewTypeName(String type) {
        return type == null ? "" : REVIEW_TYPE.getOrDefault(type, type);
    }

    public static boolean isValidConclusion(String conclusion) {
        return conclusion != null && REVIEW_CONCLUSION.containsKey(conclusion);
    }

    public static String conclusionName(String conclusion) {
        return conclusion == null ? "" : REVIEW_CONCLUSION.getOrDefault(conclusion, conclusion);
    }

    public static boolean isValidSafetyRole(String role) {
        return role != null && SAFETY_ROLE.containsKey(role);
    }

    public static String safetyRoleName(String role) {
        return role == null ? "" : SAFETY_ROLE.getOrDefault(role, role);
    }
}
