package com.toolshare.repository;

import com.toolshare.entity.BorrowRequest;
import com.toolshare.entity.BorrowRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    Page<BorrowRequest> findByRequesterId(Long requesterId, Pageable pageable);

    Page<BorrowRequest> findByToolId(Long toolId, Pageable pageable);

    @Query("SELECT br FROM BorrowRequest br WHERE " +
           "(:status IS NULL OR br.status = :status) " +
           "AND (:requesterId IS NULL OR br.requesterId = :requesterId) " +
           "AND (:toolId IS NULL OR br.toolId = :toolId) " +
           "AND (:startDate IS NULL OR br.startDate >= :startDate) " +
           "AND (:endDate IS NULL OR br.expectedReturnDate <= :endDate)")
    Page<BorrowRequest> search(@Param("status") BorrowRequestStatus status,
                               @Param("requesterId") Long requesterId,
                               @Param("toolId") Long toolId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               Pageable pageable);

    long countByStatus(BorrowRequestStatus status);

    @Query("SELECT DATE(br.createdAt), COUNT(br) FROM BorrowRequest br " +
           "WHERE br.createdAt >= :startDate AND br.createdAt <= :endDate " +
           "GROUP BY DATE(br.createdAt) ORDER BY DATE(br.createdAt)")
    List<Object[]> countByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT br FROM BorrowRequest br WHERE br.status = :status " +
           "AND br.expectedReturnDate BETWEEN :fromDate AND :toDate " +
           "AND br.dueSoonNotified = false")
    Page<BorrowRequest> findDueSoonBorrows(@Param("status") BorrowRequestStatus status,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           Pageable pageable);

    @Query("SELECT br FROM BorrowRequest br WHERE br.status = :status " +
           "AND br.expectedReturnDate < :today " +
           "AND br.actualReturnDate IS NULL")
    Page<BorrowRequest> findOverdueBorrows(@Param("status") BorrowRequestStatus status,
                                           @Param("today") LocalDate today,
                                           Pageable pageable);

    long countByStatusAndExpectedReturnDateBeforeAndActualReturnDateIsNull(
            BorrowRequestStatus status, LocalDate date);

    @Query("SELECT br FROM BorrowRequest br WHERE br.toolId = :toolId AND br.status = :status AND br.actualReturnDate IS NULL")
    java.util.Optional<BorrowRequest> findActiveBorrowByToolId(@Param("toolId") Long toolId, @Param("status") BorrowRequestStatus status);

    @Query("SELECT br FROM BorrowRequest br WHERE br.toolId IN :toolIds AND br.status = :status AND br.actualReturnDate IS NULL")
    List<BorrowRequest> findActiveBorrowsByToolIds(@Param("toolIds") List<Long> toolIds, @Param("status") BorrowRequestStatus status);
}
