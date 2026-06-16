package com.toolshare.repository;

import com.toolshare.entity.ToolBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolBoxRepository extends JpaRepository<ToolBox, Long> {
    Page<ToolBox> findByManagerId(Long managerId, Pageable pageable);

    @Query("SELECT tb FROM ToolBox tb WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR tb.name LIKE %:keyword% OR tb.location LIKE %:keyword%) " +
           "AND (:isActive IS NULL OR tb.isActive = :isActive)")
    Page<ToolBox> search(@Param("keyword") String keyword,
                         @Param("isActive") Boolean isActive,
                         Pageable pageable);

    List<ToolBox> findByManagerId(Long managerId);

    java.util.Optional<ToolBox> findByCode(String code);

    boolean existsByCode(String code);
}
