package com.archery.range.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.archery.range.common.ApiResponse;
import com.archery.range.domain.RangeDict;
import com.archery.range.dto.SafetyDtos;
import com.archery.range.service.SafetyService;

import jakarta.validation.Valid;

/**
 * 模块六：安全停射联锁台。
 *
 * 值班经理建立停射事件 → 受影响箭道/器材立即安全锁定、进行中回合暂停 →
 * 教练与器材管理员分项复核（各一份最终结论，创建人不能代签）→
 * 两项都通过后值班经理复射放行，资源在同一事务里整体恢复。
 */
@RestController
@RequestMapping("/api")
public class SafetyController {

    private final SafetyService safetyService;

    public SafetyController(SafetyService safetyService) {
        this.safetyService = safetyService;
    }

    @GetMapping("/safety/events")
    public ApiResponse<List<SafetyDtos.EventView>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(safetyService.list(status));
    }

    /** 事件详情：受影响资源、每项复核人和时间、完整状态轨迹 */
    @GetMapping("/safety/events/{id}")
    public ApiResponse<SafetyDtos.EventDetailView> detail(@PathVariable Long id) {
        return ApiResponse.success(safetyService.detail(id));
    }

    /** 建立停射事件（值班经理），立即联锁 */
    @PostMapping("/safety/events")
    public ApiResponse<SafetyDtos.EventDetailView> create(@Valid @RequestBody SafetyDtos.EventCreateReq req) {
        return ApiResponse.success("停射事件已建立，受影响资源已安全锁定", safetyService.create(req));
    }

    /** 待处置 → 分项复核 */
    @PostMapping("/safety/events/{id}/advance")
    public ApiResponse<SafetyDtos.EventDetailView> advance(@PathVariable Long id,
            @Valid @RequestBody SafetyDtos.OperatorReq req) {
        return ApiResponse.success("已转入分项复核", safetyService.advance(id, req));
    }

    /** 提交分项复核结论（教练 / 器材管理员，每项一份最终结论） */
    @PostMapping("/safety/events/{id}/reviews")
    public ApiResponse<SafetyDtos.EventDetailView> review(@PathVariable Long id,
            @Valid @RequestBody SafetyDtos.ReviewReq req) {
        return ApiResponse.success("复核结论已记录", safetyService.review(id, req));
    }

    /** 复射放行（值班经理，两项复核都通过后；整体恢复，只成功一次） */
    @PostMapping("/safety/events/{id}/release")
    public ApiResponse<SafetyDtos.EventDetailView> release(@PathVariable Long id,
            @Valid @RequestBody SafetyDtos.OperatorReq req) {
        return ApiResponse.success("已复射放行，受影响资源恢复至锁定前状态", safetyService.release(id, req));
    }

    /** 联锁台词典：事件类型 / 严重级别 / 角色 / 复核项 */
    @GetMapping("/safety/options")
    public ApiResponse<Map<String, Object>> options() {
        Map<String, Object> options = new java.util.LinkedHashMap<>();
        options.put("eventTypes", List.of(
                Map.of("code", "LANE_FAULT", "name", RangeDict.eventTypeName("LANE_FAULT")),
                Map.of("code", "INTRUSION", "name", RangeDict.eventTypeName("INTRUSION")),
                Map.of("code", "EQUIP_FAULT", "name", RangeDict.eventTypeName("EQUIP_FAULT")),
                Map.of("code", "OTHER", "name", RangeDict.eventTypeName("OTHER"))));
        options.put("severities", List.of(
                Map.of("code", "LOW", "name", RangeDict.eventSeverityName("LOW")),
                Map.of("code", "MEDIUM", "name", RangeDict.eventSeverityName("MEDIUM")),
                Map.of("code", "HIGH", "name", RangeDict.eventSeverityName("HIGH"))));
        options.put("roles", List.of(
                Map.of("code", "MANAGER", "name", RangeDict.safetyRoleName("MANAGER")),
                Map.of("code", "COACH", "name", RangeDict.safetyRoleName("COACH")),
                Map.of("code", "KEEPER", "name", RangeDict.safetyRoleName("KEEPER"))));
        options.put("reviewTypes", List.of(
                Map.of("code", "COACH", "name", RangeDict.reviewTypeName("COACH")),
                Map.of("code", "KEEPER", "name", RangeDict.reviewTypeName("KEEPER"))));
        return ApiResponse.success(options);
    }
}
