package com.funeral.controller;

import com.funeral.common.ApiResponse;
import com.funeral.entity.CatalogItem;
import com.funeral.repo.CatalogItemRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogItemRepository catalogRepo;

    public CatalogController(CatalogItemRepository catalogRepo) {
        this.catalogRepo = catalogRepo;
    }

    @GetMapping
    public ApiResponse<List<CatalogItem>> list() {
        return ApiResponse.ok(catalogRepo.findByActiveTrueOrderByServiceClassAscCategoryAscIdAsc());
    }

    /** 三类服务分组（公益基本 / 自选增值 / 政府补助） */
    @GetMapping("/groups")
    public ApiResponse<Map<String, List<CatalogItem>>> groups() {
        Map<String, List<CatalogItem>> map = new LinkedHashMap<>();
        map.put("PUBLIC_BASIC", catalogRepo.findByActiveTrueOrderByServiceClassAscCategoryAscIdAsc()
                .stream().filter(c -> "PUBLIC_BASIC".equals(c.getServiceClass())).toList());
        map.put("OPTIONAL", catalogRepo.findByActiveTrueOrderByServiceClassAscCategoryAscIdAsc()
                .stream().filter(c -> "OPTIONAL".equals(c.getServiceClass())).toList());
        map.put("SUBSIDY", catalogRepo.findByActiveTrueOrderByServiceClassAscCategoryAscIdAsc()
                .stream().filter(c -> "SUBSIDY".equals(c.getServiceClass())).toList());
        return ApiResponse.ok(map);
    }
}
