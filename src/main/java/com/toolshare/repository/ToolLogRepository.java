package com.toolshare.repository;

import com.toolshare.entity.ToolLog;
import com.toolshare.entity.ToolLogAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ToolLogRepository extends JpaRepository<ToolLog, Long> {
    Page<ToolLog> findByToolId(Long toolId, Pageable pageable);

    Page<ToolLog> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT tl FROM ToolLog tl WHERE " +
           "(:toolId IS NULL OR tl.toolId = :toolId) " +
           "AND (:userId IS NULL OR tl.userId = :userId) " +
           "AND (:action IS NULL OR tl.action = :action) " +
           "AND (:startTime IS NULL OR tl.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR tl.createdAt <= :endTime)")
    Page<ToolLog> search(@Param("toolId") Long toolId,
                         @Param("userId") Long userId,
                         @Param("action") ToolLogAction action,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime,
                         Pageable pageable);

    @Query("SELECT tl.action, COUNT(tl) FROM ToolLog tl GROUP BY tl.action")
    List<Object[]> countByAction();
}
