package com.toolshare.repository;

import com.toolshare.entity.Tool;
import com.toolshare.entity.ToolStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
    Page<Tool> findByBoxId(Long boxId, Pageable pageable);

    Page<Tool> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT t FROM Tool t WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR t.name LIKE %:keyword% OR t.description LIKE %:keyword%) " +
           "AND (:category IS NULL OR :category = '' OR t.category = :category) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:boxId IS NULL OR t.boxId = :boxId)")
    Page<Tool> search(@Param("keyword") String keyword,
                      @Param("category") String category,
                      @Param("status") ToolStatus status,
                      @Param("boxId") Long boxId,
                      Pageable pageable);

    long countByStatus(ToolStatus status);

    long countByBoxId(Long boxId);

    @Query("SELECT t.category, COUNT(t) FROM Tool t GROUP BY t.category")
    List<Object[]> countByCategory();

    List<Tool> findByBoxId(Long boxId);
}
