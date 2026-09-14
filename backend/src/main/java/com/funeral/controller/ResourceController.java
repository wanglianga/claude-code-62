package com.funeral.controller;

import com.funeral.common.ApiResponse;
import com.funeral.common.BusinessException;
import com.funeral.common.CurrentUser;
import com.funeral.entity.Resource;
import com.funeral.repo.ResourceRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepo;

    public ResourceController(ResourceRepository resourceRepo) {
        this.resourceRepo = resourceRepo;
    }

    @GetMapping
    public ApiResponse<List<Resource>> list(@RequestParam String type) {
        return ApiResponse.ok(resourceRepo.findByTypeOrderByIdAsc(type));
    }

    /** 馆领导/礼厅管理员：设置资源检修停用（如火化设备检修） */
    @PutMapping("/{id}/availability")
    public ApiResponse<Resource> setAvailability(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Resource r = resourceRepo.findById(id).orElseThrow(() -> new BusinessException("资源不存在"));
        boolean available = Boolean.TRUE.equals(body.get("available"));
        r.setAvailable(available);
        r.setNote((String) body.getOrDefault("note", r.getNote()));
        if (body.get("unavailableUntil") != null && !available) {
            r.setUnavailableUntil(LocalDateTime.parse(((String) body.get("unavailableUntil"))
                    .replace(" ", "T")));
        }
        if (available) r.setUnavailableUntil(null);
        resourceRepo.save(r);
        return ApiResponse.ok(r);
    }

    @GetMapping("/all")
    public ApiResponse<List<Resource>> all() {
        return ApiResponse.ok(resourceRepo.findAll());
    }
}
