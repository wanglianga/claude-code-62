package com.funeral.controller;

import com.funeral.common.ApiResponse;
import com.funeral.common.CurrentUser;
import com.funeral.entity.CollaborationTask;
import com.funeral.repo.CollaborationTaskRepository;
import com.funeral.repo.FuneralOrderRepository;
import com.funeral.repo.ServiceArchiveRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 各角色工作台看板 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final FuneralOrderRepository orderRepo;
    private final CollaborationTaskRepository collabRepo;
    private final ServiceArchiveRepository archiveRepo;

    public DashboardController(FuneralOrderRepository orderRepo, CollaborationTaskRepository collabRepo,
                               ServiceArchiveRepository archiveRepo) {
        this.orderRepo = orderRepo;
        this.collabRepo = collabRepo;
        this.archiveRepo = archiveRepo;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard() {
        String role = CurrentUser.role();
        List<CollaborationTask> openTasks;
        if ("FAMILY".equals(role)) {
            openTasks = List.of();
        } else {
            // 馆领导看全部协同，其他岗位看本角色待办
            openTasks = "LEADER".equals(role)
                    ? collabRepo.findByStatusNotOrderByPriorityAscCreatedAtDesc("RESOLVED")
                    : collabRepo.findByAssigneeRoleAndStatusNotOrderByPriorityAscCreatedAtDesc(role, "RESOLVED");
        }

        Map<String, Object> m = new LinkedHashMap<>();
        var orders = orderRepo.findAllByOrderByCreatedAtDesc();
        m.put("totalOrders", orders.size());
        m.put("statusGroups", buildStatusGroups(orders));
        m.put("myCollaborations", openTasks);
        m.put("openCollaborationCount", openTasks.size());
        m.put("archiveCount", archiveRepo.count());
        m.put("recentOrders", orders.stream().limit(10).toList());
        return ApiResponse.ok(m);
    }

    private Map<String, Long> buildStatusGroups(List<com.funeral.entity.FuneralOrder> orders) {
        Map<String, Long> groups = new LinkedHashMap<>();
        for (String s : List.of("RESOURCE_VERIFYING", "VERIFIED", "NEGOTIATING", "CONFIRMED",
                "IN_SERVICE", "COMPLETED", "SETTLED", "ARCHIVED")) {
            groups.put(s, orders.stream().filter(o -> s.equals(o.getStatus())).count());
        }
        return groups;
    }
}
