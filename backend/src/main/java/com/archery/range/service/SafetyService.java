package com.archery.range.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.archery.range.common.BizException;
import com.archery.range.domain.Equipment;
import com.archery.range.domain.Lane;
import com.archery.range.domain.RangeDict;
import com.archery.range.domain.Round;
import com.archery.range.domain.SafetyEvent;
import com.archery.range.domain.SafetyEventEquipment;
import com.archery.range.domain.SafetyEventLane;
import com.archery.range.domain.SafetyEventLog;
import com.archery.range.domain.SafetyEventRound;
import com.archery.range.domain.SafetyReview;
import com.archery.range.dto.SafetyDtos;
import com.archery.range.repository.EquipmentRepository;
import com.archery.range.repository.LaneRepository;
import com.archery.range.repository.RoundRepository;
import com.archery.range.repository.SafetyEventEquipmentRepository;
import com.archery.range.repository.SafetyEventLaneRepository;
import com.archery.range.repository.SafetyEventLogRepository;
import com.archery.range.repository.SafetyEventRepository;
import com.archery.range.repository.SafetyEventRoundRepository;
import com.archery.range.repository.SafetyReviewRepository;

/**
 * 安全停射联锁台。
 *
 * 联锁模型：事件生效时把受影响箭道置为 LOCKED、器材置为 LOCKED、
 * 进行中的回合置为 PAUSED，并在快照表里记录各自锁定前的业务状态；
 * 复射放行时在同一事务里按快照逐条恢复 —— 要么全部恢复，要么整体回滚，
 * 不会留下「事件已放行但部分资源仍被锁住」或「解除到一半」的中间态。
 *
 * 并发模型：复核与放行都先 {@code SELECT ... FOR UPDATE} 锁住事件行，
 * 把并发操作串行化；safety_review 的 (event_id, review_type) 唯一约束兜底，
 * 保证同一复核项只有一份最终结论、复射只成功一次。
 */
@Service
public class SafetyService {

    private static final DateTimeFormatter NO_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SafetyEventRepository eventRepository;
    private final SafetyEventLaneRepository eventLaneRepository;
    private final SafetyEventEquipmentRepository eventEquipmentRepository;
    private final SafetyEventRoundRepository eventRoundRepository;
    private final SafetyReviewRepository reviewRepository;
    private final SafetyEventLogRepository logRepository;
    private final LaneRepository laneRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoundRepository roundRepository;

    public SafetyService(SafetyEventRepository eventRepository,
            SafetyEventLaneRepository eventLaneRepository,
            SafetyEventEquipmentRepository eventEquipmentRepository,
            SafetyEventRoundRepository eventRoundRepository,
            SafetyReviewRepository reviewRepository,
            SafetyEventLogRepository logRepository,
            LaneRepository laneRepository,
            EquipmentRepository equipmentRepository,
            RoundRepository roundRepository) {
        this.eventRepository = eventRepository;
        this.eventLaneRepository = eventLaneRepository;
        this.eventEquipmentRepository = eventEquipmentRepository;
        this.eventRoundRepository = eventRoundRepository;
        this.reviewRepository = reviewRepository;
        this.logRepository = logRepository;
        this.laneRepository = laneRepository;
        this.equipmentRepository = equipmentRepository;
        this.roundRepository = roundRepository;
    }

    // ---------------- 查询 ----------------

    @Transactional(readOnly = true)
    public List<SafetyDtos.EventView> list(String status) {
        List<SafetyEvent> events = (status != null && !status.isBlank())
                ? eventRepository.findByStatusOrderByCreatedAtDesc(status)
                : eventRepository.findAllByOrderByCreatedAtDesc();
        return events.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public SafetyDtos.EventDetailView detail(Long id) {
        SafetyEvent event = require(id);
        List<SafetyEventLane> lanes = eventLaneRepository.findByEventIdOrderByIdAsc(id);
        List<SafetyEventEquipment> equips = eventEquipmentRepository.findByEventIdOrderByIdAsc(id);
        List<SafetyEventRound> rounds = eventRoundRepository.findByEventIdOrderByIdAsc(id);
        List<SafetyReview> reviews = reviewRepository.findByEventIdOrderByIdAsc(id);
        List<SafetyEventLog> logs = logRepository.findByEventIdOrderByIdAsc(id);

        Map<Long, Round> roundMap = rounds.isEmpty()
                ? Map.of()
                : roundRepository.findAllById(rounds.stream().map(SafetyEventRound::getRoundId).toList())
                        .stream().collect(Collectors.toMap(Round::getId, Function.identity()));

        List<SafetyDtos.RoundLockView> roundViews = new ArrayList<>();
        for (SafetyEventRound item : rounds) {
            Round round = roundMap.get(item.getRoundId());
            roundViews.add(new SafetyDtos.RoundLockView(
                    item.getRoundId(),
                    item.getRoundNo(),
                    round == null || round.getMember() == null ? "" : round.getMember().getName(),
                    round == null || round.getLane() == null ? "" : round.getLane().getLaneNo(),
                    item.getPrevStatus(),
                    RangeDict.roundStatusName(item.getPrevStatus()),
                    round == null ? 0 : round.getArrows().size(),
                    round == null ? 0 : round.getArrowCount(),
                    item.getRestored()));
        }

        return new SafetyDtos.EventDetailView(
                toView(event),
                lanes.stream().map(item -> new SafetyDtos.LaneLockView(
                        item.getLaneId(), item.getLaneNo(),
                        item.getPrevStatus(), RangeDict.laneStatusName(item.getPrevStatus()),
                        item.getRestored())).toList(),
                equips.stream().map(item -> new SafetyDtos.EquipLockView(
                        item.getEquipmentId(), item.getEquipCode(),
                        item.getPrevStatus(), RangeDict.equipStatusName(item.getPrevStatus()),
                        item.getPrevRenterName(), item.getRestored())).toList(),
                roundViews,
                reviews.stream().map(item -> new SafetyDtos.ReviewView(
                        item.getReviewType(), RangeDict.reviewTypeName(item.getReviewType()),
                        item.getConclusion(), RangeDict.conclusionName(item.getConclusion()),
                        item.getReviewerName(), item.getNote(), item.getReviewedAt())).toList(),
                logs.stream().map(item -> new SafetyDtos.LogView(
                        item.getAction(),
                        item.getFromStatus(), RangeDict.eventStatusName(item.getFromStatus()),
                        item.getToStatus(), RangeDict.eventStatusName(item.getToStatus()),
                        item.getOperatorName(),
                        item.getOperatorRole(), RangeDict.safetyRoleName(item.getOperatorRole()),
                        item.getDetail(), item.getCreatedAt())).toList());
    }

    // ---------------- 建立事件（立即联锁） ----------------

    @Transactional
    public SafetyDtos.EventDetailView create(SafetyDtos.EventCreateReq req) {
        requireRole(req.operatorRole(), "MANAGER", "只有值班经理能建立停射事件");
        String operator = requireName(req.operatorName());
        if (!RangeDict.isValidEventType(req.eventType())) {
            throw new BizException("事件类型只能是 箭道设备异常 / 人员闯入射线 / 疑似器材故障 / 其他安全隐患");
        }
        if (!RangeDict.isValidEventSeverity(req.severity())) {
            throw new BizException("严重级别只能是 一般 / 较重 / 紧急");
        }
        String reason = req.reason().trim();
        String description = req.description().trim();

        List<Long> laneIds = new ArrayList<>(new LinkedHashSet<>(
                req.laneIds() == null ? List.of() : req.laneIds()));
        List<Long> equipIds = new ArrayList<>(new LinkedHashSet<>(
                req.equipmentIds() == null ? List.of() : req.equipmentIds()));
        if (laneIds.isEmpty() && equipIds.isEmpty()) {
            throw new BizException("请至少选择一条受影响箭道或一件受影响器材");
        }

        List<Lane> lanes = laneIds.isEmpty() ? List.of() : laneRepository.findAllById(laneIds);
        if (lanes.size() != laneIds.size()) {
            throw new BizException("所选箭道中存在无效编号，请刷新后重试");
        }
        List<Equipment> equips = equipIds.isEmpty() ? List.of() : equipmentRepository.findAllById(equipIds);
        if (equips.size() != equipIds.size()) {
            throw new BizException("所选器材中存在无效编号，请刷新后重试");
        }
        // 同一资源只能被一个在办事件锁定，否则放行时无法判断该恢复到哪一版状态
        for (Lane lane : lanes) {
            if (lane.getLockEventId() != null) {
                throw new BizException("箭道 [" + lane.getLaneNo() + "] 已被其他停射事件锁定，不能重复纳入");
            }
        }
        for (Equipment equip : equips) {
            if (equip.getLockEventId() != null) {
                throw new BizException("器材 [" + equip.getEquipCode() + "] 已被其他停射事件锁定，不能重复纳入");
            }
        }

        // 受影响箭道上进行中的回合一并暂停（箭支与分数不动，只停状态）
        List<Round> rounds = laneIds.isEmpty()
                ? List.of()
                : roundRepository.findByLaneIdInAndStatus(laneIds, "ONGOING");
        for (Round round : rounds) {
            if (round.getLockEventId() != null) {
                throw new BizException("回合 [" + round.getRoundNo() + "] 已被其他停射事件暂停，不能重复纳入");
            }
        }

        SafetyEvent event = new SafetyEvent();
        event.setEventNo(nextEventNo());
        event.setEventType(req.eventType());
        event.setSeverity(req.severity());
        event.setReason(reason);
        event.setDescription(description);
        event.setStatus("PENDING");
        event.setCreatedBy(operator);
        event.setCreatedAt(LocalDateTime.now());
        event = eventRepository.save(event);
        final Long eventId = event.getId();

        for (Lane lane : lanes) {
            SafetyEventLane snapshot = new SafetyEventLane();
            snapshot.setEventId(eventId);
            snapshot.setLaneId(lane.getId());
            snapshot.setLaneNo(lane.getLaneNo());
            snapshot.setPrevStatus(lane.getStatus());
            snapshot.setRestored(false);
            eventLaneRepository.save(snapshot);
            lane.setStatus("LOCKED");
            lane.setLockEventId(eventId);
            laneRepository.save(lane);
        }
        for (Equipment equip : equips) {
            SafetyEventEquipment snapshot = new SafetyEventEquipment();
            snapshot.setEventId(eventId);
            snapshot.setEquipmentId(equip.getId());
            snapshot.setEquipCode(equip.getEquipCode());
            snapshot.setPrevStatus(equip.getStatus());
            snapshot.setPrevRenterId(equip.getRenterId());
            snapshot.setPrevRenterName(equip.getRenterName());
            snapshot.setPrevRentedAt(equip.getRentedAt());
            snapshot.setRestored(false);
            eventEquipmentRepository.save(snapshot);
            equip.setStatus("LOCKED");
            equip.setLockEventId(eventId);
            equipmentRepository.save(equip);
        }
        for (Round round : rounds) {
            SafetyEventRound snapshot = new SafetyEventRound();
            snapshot.setEventId(eventId);
            snapshot.setRoundId(round.getId());
            snapshot.setRoundNo(round.getRoundNo());
            snapshot.setPrevStatus(round.getStatus());
            snapshot.setRestored(false);
            eventRoundRepository.save(snapshot);
            round.setStatus("PAUSED");
            round.setLockEventId(eventId);
            roundRepository.save(round);
        }

        writeLog(eventId, "CREATE", null, "PENDING", operator, "MANAGER",
                "建立停射事件：锁定箭道 " + lanes.size() + " 条、器材 " + equips.size()
                        + " 件，暂停回合 " + rounds.size() + " 个");
        return detail(eventId);
    }

    // ---------------- 待处置 → 分项复核 ----------------

    @Transactional
    public SafetyDtos.EventDetailView advance(Long id, SafetyDtos.OperatorReq req) {
        requireRole(req.operatorRole(), "MANAGER", "只有值班经理能把事件转入分项复核");
        String operator = requireName(req.operatorName());
        SafetyEvent event = requireForUpdate(id);
        if (!"PENDING".equals(event.getStatus())) {
            throw new BizException("事件 [" + event.getEventNo() + "] 当前为「"
                    + RangeDict.eventStatusName(event.getStatus()) + "」，不能转入分项复核");
        }
        event.setStatus("REVIEWING");
        eventRepository.save(event);
        writeLog(id, "ADVANCE", "PENDING", "REVIEWING", operator, "MANAGER", "转入分项复核，等待教练与器材管理员确认");
        return detail(id);
    }

    // ---------------- 分项复核（每项只有一份最终结论） ----------------

    @Transactional
    public SafetyDtos.EventDetailView review(Long id, SafetyDtos.ReviewReq req) {
        String reviewer = requireName(req.operatorName());
        String reviewType;
        if ("COACH".equals(req.operatorRole())) {
            reviewType = "COACH";
        } else if ("KEEPER".equals(req.operatorRole())) {
            reviewType = "KEEPER";
        } else {
            throw new BizException("只有教练或器材管理员能执行分项复核");
        }
        if (!RangeDict.isValidConclusion(req.conclusion())) {
            throw new BizException("复核结论只能是 通过 / 不通过");
        }
        String note = req.note() == null ? "" : req.note().trim();
        if ("REJECT".equals(req.conclusion()) && note.isEmpty()) {
            throw new BizException("复核不通过时必须填写原因，事件将继续锁定");
        }

        SafetyEvent event = requireForUpdate(id);
        if (!"REVIEWING".equals(event.getStatus())) {
            throw new BizException("事件 [" + event.getEventNo() + "] 当前不在分项复核阶段，不能提交复核结论");
        }
        if (event.getCreatedBy().equals(reviewer)) {
            throw new BizException("事件创建人不能代替两个复核角色签字");
        }
        if (reviewRepository.existsByEventIdAndReviewType(id, reviewType)) {
            throw new BizException(RangeDict.reviewTypeName(reviewType) + "已有最终结论，不能重复提交");
        }

        SafetyReview review = new SafetyReview();
        review.setEventId(id);
        review.setReviewType(reviewType);
        review.setConclusion(req.conclusion());
        review.setReviewerName(reviewer);
        review.setNote(note.isEmpty() ? null : note);
        review.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(review);

        String detail = RangeDict.reviewTypeName(reviewType) + "结论：" + RangeDict.conclusionName(req.conclusion())
                + (note.isEmpty() ? "" : " · " + note);
        writeLog(id, "REVIEW_" + reviewType, "REVIEWING", "REVIEWING", reviewer, req.operatorRole(), detail);
        return detail(id);
    }

    // ---------------- 复射放行（一次性整体恢复） ----------------

    @Transactional
    public SafetyDtos.EventDetailView release(Long id, SafetyDtos.OperatorReq req) {
        requireRole(req.operatorRole(), "MANAGER", "只有值班经理能执行复射放行");
        String operator = requireName(req.operatorName());
        SafetyEvent event = requireForUpdate(id);
        if ("RELEASED".equals(event.getStatus())) {
            throw new BizException("事件 [" + event.getEventNo() + "] 已放行，请勿重复操作");
        }
        if (!"REVIEWING".equals(event.getStatus())) {
            throw new BizException("事件 [" + event.getEventNo() + "] 还未完成分项复核，不能放行");
        }

        List<SafetyReview> reviews = reviewRepository.findByEventIdOrderByIdAsc(id);
        SafetyReview coach = findReview(reviews, "COACH");
        SafetyReview keeper = findReview(reviews, "KEEPER");
        if (coach == null || keeper == null) {
            throw new BizException("教练复核与器材管理员复核都出具结论后才能放行");
        }
        if (!"PASS".equals(coach.getConclusion()) || !"PASS".equals(keeper.getConclusion())) {
            throw new BizException("存在复核不通过项，事件继续锁定，不能放行");
        }

        // 同一事务内按快照整体恢复：任何一步失败都会整体回滚，事件保持未放行，可安全重试
        List<SafetyEventLane> lanes = eventLaneRepository.findByEventIdOrderByIdAsc(id);
        for (SafetyEventLane snapshot : lanes) {
            Lane lane = laneRepository.findById(snapshot.getLaneId())
                    .orElseThrow(() -> new BizException("箭道不存在：id=" + snapshot.getLaneId()));
            lane.setStatus(snapshot.getPrevStatus());
            lane.setLockEventId(null);
            laneRepository.save(lane);
            snapshot.setRestored(true);
            eventLaneRepository.save(snapshot);
        }
        List<SafetyEventEquipment> equips = eventEquipmentRepository.findByEventIdOrderByIdAsc(id);
        for (SafetyEventEquipment snapshot : equips) {
            Equipment equip = equipmentRepository.findById(snapshot.getEquipmentId())
                    .orElseThrow(() -> new BizException("器材不存在：id=" + snapshot.getEquipmentId()));
            equip.setStatus(snapshot.getPrevStatus());
            equip.setRenterId(snapshot.getPrevRenterId());
            equip.setRenterName(snapshot.getPrevRenterName());
            equip.setRentedAt(snapshot.getPrevRentedAt());
            equip.setLockEventId(null);
            equipmentRepository.save(equip);
            snapshot.setRestored(true);
            eventEquipmentRepository.save(snapshot);
        }
        List<SafetyEventRound> rounds = eventRoundRepository.findByEventIdOrderByIdAsc(id);
        for (SafetyEventRound snapshot : rounds) {
            Round round = roundRepository.findById(snapshot.getRoundId())
                    .orElseThrow(() -> new BizException("回合不存在：id=" + snapshot.getRoundId()));
            round.setStatus(snapshot.getPrevStatus());
            round.setLockEventId(null);
            roundRepository.save(round);
            snapshot.setRestored(true);
            eventRoundRepository.save(snapshot);
        }

        event.setStatus("RELEASED");
        event.setReleasedBy(operator);
        event.setReleasedAt(LocalDateTime.now());
        eventRepository.save(event);
        writeLog(id, "RELEASE", "REVIEWING", "RELEASED", operator, "MANAGER",
                "复射放行：恢复箭道 " + lanes.size() + " 条、器材 " + equips.size()
                        + " 件、回合 " + rounds.size() + " 个至锁定前状态");
        return detail(id);
    }

    // ---------------- 内部工具 ----------------

    private SafetyEvent require(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new BizException("停射事件不存在：id=" + id));
    }

    private SafetyEvent requireForUpdate(Long id) {
        return eventRepository.findForUpdate(id)
                .orElseThrow(() -> new BizException("停射事件不存在：id=" + id));
    }

    private static SafetyReview findReview(List<SafetyReview> reviews, String type) {
        return reviews.stream().filter(item -> type.equals(item.getReviewType())).findFirst().orElse(null);
    }

    private static void requireRole(String role, String expected, String message) {
        if (!expected.equals(role)) {
            throw new BizException(message);
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BizException("请填写操作人姓名");
        }
        return name.trim();
    }

    private void writeLog(Long eventId, String action, String from, String to,
            String operator, String role, String detail) {
        SafetyEventLog log = new SafetyEventLog();
        log.setEventId(eventId);
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorName(operator);
        log.setOperatorRole(role);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
    }

    private String nextEventNo() {
        String no = "SE" + LocalDateTime.now().format(NO_STAMP);
        int seq = 1;
        while (eventRepository.existsByEventNo(no)) {
            no = "SE" + LocalDateTime.now().format(NO_STAMP) + seq;
            seq += 1;
        }
        return no;
    }

    private SafetyDtos.EventView toView(SafetyEvent event) {
        Long id = event.getId();
        return new SafetyDtos.EventView(
                id,
                event.getEventNo(),
                event.getEventType(),
                RangeDict.eventTypeName(event.getEventType()),
                event.getSeverity(),
                RangeDict.eventSeverityName(event.getSeverity()),
                event.getReason(),
                event.getDescription(),
                event.getStatus(),
                RangeDict.eventStatusName(event.getStatus()),
                event.getCreatedBy(),
                event.getCreatedAt(),
                event.getReleasedBy(),
                event.getReleasedAt(),
                eventLaneRepository.findByEventIdOrderByIdAsc(id).size(),
                eventEquipmentRepository.findByEventIdOrderByIdAsc(id).size(),
                eventRoundRepository.findByEventIdOrderByIdAsc(id).size());
    }
}
