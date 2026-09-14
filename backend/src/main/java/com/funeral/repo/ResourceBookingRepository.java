package com.funeral.repo;

import com.funeral.entity.ResourceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceBookingRepository extends JpaRepository<ResourceBooking, Long> {

    List<ResourceBooking> findByOrderId(Long orderId);

    /** 查询某资源在给定时间段内处于占用（预占/确认）的记录 */
    @Query("""
            select b from ResourceBooking b
            where b.resourceType = :type and b.resourceId = :rid
              and b.status in ('HELD','CONFIRMED')
              and b.startAt < :end and b.endAt > :start
              and (:excludeOrderId is null or b.orderId <> :excludeOrderId)
            order by b.startAt
            """)
    List<ResourceBooking> findConflicts(@Param("type") String type,
                                        @Param("rid") Long resourceId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("excludeOrderId") Long excludeOrderId);

    /** 查询同类型其他资源在该时间段是否空闲 */
    @Query("""
            select b from ResourceBooking b
            where b.resourceType = :type
              and b.status in ('HELD','CONFIRMED')
              and b.startAt < :end and b.endAt > :start
            """)
    List<ResourceBooking> findBusyOfType(@Param("type") String type,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}
