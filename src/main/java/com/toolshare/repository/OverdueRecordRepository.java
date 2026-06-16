package com.toolshare.repository;

import com.toolshare.entity.OverdueRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface OverdueRecordRepository extends JpaRepository<OverdueRecord, Long> {

    Optional<OverdueRecord> findByBorrowRequestId(Long borrowRequestId);

    Page<OverdueRecord> findByRequesterId(Long requesterId, Pageable pageable);

    Page<OverdueRecord> findByResolved(boolean resolved, Pageable pageable);

    Page<OverdueRecord> findByRequesterIdAndResolved(Long requesterId, boolean resolved, Pageable pageable);

    long countByResolved(boolean resolved);

    @Query("SELECT COUNT(o) FROM OverdueRecord o WHERE o.overdueDate BETWEEN :startDate AND :endDate")
    long countByOverdueDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT o.requesterId, COUNT(o) FROM OverdueRecord o GROUP BY o.requesterId ORDER BY COUNT(o) DESC")
    Page<Object[]> countByRequester(Pageable pageable);
}
