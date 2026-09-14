package com.funeral.controller;

import com.funeral.common.ApiResponse;
import com.funeral.entity.ServiceArchive;
import com.funeral.repo.ServiceArchiveRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/archives")
public class ArchiveController {

    private final ServiceArchiveRepository archiveRepo;

    public ArchiveController(ServiceArchiveRepository archiveRepo) {
        this.archiveRepo = archiveRepo;
    }

    @GetMapping
    public ApiResponse<List<ServiceArchive>> list() {
        return ApiResponse.ok(archiveRepo.findAllByOrderByArchivedAtDesc());
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceArchive> one(@PathVariable Long id) {
        return ApiResponse.ok(archiveRepo.findById(id).orElse(null));
    }
}
