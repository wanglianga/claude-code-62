package com.funeral.service;

import com.funeral.common.BusinessException;
import com.funeral.common.Constants;
import com.funeral.common.CurrentUser;
import com.funeral.entity.*;
import com.funeral.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {

    private final FuneralOrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final CatalogItemRepository catalogRepo;
    private final ResourceRepository resourceRepo;
    private final ResourceBookingRepository bookingRepo;
    private final SignatureRecordRepository signatureRepo;
    private final CommunicationLogRepository logRepo;
    private final CollaborationTaskRepository collabRepo;
    private final OrderTimelineRepository timelineRepo;
    private final ServiceArchiveRepository archiveRepo;
    private final TimelineService timeline;

    public OrderService(FuneralOrderRepository orderRepo, OrderItemRepository itemRepo,
                        CatalogItemRepository catalogRepo, ResourceRepository resourceRepo,
                        ResourceBookingRepository bookingRepo, SignatureRecordRepository signatureRepo,
                        CommunicationLogRepository logRepo, CollaborationTaskRepository collabRepo,
                        OrderTimelineRepository timelineRepo, ServiceArchiveRepository archiveRepo,
                        TimelineService timeline) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.catalogRepo = catalogRepo;
        this.resourceRepo = resourceRepo;
        this.bookingRepo = bookingRepo;
        this.signatureRepo = signatureRepo;
        this.logRepo = logRepo;
        this.collabRepo = collabRepo;
        this.timelineRepo = timelineRepo;
        this.archiveRepo = archiveRepo;
        this.timeline = timeline;
    }

    // ============================================================
    // 预约创建 / 详情
    // ============================================================

    @Transactional
    public FuneralOrder createOrder(Map<String, Object> body) {
        FuneralOrder o = new FuneralOrder();
        o.setOrderNo(generateOrderNo());
        o.setDeceasedName(str(body, "deceasedName", "逝者姓名不能为空"));
        o.setGender((String) body.get("gender"));
        o.setAge(intOrNull(body.get("age")));
        o.setIdCardNo((String) body.get("idCardNo"));
        o.setDeathTime(parseTime(body.get("deathTime")));
        o.setDeathCause((String) body.get("deathCause"));
        o.setFromOtherCity(bool(body, "fromOtherCity"));
        o.setCertificateNo((String) body.get("certificateNo"));
        String certStatus = (String) body.getOrDefault("certificateStatus", "PENDING");
        o.setCertificateStatus(certStatus == null || certStatus.isBlank() ? "PENDING" : certStatus);
        o.setCertificateNote((String) body.get("certificateNote"));
        o.setPickupAddress((String) body.get("pickupAddress"));
        o.setPickupTime(parseTime(body.get("pickupTime")));
        o.setReligiousCustom((String) body.get("religiousCustom"));
        o.setNeedRefrigeration(bool(body, "needRefrigeration"));
        o.setHallSpec((String) body.get("hallSpec"));
        o.setFarewellTime(parseTime(body.get("farewellTime")));
        o.setContactName(str(body, "contactName", "亲属联系人姓名不能为空"));
        o.setContactPhone((String) body.get("contactPhone"));
        o.setContactRelation((String) body.get("contactRelation"));
        o.setCreatedById(CurrentUser.id());
        o.setCreatedByName(CurrentUser.name());
        o.setStatus("RESOURCE_VERIFYING");
        orderRepo.save(o);

        // 提交时自动识别异常并发起跨岗位协同
        if (Boolean.TRUE.equals(o.getFromOtherCity())) {
            createCollabInternal(o, "NONLOCAL", "外地逝者接运协同",
                    "逝者由外地接运，需接运组核对长途车辆、途经手续与到馆时间。",
                    Constants.ROLE_TRANSPORT, 1);
        }
        if ("MISSING".equals(o.getCertificateStatus())) {
            createCollabInternal(o, "DOC_MISSING", "死亡证明材料缺失待补",
                    "证明材料缺失：" + nz(o.getCertificateNote(), "家属尚未补交死亡证明"),
                    Constants.ROLE_CLERK, 1);
        }
        timeline.add(o.getId(), "SUBMIT", "家属提交预约：逝者 " + o.getDeceasedName()
                + "，联系人 " + o.getContactName());
        return o;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        FuneralOrder o = mustGet(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", o);
        m.put("items", itemRepo.findByOrderIdOrderByCategoryAscIdAsc(id));
        m.put("bookings", bookingRepo.findByOrderId(id));
        m.put("signatures", signatureRepo.findByOrderIdOrderBySignedAtDesc(id));
        m.put("communicationLogs", logRepo.findByOrderIdOrderByCreatedAtDesc(id));
        m.put("collaborations", collabRepo.findByOrderIdOrderByPriorityAscCreatedAtDesc(id));
        m.put("timeline", timelineRepo.findByOrderIdOrderByCreatedAtAsc(id));
        m.put("archive", archiveRepo.findByOrderId(id).orElse(null));
        m.put("bill", buildBill(o));
        return m;
    }

    // ============================================================
    // 资源核验：证件 / 车辆 / 冷藏位 / 火化排期 / 礼厅
    // ============================================================

    @Transactional
    public Map<String, Object> verifyResources(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);

        // 1) 证件核验
        String certStatus = (String) body.getOrDefault("certificateStatus", o.getCertificateStatus());
        o.setCertificateStatus(certStatus);
        if (body.get("certificateNo") != null) o.setCertificateNo((String) body.get("certificateNo"));
        if (body.get("certificateNote") != null) o.setCertificateNote((String) body.get("certificateNote"));

        LocalDateTime pickup;
        if (body.get("pickupTime") != null) {
            pickup = parseTime(body.get("pickupTime"));
            o.setPickupTime(pickup);
        } else {
            pickup = o.getPickupTime();
        }
        LocalDateTime farewell;
        if (body.get("farewellTime") != null) {
            farewell = parseTime(body.get("farewellTime"));
            o.setFarewellTime(farewell);
        } else {
            farewell = o.getFarewellTime();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        boolean allPass = "VERIFIED".equals(certStatus) || "PENDING".equals(certStatus);
        // 证件缺失直接阻断核验通过
        if ("MISSING".equals(certStatus)) allPass = false;

        allPass &= checkOne(o, results, Constants.RES_VEHICLE, id(body.get("vehicleId")),
                minusHours(pickup, 1), plusHours(pickup, 2), "接运车辆", true);
        if (Boolean.TRUE.equals(o.getNeedRefrigeration())) {
            allPass &= checkOne(o, results, Constants.RES_COLD, id(body.get("coldId")),
                    pickup, farewell == null ? plusHours(pickup, 48) : farewell, "冷藏位", true);
        } else {
            results.add(skip("COLD", "家属未申请冷藏"));
        }
        allPass &= checkOne(o, results, Constants.RES_HALL, id(body.get("hallId")),
                minusMinutes(farewell, 30), plusHours(farewell, 2), "告别厅", true);
        LocalDateTime cremation = body.get("cremationTime") != null
                ? parseTime(body.get("cremationTime")) : plusHours(farewell, 2);
        allPass &= checkOne(o, results, Constants.RES_FURNACE, id(body.get("furnaceId")),
                minusMinutes(cremation, 30), plusMinutes(cremation, 60), "火化设备", true);

        // 礼厅规格一致性提示
        Long hallId = id(body.get("hallId"));
        if (hallId != null && o.getHallSpec() != null) {
            resourceRepo.findById(hallId).ifPresent(h -> {
                if (!o.getHallSpec().equals(h.getHallSpec())) {
                    Map<String, Object> warn = new LinkedHashMap<>();
                    warn.put("type", "HALL_SPEC_WARN");
                    warn.put("pass", false);
                    warn.put("message", "所选 " + h.getName() + " 规格(" + nz(h.getHallSpec(), "-")
                            + ")与家属预约规格(" + o.getHallSpec() + ")不一致，请沟通确认");
                    results.add(warn);
                }
            });
        }

        String note = (String) body.get("verifyNote");
        if (allPass) {
            o.setVerifyStatus("PASS");
            o.setVerifyNote(nz(note, "证件、车辆、冷藏位、礼厅、火化排期核验通过"));
            if ("MISSING".equals(o.getCertificateStatus())) {
                throw new BusinessException("死亡证明材料缺失，不能核验通过，请先补证或走协同补办");
            }
            o.setCertificateStatus("VERIFIED");
            if (!List.of("NEGOTIATING", "CONFIRMED", "IN_SERVICE", "COMPLETED", "SETTLED").contains(o.getStatus())) {
                o.setStatus("VERIFIED");
            }
            timeline.add(orderId, "VERIFY", "资源核验通过，已预占接运车辆/冷藏位/礼厅/火化时段");
        } else {
            o.setVerifyStatus("FAIL");
            o.setVerifyNote(nz(note, "核验存在未通过项，已发起协同处理"));
            timeline.add(orderId, "VERIFY", "资源核验未全部通过，等待协同处理");
            // 自动按异常类型发起协同
            ensureConflictCollab(o, results, "HALL", "HALL_CONFLICT", "告别厅临时冲突",
                    "预约告别时段礼厅被占用或规格不符，需礼厅管理员协调改厅或调整时间。",
                    Constants.ROLE_HALL_ADMIN);
            ensureConflictCollab(o, results, "FURNACE", "FURNACE_MAINT", "火化设备检修/排期冲突",
                    "火化设备检修或时段排期冲突，需调整火化时段或更换火化炉。",
                    Constants.ROLE_HALL_ADMIN);
            ensureConflictCollab(o, results, "VEHICLE", "GENERAL", "接运车辆紧张",
                    "接运时段车辆不足，需接运组调度。", Constants.ROLE_TRANSPORT);
            ensureConflictCollab(o, results, "COLD", "GENERAL", "冷藏位紧张",
                    "冷藏位不足，需礼厅管理员统筹。", Constants.ROLE_HALL_ADMIN);
        }
        orderRepo.save(o);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("pass", allPass);
        resp.put("certificateStatus", o.getCertificateStatus());
        resp.put("results", results);
        resp.put("order", o);
        return resp;
    }

    /** 核验单个资源：可用性 + 时段冲突；通过则预占（HELD），并释放该订单该类型旧预占 */
    private boolean checkOne(FuneralOrder o, List<Map<String, Object>> results, String type,
                             Long resourceId, LocalDateTime start, LocalDateTime end,
                             String label, boolean required) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", type);
        if (resourceId == null || start == null || end == null) {
            if (required) {
                r.put("pass", false);
                r.put("message", label + "：缺少资源或时间信息");
                results.add(r);
                return false;
            }
            return true;
        }
        Resource res = resourceRepo.findById(resourceId).orElse(null);
        if (res == null) {
            r.put("pass", false);
            r.put("message", label + "：资源不存在");
            results.add(r);
            return false;
        }
        r.put("resourceId", res.getId());
        r.put("resourceName", res.getName());
        r.put("start", start);
        r.put("end", end);

        if (!Boolean.TRUE.equals(res.getAvailable())) {
            r.put("pass", false);
            r.put("message", label + " " + res.getName() + " 已停用/检修"
                    + (res.getUnavailableUntil() != null ? "（至 " + res.getUnavailableUntil() + "）" : "")
                    + "：" + nz(res.getNote(), ""));
            r.put("alternatives", findAlternatives(type, start, end, o.getId()));
            results.add(r);
            return false;
        }
        List<ResourceBooking> conflicts = bookingRepo.findConflicts(type, resourceId, start, end, o.getId());
        if (!conflicts.isEmpty()) {
            r.put("pass", false);
            r.put("message", label + " " + res.getName() + " 在该时段已被治丧单 "
                    + conflicts.stream().map(b -> "#" + b.getOrderId()).toList() + " 占用");
            r.put("alternatives", findAlternatives(type, start, end, o.getId()));
            results.add(r);
            return false;
        }
        // 通过：释放旧预占、建立新预占
        bookingRepo.findByOrderId(o.getId()).stream()
                .filter(b -> b.getResourceType().equals(type) && !"RELEASED".equals(b.getStatus()))
                .forEach(b -> {
                    b.setStatus("RELEASED");
                    b.setNote("核验改期/改资源自动释放");
                    bookingRepo.save(b);
                });
        ResourceBooking booking = new ResourceBooking();
        booking.setOrderId(o.getId());
        booking.setResourceType(type);
        booking.setResourceId(res.getId());
        booking.setResourceName(res.getName());
        booking.setStartAt(start);
        booking.setEndAt(end);
        booking.setStatus("CONFIRMED");
        bookingRepo.save(booking);

        r.put("pass", true);
        r.put("message", label + " " + res.getName() + " 时段可用，已预占");
        results.add(r);
        return true;
    }

    private List<Map<String, Object>> findAlternatives(String type, LocalDateTime start,
                                                       LocalDateTime end, Long orderId) {
        List<Long> busy = bookingRepo.findBusyOfType(type, start, end).stream()
                .map(ResourceBooking::getResourceId).toList();
        List<Map<String, Object>> alts = new ArrayList<>();
        for (Resource r : resourceRepo.findByTypeOrderByIdAsc(type)) {
            if (Boolean.TRUE.equals(r.getAvailable()) && !busy.contains(r.getId())) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("id", r.getId());
                a.put("name", r.getName());
                a.put("hallSpec", r.getHallSpec());
                alts.add(a);
            }
        }
        return alts;
    }

    private Map<String, Object> skip(String type, String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("pass", true);
        m.put("skipped", true);
        m.put("message", msg);
        return m;
    }

    private void ensureConflictCollab(FuneralOrder o, List<Map<String, Object>> results,
                                      String resultType, String collabType, String title,
                                      String desc, String role) {
        boolean hit = results.stream().anyMatch(r -> resultType.equals(r.get("type"))
                && Boolean.FALSE.equals(r.get("pass")));
        if (!hit) return;
        boolean exists = collabRepo.findByOrderIdOrderByPriorityAscCreatedAtDesc(o.getId()).stream()
                .anyMatch(t -> collabType.equals(t.getType()) && !"RESOLVED".equals(t.getStatus()));
        if (!exists) createCollabInternal(o, collabType, title, desc, role, 1);
    }

    // ============================================================
    // 订单条目（同一订单内说明全部治丧项目）
    // ============================================================

    @Transactional
    public OrderItem addItem(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        requireStage(o, "NEGOTIATING", "VERIFIED", "CONFIRMED", "IN_SERVICE", "COMPLETED", "RESOURCE_VERIFYING");

        Long catalogId = id(body.get("catalogId"));
        int qty = body.get("quantity") == null ? 1 : Math.max(1, ((Number) body.get("quantity")).intValue());

        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        if (catalogId != null) {
            CatalogItem c = catalogRepo.findById(catalogId)
                    .orElseThrow(() -> new BusinessException("服务目录项不存在"));
            item.setCatalogId(c.getId());
            item.setName(c.getName());
            item.setCategory(c.getCategory());
            item.setServiceClass(c.getServiceClass());
            item.setUnit(c.getUnit());
            item.setUnitPrice(c.getUnitPrice());
            item.setRefundable(c.getRefundable());
            item.setSource("CATALOG");
            item.setSourceNote(c.getSourceNote());
        } else {
            item.setName(str(body, "name", "自定义项目名称不能为空"));
            item.setCategory(nz((String) body.get("category"), "OTHER"));
            item.setServiceClass(nz((String) body.get("serviceClass"), Constants.CLASS_OPTIONAL));
            item.setUnit(nz((String) body.get("unit"), "项"));
            item.setUnitPrice(decimal(body.get("unitPrice")));
            item.setRefundable(Boolean.TRUE.equals(body.get("refundable")) || !body.containsKey("refundable"));
            item.setSource("CUSTOM");
            item.setSourceNote(nz((String) body.get("sourceNote"), "现场约定，以家属签字为准"));
        }
        item.setQuantity(qty);
        item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        item.setStatus("PENDING");
        item.setRemark((String) body.get("remark"));

        // 服务进行中临时增加：必须先有已锁定的沟通记录，并由家属当场手写签字
        boolean liveStage = List.of("CONFIRMED", "IN_SERVICE", "COMPLETED").contains(o.getStatus());
        CommunicationLog lockedLog = null;
        if (liveStage) {
            lockedLog = requireLockedTempLog(orderId, id(body.get("communicationLogId")));
            if (body.get("signatureData") == null
                    || !String.valueOf(body.get("signatureData")).startsWith("data:image/")) {
                throw new BusinessException("现场临时增项必须由家属手写签字确认后才能加入订单");
            }
        }
        checkStock(item, qty, false);
        itemRepo.save(item);

        if (liveStage) {
            SignatureRecord sign = saveSignature(o, "CONFIRM_ITEM",
                    "现场临时新增项目（凭锁定沟通记录#" + lockedLog.getId() + "）：" + item.getName()
                            + " ×" + qty + "，小计 " + item.getSubtotal()
                            + "（" + classText(item.getServiceClass()) + "，"
                            + (item.getRefundable() ? "可退" : "不可退") + "）", item.getId(), body);
            deductStock(item.getCatalogId(), qty);
            item.setStatus("CONFIRMED");
            item.setSignatureId(sign.getId());
            item.setConfirmedByName(str(body, "signerName", "请填写签字家属姓名"));
            item.setConfirmedAt(LocalDateTime.now());
            itemRepo.save(item);
            lockedLog.setChangeApplied(true);
            lockedLog.setChangeNote(nz(lockedLog.getChangeNote(), "")
                    + "｜新增项目（家属已签字）：" + item.getName() + " ×" + qty);
            logRepo.save(lockedLog);
        }
        if (List.of("VERIFIED", "RESOURCE_VERIFYING").contains(o.getStatus())) {
            o.setStatus("NEGOTIATING");
            orderRepo.save(o);
        }
        timeline.add(orderId, "ITEM_CHANGE", (liveStage ? "现场凭锁定沟通记录新增并经家属签字：" : "加入项目：")
                + item.getName() + " ×" + qty + "（" + classText(item.getServiceClass()) + "）");
        return item;
    }

    /** 家属逐项确认（手写签字），确认时扣减物品库存 */
    @Transactional
    public SignatureRecord confirmItem(Long orderId, Long itemId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        OrderItem item = itemRepo.findById(itemId)
                .filter(i -> i.getOrderId().equals(orderId))
                .orElseThrow(() -> new BusinessException("订单条目不存在"));
        if ("REMOVED".equals(item.getStatus())) throw new BusinessException("该项目已删减");
        if ("CONFIRMED".equals(item.getStatus())) throw new BusinessException("该项目已确认，无需重复签字");

        SignatureRecord sign = saveSignature(o, "CONFIRM_ITEM",
                "确认项目：" + item.getName() + " ×" + item.getQuantity()
                        + "，单价 " + item.getUnitPrice() + "，小计 " + item.getSubtotal()
                        + "（" + classText(item.getServiceClass()) + "，"
                        + (item.getRefundable() ? "可退" : "不可退") + "）", itemId, body);

        checkStock(item, item.getQuantity(), false);
        deductStock(item.getCatalogId(), item.getQuantity());
        item.setStatus("CONFIRMED");
        item.setConfirmedById(CurrentUser.id());
        item.setConfirmedByName(CurrentUser.name());
        item.setConfirmedAt(java.time.LocalDateTime.now());
        item.setSignatureId(sign.getId());
        itemRepo.save(item);
        timeline.add(orderId, "SIGN", "家属签字确认项目：" + item.getName());
        return sign;
    }

    /** 家属删减项目（始终保留签字）；已确认物品退回库存 */
    @Transactional
    public SignatureRecord removeItem(Long orderId, Long itemId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        OrderItem item = itemRepo.findById(itemId)
                .filter(i -> i.getOrderId().equals(orderId))
                .orElseThrow(() -> new BusinessException("订单条目不存在"));
        if ("REMOVED".equals(item.getStatus())) throw new BusinessException("该项目已删减");
        if (o.getStatus().equals("SETTLED") || o.getStatus().equals("ARCHIVED")) {
            throw new BusinessException("订单已结算归档，不能删减项目");
        }
        String reason = nz((String) body.get("reason"), "家属要求删减");
        SignatureRecord sign = saveSignature(o, "REMOVE_ITEM",
                "删减项目：" + item.getName() + " ×" + item.getQuantity()
                        + "，原因：" + reason
                        + (item.getRefundable() ? "（该项目可退）" : "（该项目不可退，需按政策处理）"),
                itemId, body);

        boolean liveStage = List.of("CONFIRMED", "IN_SERVICE", "COMPLETED").contains(o.getStatus());
        if (liveStage) {
            CommunicationLog log = requireLockedTempLog(orderId, id(body.get("communicationLogId")));
            log.setChangeApplied(true);
            log.setChangeNote(nz(log.getChangeNote(), "") + "｜删减项目：" + item.getName());
            logRepo.save(log);
        }
        if ("CONFIRMED".equals(item.getStatus())) {
            deductStock(item.getCatalogId(), -item.getQuantity());
        }
        item.setStatus("REMOVED");
        item.setRemark(nz(item.getRemark(), "") + "｜删减原因：" + reason + "（家属已签字）");
        itemRepo.save(item);
        timeline.add(orderId, "SIGN", "家属签字删减项目：" + item.getName() + "（原因：" + reason + "）");
        return sign;
    }

    /** 家属确认整体治丧方案（签字） */
    @Transactional
    public SignatureRecord confirmPlan(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        List<OrderItem> items = itemRepo.findByOrderIdOrderByCategoryAscIdAsc(orderId).stream()
                .filter(i -> !"REMOVED".equals(i.getStatus())).toList();
        if (items.isEmpty()) throw new BusinessException("订单尚无治丧项目，无法确认方案");
        if (!"PASS".equals(o.getVerifyStatus())) {
            throw new BusinessException("资源核验尚未通过，不能确认治丧方案");
        }
        List<OrderItem> pending = items.stream().filter(i -> "PENDING".equals(i.getStatus())).toList();

        SignatureRecord planSign = saveSignature(o, "CONFIRM_PLAN",
                "确认治丧方案：共 " + items.size() + " 项，费用合计（未减补助）"
                        + buildBill(o).get("totalAmount") + " 元", null, body);
        // 未逐项签字的条目，以方案签字一并确认并留痕
        for (OrderItem it : pending) {
            checkStock(it, it.getQuantity(), true);
            deductStock(it.getCatalogId(), it.getQuantity());
            it.setStatus("CONFIRMED");
            it.setConfirmedById(CurrentUser.id());
            it.setConfirmedByName(CurrentUser.name());
            it.setConfirmedAt(java.time.LocalDateTime.now());
            it.setSignatureId(planSign.getId());
            itemRepo.save(it);
        }
        o.setFamilyConfirmed(true);
        o.setStatus("CONFIRMED");
        orderRepo.save(o);
        timeline.add(orderId, "SIGN", "家属签字确认整体治丧方案（含 " + pending.size() + " 个未单独签字项目一并确认）");
        return planSign;
    }

    private void checkStock(OrderItem item, int qty, boolean ignoreZero) {
        if (item.getCatalogId() == null) return;
        CatalogItem c = catalogRepo.findById(item.getCatalogId()).orElse(null);
        if (c == null || c.getStock() == null) return;
        if (c.getStock() < qty) {
            throw new BusinessException("物品【" + c.getName() + "】库存不足：当前库存 " + c.getStock()
                    + "，本次需要 " + qty + "，请先与家属沟通更换方案");
        }
    }

    private void deductStock(Long catalogId, int delta) {
        if (catalogId == null) return;
        catalogRepo.findById(catalogId).ifPresent(c -> {
            if (c.getStock() != null) c.setStock(Math.max(0, c.getStock() - delta));
            catalogRepo.save(c);
        });
    }

    // ============================================================
    // 沟通记录 + "先记录后变更"
    // ============================================================

    @Transactional
    public CommunicationLog addCommunicationLog(Long orderId, Map<String, Object> body) {
        mustGet(orderId);
        CommunicationLog log = new CommunicationLog();
        log.setOrderId(orderId);
        log.setType(nz((String) body.get("type"), "CHAT"));
        log.setContent(str(body, "content", "沟通内容不能为空"));
        log.setParticipants((String) body.get("participants"));
        log.setAuthorId(CurrentUser.id());
        log.setAuthorName(CurrentUser.name());
        // 临时变更/情绪激动/意见分歧类记录保存即锁定，作为随后变更礼厅/车辆/库存的依据
        String type = log.getType();
        log.setLocked(Boolean.TRUE.equals(body.get("locked"))
                || List.of("TEMP_CHANGE", "EMOTIONAL", "DISPUTE").contains(type));
        logRepo.save(log);
        timeline.add(orderId, "NEGOTIATE", "沟通记录[" + commTypeText(type) + "]："
                + abbreviate(log.getContent(), 80) + (log.getLocked() ? "（已锁定）" : ""));

        // 亲属意见不一致 / 情绪激动：自动发起馆领导协同
        if (("DISPUTE".equals(type) || "EMOTIONAL".equals(type))) {
            boolean exists = collabRepo.findByOrderIdOrderByPriorityAscCreatedAtDesc(orderId).stream()
                    .anyMatch(t -> "FAMILY_DISAGREE".equals(t.getType()) && !"RESOLVED".equals(t.getStatus()));
            if (!exists) {
                FuneralOrder o = mustGet(orderId);
                createCollabInternal(o, "FAMILY_DISAGREE",
                        "EMOTIONAL".equals(type) ? "家属情绪激动需安抚协同" : "亲属意见不一致待协调",
                        "沟通记录：" + abbreviate(log.getContent(), 200),
                        Constants.ROLE_LEADER, 1);
            }
        }
        return log;
    }

    /** 凭已锁定沟通记录变更礼厅/车辆等资源（重新核验并预占新资源） */
    @Transactional
    public Map<String, Object> changeResourceAfterLog(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        CommunicationLog log = requireLockedTempLog(orderId, id(body.get("communicationLogId")));
        Map<String, Object> verifyBody = new HashMap<>(body);
        Map<String, Object> result = verifyResources(orderId, verifyBody);
        log.setChangeApplied(true);
        log.setChangeNote(nz(log.getChangeNote(), "")
                + "｜凭沟通记录变更资源：车辆#" + body.get("vehicleId")
                + " 冷藏位#" + body.get("coldId")
                + " 礼厅#" + body.get("hallId")
                + " 火化炉#" + body.get("furnaceId")
                + "，核验结果：" + (Boolean.TRUE.equals(result.get("pass")) ? "通过" : "未通过"));
        logRepo.save(log);
        timeline.add(orderId, "RESOURCE_CHANGE", "凭锁定沟通记录变更礼厅/车辆/火化排期，结果："
                + (Boolean.TRUE.equals(result.get("pass")) ? "通过" : "未通过"));
        return result;
    }

    private CommunicationLog requireLockedTempLog(Long orderId, Long logId) {
        if (logId == null) {
            throw new BusinessException("现场变更必须先保存并锁定沟通记录，再变更礼厅、车辆和物品库存");
        }
        CommunicationLog log = logRepo.findById(logId)
                .orElseThrow(() -> new BusinessException("沟通记录不存在"));
        if (!log.getOrderId().equals(orderId)) throw new BusinessException("沟通记录与治丧单不匹配");
        if (!Boolean.TRUE.equals(log.getLocked())) {
            throw new BusinessException("沟通记录尚未锁定，不能作为变更依据，请先保存并锁定");
        }
        return log;
    }

    // ============================================================
    // 协同待办
    // ============================================================

    @Transactional
    public CollaborationTask createCollab(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        return createCollabInternal(o,
                nz((String) body.get("type"), "GENERAL"),
                str(body, "title", "协同标题不能为空"),
                str(body, "description", "协同说明不能为空"),
                nz((String) body.get("assigneeRole"), Constants.ROLE_CLERK),
                body.get("priority") == null ? 2 : ((Number) body.get("priority")).intValue());
    }

    private CollaborationTask createCollabInternal(FuneralOrder o, String type, String title,
                                                   String desc, String role, int priority) {
        CollaborationTask t = new CollaborationTask();
        t.setOrderId(o.getId());
        t.setType(type);
        t.setTitle(title);
        t.setDescription(desc);
        t.setAssigneeRole(role);
        t.setPriority(priority);
        t.setStatus("OPEN");
        t.setCreatedById(CurrentUser.id());
        t.setCreatedByName(CurrentUser.name().equals("系统") ? "系统自动" : CurrentUser.name());
        collabRepo.save(t);
        timeline.addAsSystem(o.getId(), "COLLAB", "发起协同【" + title + "】→ " + roleText(role));
        return t;
    }

    @Transactional
    public CollaborationTask resolveCollab(Long taskId, Map<String, Object> body) {
        CollaborationTask t = collabRepo.findById(taskId)
                .orElseThrow(() -> new BusinessException("协同任务不存在"));
        t.setStatus("RESOLVED");
        t.setResolution(str(body, "resolution", "处理结果不能为空"));
        t.setAssigneeId(CurrentUser.id());
        t.setAssigneeName(CurrentUser.name());
        t.setResolvedAt(LocalDateTime.now());
        collabRepo.save(t);
        timeline.add(t.getOrderId(), "COLLAB", "协同【" + t.getTitle() + "】已解决："
                + abbreviate(t.getResolution(), 100));
        return t;
    }

    // ============================================================
    // 低保等减免审核（政府补助项目 + 减免依据）
    // ============================================================

    @Transactional
    public FuneralOrder requestReduction(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        o.setReductionStatus("PENDING");
        orderRepo.save(o);
        createCollabInternal(o, "REDUCTION_REVIEW", "低保/救助减免待审核",
                "家属申请减免：" + nz((String) body.get("reason"), "见证件材料"),
                Constants.ROLE_FINANCE, 1);
        timeline.add(orderId, "REDUCTION", "家属提交减免申请，等待财务/馆领导审核");
        return o;
    }

    @Transactional
    public FuneralOrder reviewReduction(Long orderId, boolean approved, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        if (approved) {
            o.setReductionStatus("APPROVED");
            // 审批通过的补助项目加入订单（来源=政策，家属可见减免依据）
            @SuppressWarnings("unchecked")
            List<Number> ids = (List<Number>) body.getOrDefault("subsidyCatalogIds", List.of());
            for (Number cid : ids) {
                CatalogItem c = catalogRepo.findById(cid.longValue()).orElse(null);
                if (c == null || !Constants.CLASS_SUBSIDY.equals(c.getServiceClass())) continue;
                boolean exists = itemRepo.findByOrderIdOrderByCategoryAscIdAsc(orderId).stream()
                        .anyMatch(i -> !"REMOVED".equals(i.getStatus())
                                && c.getId().equals(i.getCatalogId()));
                if (exists) continue;
                OrderItem item = new OrderItem();
                item.setOrderId(orderId);
                item.setCatalogId(c.getId());
                item.setName(c.getName());
                item.setCategory(c.getCategory());
                item.setServiceClass(Constants.CLASS_SUBSIDY);
                item.setUnit(c.getUnit());
                item.setUnitPrice(c.getUnitPrice());
                item.setQuantity(1);
                item.setSubtotal(c.getUnitPrice());
                item.setStatus("CONFIRMED");
                item.setRefundable(false);
                item.setSource("POLICY");
                item.setSourceNote("经" + CurrentUser.name() + "审核通过：" + nz(c.getSourceNote(), "政府补助政策"));
                item.setConfirmedById(CurrentUser.id());
                item.setConfirmedByName(CurrentUser.name());
                item.setConfirmedAt(LocalDateTime.now());
                itemRepo.save(item);
            }
            timeline.add(orderId, "REDUCTION", "减免审核通过，依据：" + nz((String) body.get("basis"), "民政救助政策"));
        } else {
            o.setReductionStatus("REJECTED");
            timeline.add(orderId, "REDUCTION", "减免审核未通过：" + nz((String) body.get("basis"), "不符合救助条件"));
        }
        orderRepo.save(o);
        return o;
    }

    // ============================================================
    // 服务执行 / 结算 / 骨灰领取 / 反馈 / 归档
    // ============================================================

    @Transactional
    public FuneralOrder startService(Long orderId) {
        FuneralOrder o = mustGet(orderId);
        if (!"CONFIRMED".equals(o.getStatus())) throw new BusinessException("方案未经家属签字确认，不能开始服务");
        o.setStatus("IN_SERVICE");
        orderRepo.save(o);
        timeline.add(orderId, "SERVICE", "治丧服务开始执行（接运/冷藏/告别/火化）");
        return o;
    }

    @Transactional
    public FuneralOrder completeService(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        if (!"IN_SERVICE".equals(o.getStatus())) throw new BusinessException("仅服务进行中的订单可以完成服务");
        o.setStatus("COMPLETED");
        orderRepo.save(o);

        ServiceArchive archive = archiveRepo.findByOrderId(orderId).orElseGet(ServiceArchive::new);
        archive.setOrderId(orderId);
        archive.setOrderNo(o.getOrderNo());
        if (body.get("cremationCertNo") != null) archive.setCremationCertNo((String) body.get("cremationCertNo"));
        if (body.get("cremationTime") != null) archive.setCremationTime(parseTime(body.get("cremationTime")));
        if (body.get("furnaceName") != null) archive.setFurnaceName((String) body.get("furnaceName"));
        if (body.get("unresolvedItems") != null) archive.setUnresolvedItems((String) body.get("unresolvedItems"));
        archiveRepo.save(archive);
        // 预占资源随服务结束释放
        bookingRepo.findByOrderId(orderId).forEach(b -> {
            if (!"RELEASED".equals(b.getStatus())) {
                b.setStatus("RELEASED");
                bookingRepo.save(b);
            }
        });
        timeline.add(orderId, "SERVICE", "治丧服务完成，火化证明编号："
                + nz(archive.getCremationCertNo(), "待补登"));
        return o;
    }

    /** 费用单：家属可见每一项来源、确认人、是否可退、减免依据 */
    @Transactional(readOnly = true)
    public Map<String, Object> bill(Long orderId) {
        return buildBill(mustGet(orderId));
    }

    private Map<String, Object> buildBill(FuneralOrder o) {
        List<OrderItem> items = itemRepo.findByOrderIdOrderByCategoryAscIdAsc(o.getId());
        List<OrderItem> valid = items.stream().filter(i -> !"REMOVED".equals(i.getStatus())).toList();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal reduction = BigDecimal.ZERO;
        List<String> reductionBasis = new ArrayList<>();
        for (OrderItem i : valid) {
            if (Constants.CLASS_SUBSIDY.equals(i.getServiceClass())) {
                reduction = reduction.add(i.getSubtotal().negate());
                reductionBasis.add(i.getName() + "：" + nz(i.getSourceNote(), "政府补助"));
            } else {
                total = total.add(i.getSubtotal());
            }
        }
        BigDecimal payable = total.subtract(reduction);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", valid);
        m.put("removedItems", items.stream().filter(i -> "REMOVED".equals(i.getStatus())).toList());
        m.put("totalAmount", total);
        m.put("reductionAmount", reduction);
        m.put("payableAmount", payable.max(BigDecimal.ZERO));
        m.put("paidAmount", o.getPaidAmount());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("reductionStatus", o.getReductionStatus());
        m.put("reductionBasis", reductionBasis);
        m.put("familyConfirmed", o.getFamilyConfirmed());
        return m;
    }

    /** 家属确认费用明细（签字） */
    @Transactional
    public SignatureRecord confirmBill(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        if (!List.of("COMPLETED", "IN_SERVICE", "CONFIRMED").contains(o.getStatus())) {
            throw new BusinessException("当前状态不能确认费用单");
        }
        if ("PENDING".equals(o.getReductionStatus())) {
            throw new BusinessException("低保减免仍在审核中，请审核完成后再确认费用");
        }
        Map<String, Object> bill = buildBill(o);
        SignatureRecord sign = saveSignature(o, "CONFIRM_BILL",
                "确认费用明细：费用合计 " + bill.get("totalAmount") + " 元，政府补助减免 "
                        + bill.get("reductionAmount") + " 元，应交 " + bill.get("payableAmount") + " 元",
                null, body);
        timeline.add(orderId, "SIGN", "家属签字确认费用明细，应交 " + bill.get("payableAmount") + " 元");
        return sign;
    }

    @Transactional
    public Map<String, Object> pay(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        if (List.of("SETTLED", "ARCHIVED").contains(o.getStatus())) {
            throw new BusinessException("订单已结算");
        }
        boolean billSigned = signatureRepo.findByOrderIdOrderBySignedAtDesc(orderId).stream()
                .anyMatch(s -> "CONFIRM_BILL".equals(s.getActionType()));
        if (!billSigned) throw new BusinessException("家属尚未签字确认费用明细，不能收款");
        BigDecimal amount = decimal(body.get("amount"));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException("收款金额必须大于 0");
        BigDecimal paid = nz(o.getPaidAmount()).add(amount);
        BigDecimal payable = (BigDecimal) buildBill(o).get("payableAmount");
        o.setPaidAmount(paid);
        if (paid.compareTo(payable) >= 0) {
            o.setPaymentStatus("PAID");
            o.setStatus("SETTLED");
            o.setSettleTime(LocalDateTime.now());
            o.setSettledById(CurrentUser.id());
            o.setSettledByName(CurrentUser.name());
            // 结算时持久化汇总金额
            Map<String, Object> b = buildBill(o);
            o.setTotalAmount((BigDecimal) b.get("totalAmount"));
            o.setReductionAmount((BigDecimal) b.get("reductionAmount"));
            o.setPayableAmount((BigDecimal) b.get("payableAmount"));
            if (body.get("unresolvedNote") != null) o.setUnresolvedNote((String) body.get("unresolvedNote"));
        } else {
            o.setPaymentStatus("PARTIAL");
            o.setStatus("COMPLETED");
        }
        orderRepo.save(o);
        timeline.add(orderId, "SETTLE", CurrentUser.name() + " 收款 " + amount + " 元，累计 "
                + paid + "/" + payable + " 元" + ("PAID".equals(o.getPaymentStatus()) ? "，订单已结清" : ""));
        return buildBill(o);
    }

    /** 骨灰领取（签字 + 领取人信息） */
    @Transactional
    public ServiceArchive urnClaim(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        SignatureRecord sign = saveSignature(o, "URN_CLAIM",
                "骨灰领取：" + str(body, "urnClaimantName", "领取人姓名不能为空")
                        + "（" + nz((String) body.get("urnClaimantRelation"), "亲属") + "）凭证件领取",
                null, body);
        ServiceArchive a = archiveRepo.findByOrderId(orderId).orElseGet(ServiceArchive::new);
        a.setOrderId(orderId);
        a.setOrderNo(o.getOrderNo());
        a.setUrnClaimantName((String) body.get("urnClaimantName"));
        a.setUrnClaimantPhone((String) body.get("urnClaimantPhone"));
        a.setUrnClaimantRelation((String) body.get("urnClaimantRelation"));
        a.setUrnClaimantIdNo((String) body.get("urnClaimantIdNo"));
        a.setUrnClaimTime(LocalDateTime.now());
        archiveRepo.save(a);
        timeline.add(orderId, "URN_CLAIM", "骨灰由 " + a.getUrnClaimantName() + " 签字领取（签字记录#"
                + sign.getId() + "）");
        return a;
    }

    @Transactional
    public ServiceArchive feedback(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        int rating = body.get("rating") == null ? 5 : Math.max(1, Math.min(5, ((Number) body.get("rating")).intValue()));
        String content = nz((String) body.get("content"), "");
        o.setFeedbackRating(rating);
        o.setFeedbackContent(content);
        o.setFeedbackTime(LocalDateTime.now());
        orderRepo.save(o);

        ServiceArchive a = archiveRepo.findByOrderId(orderId).orElseGet(ServiceArchive::new);
        a.setOrderId(orderId);
        a.setOrderNo(o.getOrderNo());
        a.setFeedbackRating(rating);
        a.setFeedbackContent(content);
        archiveRepo.save(a);
        timeline.add(orderId, "FEEDBACK", "家属反馈：" + rating + " 星 " + abbreviate(content, 100));
        return a;
    }

    @Transactional
    public ServiceArchive archive(Long orderId, Map<String, Object> body) {
        FuneralOrder o = mustGet(orderId);
        if (!"SETTLED".equals(o.getStatus())) throw new BusinessException("仅已结清订单可以归档");
        Map<String, Object> bill = buildBill(o);
        ServiceArchive a = archiveRepo.findByOrderId(orderId).orElseGet(ServiceArchive::new);
        a.setOrderId(orderId);
        a.setOrderNo(o.getOrderNo());
        a.setFeeSnapshot(catalogSnapshot(bill));
        a.setReductionBasis(String.join("；", (List<String>) bill.get("reductionBasis")));
        if (body.get("unresolvedItems") != null) {
            a.setUnresolvedItems((String) body.get("unresolvedItems"));
            o.setUnresolvedNote((String) body.get("unresolvedItems"));
        }
        a.setFeedbackRating(o.getFeedbackRating());
        a.setFeedbackContent(o.getFeedbackContent());
        a.setArchivedById(CurrentUser.id());
        a.setArchivedByName(CurrentUser.name());
        a.setArchivedAt(LocalDateTime.now());
        archiveRepo.save(a);
        o.setStatus("ARCHIVED");
        orderRepo.save(o);
        timeline.add(orderId, "ARCHIVE", "服务档案归档完成：费用明细、减免依据、火化证明、骨灰领取记录、反馈均已入档");
        return a;
    }

    private String catalogSnapshot(Map<String, Object> bill) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : (List<?>) bill.get("items")) {
            OrderItem i = (OrderItem) obj;
            sb.append("[").append(classText(i.getServiceClass())).append("] ")
                    .append(i.getName()).append(" ×").append(i.getQuantity())
                    .append("，单价").append(i.getUnitPrice())
                    .append("，小计").append(i.getSubtotal())
                    .append("，来源：").append(nz(i.getSourceNote(), "-"))
                    .append("，确认人：").append(nz(i.getConfirmedByName(), "-"))
                    .append("，").append(i.getRefundable() ? "可退" : "不可退")
                    .append("\n");
        }
        sb.append("费用合计：").append(bill.get("totalAmount"))
                .append("；补助减免：").append(bill.get("reductionAmount"))
                .append("；实收：").append(bill.get("payableAmount"));
        return sb.toString();
    }

    // ============================================================
    // 通用：签字保存
    // ============================================================

    private SignatureRecord saveSignature(FuneralOrder o, String action, String summary,
                                          Long itemId, Map<String, Object> body) {
        String signData = (String) body.get("signatureData");
        if (signData == null || !signData.startsWith("data:image/") || signData.length() < 300) {
            throw new BusinessException("请家属在签名板手写签名后再提交");
        }
        SignatureRecord s = new SignatureRecord();
        s.setOrderId(o.getId());
        s.setOrderItemId(itemId);
        s.setActionType(action);
        s.setContentSummary(summary);
        s.setSignerName(str(body, "signerName", "请填写签字家属姓名"));
        s.setSignerPhone((String) body.get("signerPhone"));
        s.setSignerRelation((String) body.get("signerRelation"));
        s.setSignatureData(signData);
        s.setWitnessId(CurrentUser.id());
        s.setWitnessName(CurrentUser.name());
        signatureRepo.save(s);
        return s;
    }

    // ============================================================
    // helpers
    // ============================================================

    public FuneralOrder mustGet(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new BusinessException("治丧单不存在：" + id));
    }

    private void requireStage(FuneralOrder o, String... stages) {
        if (List.of("CANCELLED", "ARCHIVED").contains(o.getStatus())) {
            throw new BusinessException("订单已结束，不能变更");
        }
    }

    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rnd = String.format("%03d", new Random().nextInt(1000));
        String no = "FY" + ts + rnd;
        return orderRepo.existsByOrderNo(no) ? no + "X" : no;
    }

    private static String str(Map<String, Object> b, String k, String err) {
        Object v = b.get(k);
        if (v == null || v.toString().isBlank()) throw new BusinessException(err);
        return v.toString();
    }

    private static String nz(String s, String def) { return s == null || s.isBlank() ? def : s; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static boolean bool(Map<String, Object> b, String k) { return Boolean.TRUE.equals(b.get(k)); }
    private static Long id(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }
    private static Integer intOrNull(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        return ((Number) v).intValue();
    }
    private static BigDecimal decimal(Object v) {
        if (v == null || v.toString().isBlank()) return BigDecimal.ZERO;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }

    static LocalDateTime parseTime(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        String s = v.toString().trim();
        try {
            if (s.length() == 16) return LocalDateTime.parse(s.replace(" ", "T"));
            if (s.length() == 10) return java.time.LocalDate.parse(s).atStartOfDay();
            return LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s.replace(" ", "T"));
        } catch (Exception e) {
            throw new BusinessException("时间格式不正确：" + s + "（应为 yyyy-MM-dd HH:mm）");
        }
    }

    private static LocalDateTime plusHours(LocalDateTime t, long h) { return t == null ? null : t.plusHours(h); }
    private static LocalDateTime minusHours(LocalDateTime t, long h) { return t == null ? null : t.minusHours(h); }
    private static LocalDateTime plusMinutes(LocalDateTime t, long m) { return t == null ? null : t.plusMinutes(m); }
    private static LocalDateTime minusMinutes(LocalDateTime t, long m) { return t == null ? null : t.minusMinutes(m); }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public static String classText(String c) {
        return switch (c) {
            case "PUBLIC_BASIC" -> "公益基本服务";
            case "OPTIONAL" -> "自选增值服务";
            case "SUBSIDY" -> "政府补助项目";
            default -> c;
        };
    }

    public static String roleText(String r) {
        return switch (r) {
            case "FAMILY" -> "家属";
            case "TRANSPORT" -> "接运组";
            case "CLERK" -> "业务员";
            case "FINANCE" -> "财务";
            case "HALL_ADMIN" -> "礼厅管理员";
            case "LEADER" -> "馆领导";
            default -> r;
        };
    }

    public static String commTypeText(String t) {
        return switch (t) {
            case "DISPUTE" -> "意见不一致";
            case "EMOTIONAL" -> "情绪激动";
            case "TEMP_CHANGE" -> "现场临时变更";
            case "OTHER" -> "其他";
            default -> "普通沟通";
        };
    }
}
