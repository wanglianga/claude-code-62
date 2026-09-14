package com.funeral.service;

import com.funeral.common.CurrentUser;
import com.funeral.entity.OrderTimeline;
import com.funeral.repo.OrderTimelineRepository;
import org.springframework.stereotype.Service;

@Service
public class TimelineService {

    private final OrderTimelineRepository timelineRepo;

    public TimelineService(OrderTimelineRepository timelineRepo) {
        this.timelineRepo = timelineRepo;
    }

    public void add(Long orderId, String eventType, String content) {
        OrderTimeline t = new OrderTimeline();
        t.setOrderId(orderId);
        t.setEventType(eventType);
        t.setContent(content);
        t.setActorId(CurrentUser.id());
        t.setActorName(CurrentUser.name());
        t.setActorRole(CurrentUser.role());
        timelineRepo.save(t);
    }

    public void addAsSystem(Long orderId, String eventType, String content) {
        OrderTimeline t = new OrderTimeline();
        t.setOrderId(orderId);
        t.setEventType(eventType);
        t.setContent(content);
        t.setActorName("系统");
        t.setActorRole("SYSTEM");
        timelineRepo.save(t);
    }
}
