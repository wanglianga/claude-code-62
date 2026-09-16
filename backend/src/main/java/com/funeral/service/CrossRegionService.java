package com.funeral.service;

import com.funeral.common.BusinessException;
import com.funeral.common.Constants;
import com.funeral.common.CurrentUser;
import com.funeral.entity.*;
import com.funeral.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CrossRegionService {

    private final CrossRegionTransportRepository crossRepo;
    private final FuneralOrderRepository orderRepo;
    private final ResourceRepository resourceRepo;
    private final ResourceBookingRepository bookingRepo;
    private final NotificationRepository notifRepo;
    private final CollaborationTaskRepository collabRepo;
    private final TimelineService timeline;

    public CrossRegionService(CrossRegionTransportRepository crossRepo, FuneralOrderRepository orderRepo,
                              ResourceRepository resourceRepo, ResourceBookingRepository bookingRepo,
                              NotificationRepository notifRepo, CollaborationTaskRepository collabRepo,
                              TimelineService timeline) {
        this.crossRepo = crossRepo;
        this.orderRepo = orderRepo;
        this.resourceRepo = resourceRepo;
        this.bookingRepo = bookingRepo;
        this.notifRepo = notifRepo;
        this.collabRepo = collabRepo;
        this.timeline = timeline;
    }

    // ---------------- 登记 ----------------

    @Transactional
    public CrossRegionTransport register(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustOrder(orderId);
        CrossRegionTransport c = crossRepo.findByOrderId(orderId).orElseGet(CrossRegionTransport::new);
        c.setOrderId(orderId);
        c.setOrderNo(o.getOrderNo());
        c.setDeathPlace(str(body, "deathPlace", "请填写死亡地"));
        c.setPickupAddress(str(body, "pickupAddress", "请填写跨县接运地点"));
        c.setLocalOrgName((String) body.get("localOrgName"));
        c.setLocalContactName((String) body.get("localContactName"));
        c.setLocalContactPhone((String) body.get("localContactPhone"));
        c.setCertIssuingOrg(str(body, "certIssuingOrg", "请填写死亡证明出具机构"));
        c.setTransportPermitNo((String) body.get("transportPermitNo"));
        c.setDriverName((String) body.get("driverName"));
        c.setDriverPhone((String) body.get("driverPhone"));
        c.setEstimatedArrivalAt(OrderService.parseTimeStatic(body.get("estimatedArrivalAt")));
        c.setEmbalmingRequired(Boolean.TRUE.equals(body.get("embalmingRequired")));
        c.setColdConditionNote((String) body.get("coldConditionNote"));
        c.setReligiousCustom((String) body.getOrDefault("religiousCustom", o.getReligiousCustom()));
        c.setAccompanyingRelatives((String) body.get("accompanyingRelatives"));
        if (body.get("vehicleId") != null) {
            Long vid = Long.valueOf(String.valueOf(body.get("vehicleId")));
            Resource v = resourceRepo.findById(vid).orElseThrow(() -> new BusinessException("车辆不存在"));
            c.setVehicleId(v.getId());
            c.setVehicleName(v.getName());
        }
        crossRepo.save(c);

        // 同步订单关键字段
        o.setFromOtherCity(true);
        if (body.get("pickupAddress") != null) o.setPickupAddress((String) body.get("pickupAddress"));
        if (c.getReligiousCustom() != null) o.setReligiousCustom(c.getReligiousCustom());
        o.setNeedRefrigeration(c.getEmbalmingRequired() || Boolean.TRUE.equals(o.getNeedRefrigeration()));
        orderRepo.save(o);

        createCollabIfAbsent(o, "NONLOCAL", "外地/跨县接运协同",
                "死亡地：" + c.getDeathPlace() + "；接运地点：" + c.getPickupAddress()
                        + "；当地联系人：" + nz(c.getLocalContactName()) + " " + nz(c.getLocalContactPhone())
                        + "；预计到达：" + c.getEstimatedArrivalAt(),
                Constants.ROLE_TRANSPORT, 1);
        timeline.add(orderId, "CROSS_REGION", "登记跨县接运：死亡地 " + c.getDeathPlace()
                + "，证明出具机构 " + c.getCertIssuingOrg() + "，预计到馆 " + c.getEstimatedArrivalAt());
        return c;
    }

    // ---------------- 六项核验 ----------------

    @Transactional
    public Map<String, Object> verify(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustOrder(orderId);
        CrossRegionTransport c = crossRepo.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("请先登记跨县接运信息"));

        if (body.get("vehicleId") != null) {
            Resource v = resourceRepo.findById(Long.valueOf(String.valueOf(body.get("vehicleId"))))
                    .orElseThrow(() -> new BusinessException("车辆不存在"));
            c.setVehicleId(v.getId());
            c.setVehicleName(v.getName());
        }
        if (body.get("driverName") != null) c.setDriverName((String) body.get("driverName"));
        if (body.get("driverPhone") != null) c.setDriverPhone((String) body.get("driverPhone"));
        if (body.get("transportPermitNo") != null) c.setTransportPermitNo((String) body.get("transportPermitNo"));
        LocalDateTime eta = body.get("estimatedArrivalAt") != null
                ? OrderService.parseTimeStatic(body.get("estimatedArrivalAt")) : c.getEstimatedArrivalAt();
        if (eta != null) c.setEstimatedArrivalAt(eta);
        LocalDateTime farewell = body.get("farewellTime") != null
                ? OrderService.parseTimeStatic(body.get("farewellTime")) : o.getFarewellTime();
        LocalDateTime cremation = body.get("cremationTime") != null
                ? OrderService.parseTimeStatic(body.get("cremationTime"))
                : (farewell != null ? farewell.plusHours(2) : null);
        Long coldId = body.get("coldId") == null ? null : Long.valueOf(String.valueOf(body.get("coldId")));
        Long hallId = body.get("hallId") == null ? null : Long.valueOf(String.valueOf(body.get("hallId")));
        Long furnaceId = body.get("furnaceId") == null ? null : Long.valueOf(String.valueOf(body.get("furnaceId")));

        List<Map<String, Object>> results = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        // 1) 死亡证明
        boolean certOk = "VERIFIED".equals(o.getCertificateStatus())
                || "VERIFIED".equals(String.valueOf(body.getOrDefault("certificateStatus", o.getCertificateStatus())));
        if (certOk) {
            o.setCertificateStatus("VERIFIED");
            if (body.get("certificateNo") != null) o.setCertificateNo((String) body.get("certificateNo"));
            c.setCertVerified(true);
            results.add(ok("CERT", "死亡证明已核验，出具机构：" + nz(c.getCertIssuingOrg())));
        } else {
            c.setCertVerified(false);
            results.add(fail("CERT", "死亡证明缺失或未核验（出具机构：" + nz(c.getCertIssuingOrg()) + "），需联系人补材料"));
            failures.add("DOC_MISSING");
        }

        // 2) 接运许可 + 3) 车辆资质
        Resource vehicle = c.getVehicleId() == null ? null
                : resourceRepo.findById(c.getVehicleId()).orElse(null);
        boolean permitOk = c.getTransportPermitNo() != null && !c.getTransportPermitNo().isBlank();
        c.setPermitStatus(permitOk ? "VERIFIED" : "MISSING");
        c.setPermitVerified(permitOk);
        results.add(permitOk
                ? ok("PERMIT", "跨县接运许可已登记：" + c.getTransportPermitNo())
                : fail("PERMIT", "缺少跨县接运许可编号"));
        if (!permitOk) failures.add("PERMIT_MISSING");

        boolean vehicleOk = false;
        if (vehicle == null) {
            results.add(fail("VEHICLE", "未指定接运车辆"));
        } else if (!Boolean.TRUE.equals(vehicle.getAvailable())) {
            results.add(fail("VEHICLE", vehicle.getName() + " 已停用：" + nz(vehicle.getNote())));
        } else if (!Boolean.TRUE.equals(vehicle.getQualified())
                || vehicle.getPermitNo() == null || vehicle.getPermitNo().isBlank()) {
            results.add(fail("VEHICLE", vehicle.getName() + " 跨县营运资质/接运许可不齐全，不能承担跨县接运"));
        } else if (eta != null && !bookingRepo.findConflicts("VEHICLE", vehicle.getId(),
                eta.minusHours(6), eta, orderId).isEmpty()) {
            results.add(fail("VEHICLE", vehicle.getName() + " 接运时段与其他任务冲突"));
        } else {
            vehicleOk = true;
            c.setVehicleVerified(true);
            results.add(ok("VEHICLE", "车辆资质有效（营运/许可 " + vehicle.getPermitNo() + "），司机 "
                    + nz(c.getDriverName()) + " " + nz(c.getDriverPhone())));
        }
        if (!vehicleOk) failures.add("VEHICLE_ISSUE");

        // 4) 防腐冷藏条件 + 5) 馆内接收能力（需要冷藏时）
        boolean needCold = Boolean.TRUE.equals(c.getEmbalmingRequired()) || Boolean.TRUE.equals(o.getNeedRefrigeration());
        Resource cold = null;
        if (needCold) {
            if (coldId != null) cold = resourceRepo.findById(coldId).orElse(null);
            if (cold == null) {
                // 自动寻找 ETA 后空闲的冷藏位作为接收能力判断
                LocalDateTime searchStart = eta != null ? eta : LocalDateTime.now();
                List<Long> busy = bookingRepo.findBusyOfType("COLD", searchStart,
                        farewell != null ? farewell : searchStart.plusHours(48)).stream()
                        .filter(b -> !"SUSPENDED".equals(b.getStatus()))
                        .map(ResourceBooking::getResourceId).toList();
                cold = resourceRepo.findByTypeOrderByIdAsc("COLD").stream()
                        .filter(r -> Boolean.TRUE.equals(r.getAvailable()) && !busy.contains(r.getId()))
                        .findFirst().orElse(null);
                if (cold != null) coldId = cold.getId();
            }
            if (cold == null || !Boolean.TRUE.equals(cold.getAvailable())) {
                results.add(fail("COLD", "馆内冷藏位不足，暂不具备接收条件，请协调改期"));
                c.setColdConditionVerified(false);
                c.setReceptionCapacityVerified(false);
                failures.add("COLD_SHORTAGE");
            } else {
                LocalDateTime coldEnd = farewell != null ? farewell : (eta != null ? eta.plusHours(48) : LocalDateTime.now().plusHours(48));
                LocalDateTime coldStart = eta != null ? eta : LocalDateTime.now();
                List<ResourceBooking> conflicts = bookingRepo.findConflicts("COLD", cold.getId(), coldStart, coldEnd, orderId)
                        .stream().filter(b -> !"SUSPENDED".equals(b.getStatus())).toList();
                if (!conflicts.isEmpty()) {
                    results.add(fail("COLD", cold.getName() + " 在预计到馆时段已被占用，接收能力不足"));
                    c.setReceptionCapacityVerified(false);
                    failures.add("COLD_SHORTAGE");
                } else {
                    c.setColdConditionVerified(true);
                    c.setReceptionCapacityVerified(true);
                    results.add(ok("COLD", "冷藏条件具备：" + cold.getName() + "，温度达标可入库"));
                }
            }
        } else {
            results.add(skip("COLD", "无需冷藏（未申请防腐/冷藏）"));
            c.setColdConditionVerified(true);
            c.setReceptionCapacityVerified(true);
        }

        // 6) 火化排期与礼厅（关联排期）
        boolean hallOk = checkScheduleResource(results, "HALL", hallId,
                farewell == null ? null : farewell.minusMinutes(30),
                farewell == null ? null : farewell.plusHours(2), "告别厅", orderId);
        boolean furnaceOk = checkScheduleResource(results, "FURNACE", furnaceId,
                cremation == null ? null : cremation.minusMinutes(30),
                cremation == null ? null : cremation.plusMinutes(60), "火化设备", orderId);
        c.setScheduleVerified(hallOk && furnaceOk);
        if (!hallOk) failures.add("HALL_CONFLICT");
        if (!furnaceOk) failures.add("CREMATION_CONFLICT");

        boolean allPass = failures.isEmpty() && eta != null && vehicleOk;
        crossRepo.save(c);
        orderRepo.save(o);

        if (allPass) {
            // 锁定资源（先释放/解除本单旧锁定）
            ResourceBooking vb = lockBooking(orderId, "VEHICLE", vehicle,
                    eta.minusHours(6), eta, c.getDriverName(), c.getDriverPhone(),
                    c.getTransportPermitNo(), eta);
            if (needCold && cold != null) {
                ResourceBooking cb = lockBooking(orderId, "COLD", cold,
                        eta, farewell != null ? farewell : eta.plusHours(48), null, null, null, eta);
                c.setColdBookingId(cb.getId());
            }
            if (hallId != null) {
                Resource hb = resourceRepo.findById(hallId).orElseThrow();
                ResourceBooking b = lockBooking(orderId, "HALL", hb,
                        farewell.minusMinutes(30), farewell.plusHours(2), null, null, null, eta);
                c.setHallBookingId(b.getId());
                o.setFarewellTime(farewell);
            }
            if (furnaceId != null) {
                Resource fb2 = resourceRepo.findById(furnaceId).orElseThrow();
                ResourceBooking b = lockBooking(orderId, "FURNACE", fb2,
                        cremation.minusMinutes(30), cremation.plusMinutes(60), null, null, null, eta);
                c.setFurnaceBookingId(b.getId());
            }
            c.setStatus("PLANNED");
            c.setSuspendReason(null);
            crossRepo.save(c);
            // 跨区六项核验是五要素核验的超集，同步主单核验结论（避免重复核验产生重复占用）
            o.setVerifyStatus("PASS");
            o.setVerifyNote("跨县接运六项核验通过（死亡证明/接运许可/车辆资质/冷藏条件/接收能力/火化礼厅排期）");
            orderRepo.save(o);
            orderRepo.save(o);
            // 仅关闭本次核验中已恢复/补齐的异常协同；无关或仍未解决的协同保持开放
            boolean coldOk = !needCold || (Boolean.TRUE.equals(c.getColdConditionVerified())
                    && Boolean.TRUE.equals(c.getReceptionCapacityVerified()));
            Map<String, Boolean> resolved = new LinkedHashMap<>();
            resolved.put("DOC_MISSING", certOk);
            resolved.put("PERMIT_MISSING", permitOk);
            resolved.put("VEHICLE_ISSUE", vehicleOk);
            resolved.put("COLD_SHORTAGE", coldOk);
            resolved.put("HALL_CONFLICT", hallOk);
            resolved.put("CREMATION_CONFLICT", furnaceOk);
            resolveCollabByTypes(orderId, resolved,
                    "跨县接运重新核验通过，该项已补齐/恢复");
            timeline.add(orderId, "CROSS_REGION", "跨县接运六项核验通过，车辆/冷藏/礼厅/火化时段已锁定"
                    + "，已恢复事项的协同同步办结");
        } else {
            // 异常：暂停相关资源锁定，通知主要联系人补材料或改期，发起跨岗位协同
            suspendByFailures(orderId, failures, o, c, eta);
            c.setStatus("SUSPENDED");
            c.setSuspendReason("核验未通过：" + String.join("、", failures));
            crossRepo.save(c);
            timeline.add(orderId, "CROSS_REGION", "跨县接运核验未通过，已暂停相关资源锁定并通知联系人："
                    + String.join("、", failures));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("pass", allPass);
        resp.put("results", results);
        resp.put("failures", failures);
        resp.put("transport", c);
        return resp;
    }

    private boolean checkScheduleResource(List<Map<String, Object>> results, String type, Long rid,
                                          LocalDateTime start, LocalDateTime end, String label, Long orderId) {
        if (rid == null || start == null) {
            results.add(fail(type, label + "：未安排（跨县接运须关联礼厅与火化排期）"));
            return false;
        }
        Resource r = resourceRepo.findById(rid).orElse(null);
        if (r == null) { results.add(fail(type, label + "：资源不存在")); return false; }
        if (!Boolean.TRUE.equals(r.getAvailable())) {
            results.add(fail(type, label + " " + r.getName() + " 停用/检修：" + nz(r.getNote())));
            return false;
        }
        List<ResourceBooking> conflicts = bookingRepo.findConflicts(type, rid, start, end, orderId)
                .stream().filter(b -> !"SUSPENDED".equals(b.getStatus())).toList();
        if (!conflicts.isEmpty()) {
            results.add(fail(type, label + " " + r.getName() + " 排期冲突，被治丧单 "
                    + conflicts.stream().map(b -> "#" + b.getOrderId()).toList() + " 占用"));
            return false;
        }
        results.add(ok(type, label + " " + r.getName() + " 排期可用，已具备锁定条件"));
        return true;
    }

    /**
     * 按异常类型精确闭环协同：只关闭 map 中为 true（本次确已补齐/恢复）的类型，
     * 其他未解决协同保持开放。保留处理依据、核验人和完成时间。
     */
    private void resolveCollabByTypes(Long orderId, Map<String, Boolean> types, String basis) {
        List<CollaborationTask> tasks = collabRepo.findByOrderIdOrderByPriorityAscCreatedAtDesc(orderId);
        for (var entry : types.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) continue;
            for (CollaborationTask t : tasks) {
                if (entry.getKey().equals(t.getType()) && !"RESOLVED".equals(t.getStatus())) {
                    t.setStatus("RESOLVED");
                    t.setAssigneeId(CurrentUser.id());
                    t.setAssigneeName(CurrentUser.name());
                    t.setResolution(basis + "；处理/核验人：" + CurrentUser.name()
                            + "，完成时间 " + LocalDateTime.now());
                    t.setResolvedAt(LocalDateTime.now());
                    collabRepo.save(t);
                    timeline.addAsSystem(orderId, "COLLAB",
                            "异常协同【" + t.getTitle() + "】已办结：" + basis);
                }
            }
        }
    }

    /** 车辆延误：已闭环的排期类协同需要重新产生待办（旧办结记录保留，新待办表达当前异常） */
    private void reopenScheduleCollabs(FuneralOrder o, boolean needCold, LocalDateTime newEta) {
        createCollabIfAbsent(o, "VEHICLE_DELAY", "跨县接运车辆延误",
                "车辆延误，新预计到馆 " + newEta + "，请接运组持续跟踪。", Constants.ROLE_TRANSPORT, 1);
        createCollabIfAbsent(o, "HALL_CONFLICT", "车辆延误：告别厅排期待重新确认",
                "到馆时间改为 " + newEta + "，待到馆后重新确认礼厅时段。", Constants.ROLE_HALL_ADMIN, 1);
        createCollabIfAbsent(o, "CREMATION_CONFLICT", "车辆延误：火化排期待重新确认",
                "到馆时间改为 " + newEta + "，请火化组待到馆后重新确认火化时段。", Constants.ROLE_CREMATORIUM, 1);
        if (needCold) {
            createCollabIfAbsent(o, "COLD_SHORTAGE", "车辆延误：冷藏入库时段待重新确认",
                    "新预计到馆 " + newEta + "，请礼厅管理员预留冷柜。", Constants.ROLE_HALL_ADMIN, 1);
        }
    }

    private void suspendByFailures(Long orderId, List<String> failures, FuneralOrder o,
                                   CrossRegionTransport c, LocalDateTime eta) {
        // 暂停受影响类型的现有锁定（不删除，便于恢复/追溯）
        Set<String> affectedSet = new HashSet<>();
        String joined = String.join(",", failures);
        if (joined.contains("VEHICLE_ISSUE") || joined.contains("PERMIT_MISSING")) {
            affectedSet.addAll(Arrays.asList("VEHICLE", "COLD", "HALL", "FURNACE"));
        } else if (joined.contains("COLD_SHORTAGE")) {
            affectedSet.addAll(Arrays.asList("COLD", "HALL", "FURNACE"));
        } else {
            affectedSet.addAll(Arrays.asList("HALL", "FURNACE"));
        }
        for (ResourceBooking b : bookingRepo.findByOrderId(orderId)) {
            if (affectedSet.contains(b.getResourceType())
                    && List.of("HELD", "CONFIRMED").contains(b.getStatus())) {
                b.setStatus("SUSPENDED");
                b.setNote("跨县接运核验异常暂停锁定：" + String.join("、", failures));
                bookingRepo.save(b);
            }
        }
        // 通知主要联系人
        String reasonText = failureText(failures);
        Notification n = new Notification();
        n.setOrderId(orderId);
        n.setType(failures.contains("DOC_MISSING") ? "DOC_MISSING"
                : failures.contains("VEHICLE_ISSUE") ? "VEHICLE_DELAY"
                : failures.contains("COLD_SHORTAGE") ? "COLD_SHORTAGE" : "RESCHEDULE");
        n.setChannel("PHONE");
        n.setTargetName(nz(o.getContactName()));
        n.setTargetPhone(nz(o.getContactPhone()));
        n.setTitle("跨县接运需要补充材料/改期");
        n.setContent("您的亲属由" + nz(c.getDeathPlace()) + "接运事宜：" + reasonText
                + "。请尽快补充材料或与业务员联系改期，预计到馆时间：" + eta + "。");
        n.setSentById(CurrentUser.id());
        n.setSentByName(CurrentUser.name());
        notifRepo.save(n);

        // 跨岗位协同
        for (String f : new LinkedHashSet<>(failures)) {
            switch (f) {
                case "DOC_MISSING" -> createCollabIfAbsent(o, "DOC_MISSING", "异地死亡证明待补充",
                        "死亡证明出具机构：" + nz(c.getCertIssuingOrg()) + "；已通知联系人 " + n.getTargetName(),
                        Constants.ROLE_CLERK, 1);
                case "PERMIT_MISSING" -> createCollabIfAbsent(o, "PERMIT_MISSING",
                        "跨县接运许可待补", "请接运组补齐跨县接运/准运许可后重新核验。",
                        Constants.ROLE_TRANSPORT, 1);
                case "VEHICLE_ISSUE" -> createCollabIfAbsent(o, "VEHICLE_ISSUE",
                        "跨县接运车辆资质异常", "请接运组核对车辆营运资质与排班，必要时更换具备资质的长程车辆。",
                        Constants.ROLE_TRANSPORT, 1);
                case "COLD_SHORTAGE" -> createCollabIfAbsent(o, "COLD_SHORTAGE", "馆内冷藏位不足",
                        "预计到馆 " + eta + " 冷藏位紧张，请礼厅管理员统筹或通知家属改期。",
                        Constants.ROLE_HALL_ADMIN, 1);
                case "HALL_CONFLICT" -> createCollabIfAbsent(o, "HALL_CONFLICT", "跨县接运告别厅排期冲突",
                        "异地到馆时间存在不确定性，请礼厅管理员协调礼厅时段。", Constants.ROLE_HALL_ADMIN, 1);
                case "CREMATION_CONFLICT" -> createCollabIfAbsent(o, "CREMATION_CONFLICT", "火化排期冲突待协调",
                        "请火化组核对炉况与排期，必要时调整火化时段或更换火化炉。", Constants.ROLE_CREMATORIUM, 1);
                default -> { }
            }
        }
    }

    // ---------------- 在途 / 延误 ----------------

    @Transactional
    public CrossRegionTransport depart(Long orderId, Map<String, Object> body) {
        CrossRegionTransport c = mustCross(orderId);
        if (!c.getCertVerified() || !c.getVehicleVerified() || !c.getScheduleVerified()) {
            throw new BusinessException("六项核验未全部通过，不能发车；如已解决异常请重新核验");
        }
        c.setStatus("IN_TRANSIT");
        c.setDepartedAt(LocalDateTime.now());
        if (body.get("estimatedArrivalAt") != null)
            c.setEstimatedArrivalAt(OrderService.parseTimeStatic(body.get("estimatedArrivalAt")));
        crossRepo.save(c);
        timeline.add(orderId, "CROSS_REGION", "跨县接运车辆已出发，司机 " + nz(c.getDriverName())
                + "，预计到馆 " + c.getEstimatedArrivalAt());
        return c;
    }

    @Transactional
    public CrossRegionTransport reportDelay(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustOrder(orderId);
        CrossRegionTransport c = mustCross(orderId);
        if (c.getDepartedAt() == null) {
            throw new BusinessException("车辆尚未发车（或六项核验未完成），不能上报延误；请先完成核验并发车");
        }
        LocalDateTime newEta = OrderService.parseTimeStatic(body.get("estimatedArrivalAt"));
        c.setEstimatedArrivalAt(newEta);
        c.setStatus("SUSPENDED");
        String reason = String.valueOf(body.getOrDefault("reason", "车辆/道路原因延误"));
        c.setSuspendReason("车辆延误：" + reason + "；新预计到馆 " + newEta);
        crossRepo.save(c);
        // 到馆后连锁排期暂停锁定
        for (ResourceBooking b : bookingRepo.findByOrderId(orderId)) {
            if (List.of("COLD", "HALL", "FURNACE").contains(b.getResourceType())
                    && "CONFIRMED".equals(b.getStatus())) {
                b.setStatus("SUSPENDED");
                b.setNote("接运车辆延误，排期锁定暂停，待到馆后重新确认");
                bookingRepo.save(b);
            }
        }
        Notification n = new Notification();
        n.setOrderId(orderId);
        n.setType("VEHICLE_DELAY");
        n.setTargetName(nz(o.getContactName()));
        n.setTargetPhone(nz(o.getContactPhone()));
        n.setTitle("接运车辆延误，告别/火化时间待改期确认");
        n.setContent("接运车辆延误：" + reason + "；新预计到馆时间 " + newEta
                + "。礼厅与火化排期暂为保留待确认状态，请与业务员联系确认新时间。");
        n.setSentById(CurrentUser.id());
        n.setSentByName(CurrentUser.name());
        notifRepo.save(n);
        // 已办结的排期类协同按延误重新挂起（历史办结记录保留），车辆延误协同派接运组
        boolean needCold = Boolean.TRUE.equals(c.getEmbalmingRequired()) || Boolean.TRUE.equals(o.getNeedRefrigeration());
        reopenScheduleCollabs(o, needCold, newEta);
        timeline.add(orderId, "CROSS_REGION", "接运车辆延误，已暂停冷藏/礼厅/火化锁定、通知联系人改期，排期协同重新挂起");
        return c;
    }

    // ---------------- 到馆回写 ----------------

    @Transactional
    public CrossRegionTransport arrive(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustOrder(orderId);
        CrossRegionTransport c = mustCross(orderId);
        requireArrivable(c);

        LocalDateTime now = LocalDateTime.now();
        c.setStatus("ARRIVED");
        c.setArrivedAt(now);
        c.setReceiverName(str(body, "receiverName", "请填写到馆接收人"));
        c.setHandoverNote((String) body.getOrDefault("handoverNote", "车辆、遗体、证明材料现场交接无误"));
        c.setColdStorageNote((String) body.get("coldStorageNote"));
        c.setCertReverifyNote((String) body.get("certReverifyNote"));
        c.setScheduleNote((String) body.get("scheduleNote"));

        // 车辆交接：车辆任务结束释放
        bookingRepo.findByOrderId(orderId).stream()
                .filter(b -> "VEHICLE".equals(b.getResourceType()) && !"RELEASED".equals(b.getStatus()))
                .forEach(b -> {
                    b.setStatus("RELEASED");
                    b.setNote("到馆车辆交接完成：" + c.getHandoverNote());
                    bookingRepo.save(b);
                });

        // 先解析重新确认的礼厅/火化排期（延误改期后冷藏终点跟随新告别时间）
        LocalDateTime farewell = body.get("farewellTime") != null
                ? OrderService.parseTimeStatic(body.get("farewellTime")) : o.getFarewellTime();
        LocalDateTime cremation = body.get("cremationTime") != null
                ? OrderService.parseTimeStatic(body.get("cremationTime"))
                : (farewell != null ? farewell.plusHours(2) : null);
        if (farewell != null) o.setFarewellTime(farewell);

        // 冷藏入库：占用与计费起点一律使用【实际入库时刻 now】，
        // ETA 只是调度预测（延误提前/推迟到馆时不再作为起点），避免出现"未来开始的空档"；
        // 终点为重新确认的告别排期
        boolean needCold = Boolean.TRUE.equals(c.getEmbalmingRequired()) || Boolean.TRUE.equals(o.getNeedRefrigeration());
        boolean coldReconfirmed = false;
        if (needCold) {
            c.setColdStoredAt(now);
            reactivateOrCreate(orderId, "COLD", c.getColdBookingId(), body.get("coldId"),
                    now, farewell != null ? farewell : now.plusHours(48));
            coldReconfirmed = true;
            if (c.getColdStorageNote() == null) c.setColdStorageNote("遗体已于实际到馆时冷藏入库，温度记录正常");
        }

        // 死亡证明到馆复核
        c.setCertReverifiedAt(now);
        o.setCertificateStatus("VERIFIED");
        c.setCertVerified(true);
        if (body.get("certificateNo") != null) o.setCertificateNo((String) body.get("certificateNo"));

        // 礼厅/火化按重新确认的服务时段恢复锁定（不被实际入库时间覆盖）
        boolean hallReconfirmed = false;
        boolean furnaceReconfirmed = false;
        if (farewell != null) {
            ResourceBooking hb = reactivateOrCreate(orderId, "HALL", c.getHallBookingId(),
                    body.get("hallId"), farewell.minusMinutes(30), farewell.plusHours(2));
            if (hb != null) { c.setHallBookingId(hb.getId()); hallReconfirmed = true; }
        }
        if (cremation != null) {
            ResourceBooking fb = reactivateOrCreate(orderId, "FURNACE", c.getFurnaceBookingId(),
                    body.get("furnaceId"), cremation.minusMinutes(30), cremation.plusMinutes(60));
            if (fb != null) { c.setFurnaceBookingId(fb.getId()); furnaceReconfirmed = true; }
        }
        c.setScheduleConfirmedAt(now);
        c.setScheduleVerified(true);
        if (c.getScheduleNote() == null) c.setScheduleNote("到馆后礼厅与火化排期已重新确认");

        // 只关闭本次到馆实际重新确认的协同：车辆延误、冷藏/礼厅/火化排期；
        // 未在本次恢复的异常（如仍缺材料）保持开放，不被无关步骤一并关闭
        Map<String, Boolean> arrived = new LinkedHashMap<>();
        arrived.put("VEHICLE_DELAY", true);
        arrived.put("COLD_SHORTAGE", coldReconfirmed);
        arrived.put("HALL_CONFLICT", hallReconfirmed);
        arrived.put("CREMATION_CONFLICT", furnaceReconfirmed);
        // 到馆证明复核无误时，材料缺失协同一并闭环（留补件/复核依据）
        arrived.put("DOC_MISSING", true);
        resolveCollabByTypes(orderId, arrived, "跨县接运车辆已到馆并完成交接/入库/排期重新确认");

        orderRepo.save(o);
        crossRepo.save(c);
        timeline.add(orderId, "CROSS_REGION", "跨县接运到馆回写：车辆交接（" + c.getReceiverName()
                + "）、冷藏入库、死亡证明复核、礼厅/火化排期确认均已回写治丧单，对应协同已按恢复情况闭环");
        return c;
    }

    /**
     * 到馆回写硬门禁：必须六项核验全部通过且车辆已发车（在途，或在途延误后的暂停）。
     * 核验未通过、核验阶段暂停、未发车一律拒绝；调用在事务内，拒绝即整体回滚，不落任何变更。
     */
    private void requireArrivable(CrossRegionTransport c) {
        if (c.getArrivedAt() != null || "ARRIVED".equals(c.getStatus())) {
            throw new BusinessException("该治丧单已完成到馆回写，不能重复登记");
        }
        if (c.getDepartedAt() == null) {
            throw new BusinessException("接运车辆尚未发车，不能登记到馆；请先完成六项核验并发车");
        }
        List<String> missing = new ArrayList<>();
        if (!Boolean.TRUE.equals(c.getCertVerified())) missing.add("死亡证明核验");
        if (!Boolean.TRUE.equals(c.getPermitVerified())) missing.add("接运许可核验");
        if (!Boolean.TRUE.equals(c.getVehicleVerified())) missing.add("车辆资质核验");
        if (!Boolean.TRUE.equals(c.getColdConditionVerified())) missing.add("冷藏条件核验");
        if (!Boolean.TRUE.equals(c.getReceptionCapacityVerified())) missing.add("馆内接收能力核验");
        if (!Boolean.TRUE.equals(c.getScheduleVerified())) missing.add("火化/礼厅排期");
        if (!missing.isEmpty()) {
            throw new BusinessException("以下核验项未通过，不能登记到馆：" + String.join("、", missing)
                    + "；请补齐证明/许可/车辆/冷藏与排期后重新核验");
        }
    }

    private ResourceBooking reactivateOrCreate(Long orderId, String type, Long existingId,
                                               Object newIdRaw, LocalDateTime start, LocalDateTime end) {
        Long newId = newIdRaw == null || String.valueOf(newIdRaw).isBlank()
                ? null : Long.valueOf(String.valueOf(newIdRaw));
        ResourceBooking booking = existingId == null ? null
                : bookingRepo.findById(existingId).orElse(null);
        if (booking != null && (newId == null || newId.equals(booking.getResourceId()))) {
            booking.setStartAt(start);
            booking.setEndAt(end);
            booking.setStatus("CONFIRMED");
            booking.setNote("到馆后重新确认");
            return bookingRepo.save(booking);
        }
        Long rid = newId != null ? newId : (booking == null ? null : booking.getResourceId());
        if (rid == null) return null;
        Resource r = resourceRepo.findById(rid).orElse(null);
        if (r == null) return null;
        List<ResourceBooking> conflicts = bookingRepo.findConflicts(type, rid, start, end, orderId);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(typeText(type) + " " + r.getName() + " 到馆新时段仍有冲突，请改期后再回写");
        }
        if (booking != null) {
            booking.setStatus("RELEASED");
            booking.setNote("到馆改期/换资源释放");
            bookingRepo.save(booking);
        }
        return lockBooking(orderId, type, r, start, end, null, null, null, start);
    }

    // ---------------- helpers ----------------

    private ResourceBooking lockBooking(Long orderId, String type, Resource r, LocalDateTime start,
                                        LocalDateTime end, String driver, String driverPhone,
                                        String permit, LocalDateTime eta) {
        bookingRepo.findByOrderId(orderId).stream()
                .filter(b -> b.getResourceType().equals(type) && !"RELEASED".equals(b.getStatus()))
                .forEach(b -> {
                    b.setStatus("RELEASED");
                    b.setNote("跨县核验重新锁定，释放旧占用");
                    bookingRepo.save(b);
                });
        ResourceBooking b = new ResourceBooking();
        b.setOrderId(orderId);
        b.setResourceType(type);
        b.setResourceId(r.getId());
        b.setResourceName(r.getName());
        b.setStartAt(start);
        b.setEndAt(end);
        b.setStatus("CONFIRMED");
        b.setDriverName(driver);
        b.setDriverPhone(driverPhone);
        b.setPermitNo(permit);
        b.setEstimatedArrivalAt(eta);
        return bookingRepo.save(b);
    }

    private void createCollabIfAbsent(FuneralOrder o, String type, String title, String desc, String role, int priority) {
        boolean exists = collabRepo.findByOrderIdOrderByPriorityAscCreatedAtDesc(o.getId()).stream()
                .anyMatch(t -> type.equals(t.getType()) && !"RESOLVED".equals(t.getStatus()));
        if (exists) return;
        CollaborationTask t = new CollaborationTask();
        t.setOrderId(o.getId());
        t.setType(type);
        t.setTitle(title);
        t.setDescription(desc);
        t.setAssigneeRole(role);
        t.setPriority(priority);
        t.setCreatedByName(CurrentUser.name().equals("系统") ? "系统自动" : CurrentUser.name());
        t.setCreatedById(CurrentUser.id());
        collabRepo.save(t);
        timeline.addAsSystem(o.getId(), "COLLAB", "发起协同【" + title + "】→ " + role);
    }

    private String failureText(List<String> failures) {
        List<String> texts = new ArrayList<>();
        for (String f : failures) texts.add(switch (f) {
            case "DOC_MISSING" -> "死亡证明材料缺失，需补交出具机构证明";
            case "PERMIT_MISSING" -> "跨县接运许可待补";
            case "VEHICLE_ISSUE" -> "接运车辆资质/排班异常";
            case "COLD_SHORTAGE" -> "馆内冷藏位不足";
            case "HALL_CONFLICT" -> "告别厅排期冲突";
            case "CREMATION_CONFLICT" -> "火化排期冲突";
            default -> f;
        });
        return String.join("；", texts);
    }

    private static Map<String, Object> ok(String type, String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type); m.put("pass", true); m.put("message", msg);
        return m;
    }
    private static Map<String, Object> fail(String type, String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type); m.put("pass", false); m.put("message", msg);
        return m;
    }
    private static Map<String, Object> skip(String type, String msg) {
        Map<String, Object> m = ok(type, msg);
        m.put("skipped", true);
        return m;
    }
    private static String typeText(String t) {
        return switch (t) {
            case "VEHICLE" -> "接运车辆"; case "COLD" -> "冷藏位";
            case "HALL" -> "告别厅"; case "FURNACE" -> "火化设备"; default -> t;
        };
    }
    private static String str(Map<String, Object> b, String k, String err) {
        Object v = b.get(k);
        if (v == null || v.toString().isBlank()) throw new BusinessException(err);
        return v.toString();
    }
    private static String nz(String s) { return s == null ? "" : s; }

    private FuneralOrder mustOrder(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new BusinessException("治丧单不存在：" + id));
    }
    private CrossRegionTransport mustCross(Long orderId) {
        return crossRepo.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("尚未登记跨县接运信息"));
    }
}
