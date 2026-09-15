package com.funeral.controller;

import com.funeral.common.ApiResponse;
import com.funeral.entity.CrossRegionTransport;
import com.funeral.service.CrossRegionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/{id}/cross-region")
public class CrossRegionController {

    private final CrossRegionService crossRegionService;

    public CrossRegionController(CrossRegionService crossRegionService) {
        this.crossRegionService = crossRegionService;
    }

    /** 登记异地/跨县接运信息（死亡地、当地联系人、证明机构、车辆司机、ETA、冷藏习俗、随行亲属） */
    @PostMapping("/register")
    public ApiResponse<CrossRegionTransport> register(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(crossRegionService.register(id, body));
    }

    /** 核验死亡证明/接运许可/车辆资质/冷藏条件/接收能力，并关联火化排期与礼厅；异常暂停锁定 */
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(crossRegionService.verify(id, body));
    }

    /** 核验通过后发车（在途） */
    @PostMapping("/depart")
    public ApiResponse<CrossRegionTransport> depart(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(crossRegionService.depart(id, body == null ? Map.of() : body));
    }

    /** 车辆延误：暂停连锁锁定，通知联系人改期，发起协同 */
    @PostMapping("/delay")
    public ApiResponse<CrossRegionTransport> delay(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(crossRegionService.reportDelay(id, body));
    }

    /** 到馆回写：车辆交接、冷藏入库、证明复核、礼厅/火化排期确认 */
    @PostMapping("/arrive")
    public ApiResponse<CrossRegionTransport> arrive(@PathVariable Long id,
                                                    @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(crossRegionService.arrive(id, body));
    }
}
