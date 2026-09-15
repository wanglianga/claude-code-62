package com.funeral.controller;

import com.funeral.common.ApiResponse;
import com.funeral.entity.*;
import com.funeral.repo.FuneralOrderRepository;
import com.funeral.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final FuneralOrderRepository orderRepo;

    public OrderController(OrderService orderService, FuneralOrderRepository orderRepo) {
        this.orderService = orderService;
        this.orderRepo = orderRepo;
    }

    @GetMapping
    public ApiResponse<List<FuneralOrder>> list() {
        return ApiResponse.ok(orderRepo.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id));
    }

    @PostMapping
    public ApiResponse<FuneralOrder> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.createOrder(body));
    }

    // ---------- 资源核验 ----------
    @PostMapping("/{id}/verify")
    public ApiResponse<Map<String, Object>> verify(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.verifyResources(id, body));
    }

    // ---------- 订单条目 ----------
    @PostMapping("/{id}/items")
    public ApiResponse<OrderItem> addItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.addItem(id, body));
    }

    @PostMapping("/{id}/items/{itemId}/confirm")
    public ApiResponse<SignatureRecord> confirmItem(@PathVariable Long id, @PathVariable Long itemId,
                                                    @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.confirmItem(id, itemId, body));
    }

    @PostMapping("/{id}/items/{itemId}/remove")
    public ApiResponse<SignatureRecord> removeItem(@PathVariable Long id, @PathVariable Long itemId,
                                                   @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.removeItem(id, itemId, body));
    }

    @PostMapping("/{id}/confirm-plan")
    public ApiResponse<SignatureRecord> confirmPlan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.confirmPlan(id, body));
    }

    // ---------- 沟通记录 / 先记录后变更 ----------
    @PostMapping("/{id}/communications")
    public ApiResponse<CommunicationLog> addComm(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.addCommunicationLog(id, body));
    }

    @PostMapping("/{id}/change-resource")
    public ApiResponse<Map<String, Object>> changeResource(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.changeResourceAfterLog(id, body));
    }

    // ---------- 协同 ----------
    @PostMapping("/{id}/collaborations")
    public ApiResponse<CollaborationTask> createCollab(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.createCollab(id, body));
    }

    @PostMapping("/collaborations/{taskId}/resolve")
    public ApiResponse<CollaborationTask> resolveCollab(@PathVariable Long taskId,
                                                        @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.resolveCollab(taskId, body));
    }

    // ---------- 困难家庭减免（两级审核） ----------
    @PostMapping("/{id}/reduction/apply")
    public ApiResponse<ReductionApplication> applyReduction(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.applyReduction(id, body));
    }

    @PostMapping("/{id}/reduction/finance-review")
    public ApiResponse<ReductionApplication> financeReview(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.financeReviewReduction(id,
                Boolean.TRUE.equals(body.get("approved")), body));
    }

    @PostMapping("/{id}/reduction/leader-review")
    public ApiResponse<ReductionApplication> leaderReview(@PathVariable Long id,
                                                          @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.leaderReviewReduction(id,
                Boolean.TRUE.equals(body.get("approved")), body));
    }

    @GetMapping("/{id}/reduction/delta")
    public ApiResponse<Map<String, Object>> reductionDelta(@PathVariable Long id) {
        return ApiResponse.ok(orderService.reductionDelta(id));
    }

    // ---------- 审核期高价/额外仪式二次确认 ----------
    @PostMapping("/{id}/items/{itemId}/guard-confirm")
    public ApiResponse<SignatureRecord> guardConfirm(@PathVariable Long id, @PathVariable Long itemId,
                                                     @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.confirmGuardedItem(id, itemId, body));
    }

    // ---------- 服务执行 ----------
    @PostMapping("/{id}/start-service")
    public ApiResponse<FuneralOrder> start(@PathVariable Long id) {
        return ApiResponse.ok(orderService.startService(id));
    }

    @PostMapping("/{id}/complete-service")
    public ApiResponse<FuneralOrder> complete(@PathVariable Long id,
                                              @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(orderService.completeService(id, body == null ? Map.of() : body));
    }

    // ---------- 费用单 / 结算 ----------
    @GetMapping("/{id}/bill")
    public ApiResponse<Map<String, Object>> bill(@PathVariable Long id) {
        return ApiResponse.ok(orderService.bill(id));
    }

    @PostMapping("/{id}/confirm-bill")
    public ApiResponse<SignatureRecord> confirmBill(@PathVariable Long id,
                                                    @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.confirmBill(id, body));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<Map<String, Object>> pay(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.pay(id, body));
    }

    // ---------- 骨灰领取 / 反馈 / 归档 ----------
    @PostMapping("/{id}/urn-claim")
    public ApiResponse<ServiceArchive> urnClaim(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.urnClaim(id, body));
    }

    @PostMapping("/{id}/feedback")
    public ApiResponse<ServiceArchive> feedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderService.feedback(id, body));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<ServiceArchive> archive(@PathVariable Long id,
                                               @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(orderService.archive(id, body == null ? Map.of() : body));
    }
}
