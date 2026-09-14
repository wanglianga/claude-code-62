package com.funeral.repo;

import com.funeral.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findByActiveTrueOrderByServiceClassAscCategoryAscIdAsc();
}
