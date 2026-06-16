package com.toolshare.repository;

import com.toolshare.entity.ToolReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ToolReviewRepository extends JpaRepository<ToolReview, Long> {

    Page<ToolReview> findByToolId(Long toolId, Pageable pageable);

    Page<ToolReview> findByReviewerId(Long reviewerId, Pageable pageable);

    Optional<ToolReview> findByBorrowRequestId(Long borrowRequestId);

    @Query("SELECT AVG(tr.rating) FROM ToolReview tr WHERE tr.toolId = :toolId")
    Double findAverageRatingByToolId(@Param("toolId") Long toolId);

    @Query("SELECT COUNT(tr) FROM ToolReview tr WHERE tr.toolId = :toolId")
    Long countByToolId(@Param("toolId") Long toolId);

    boolean existsByBorrowRequestId(Long borrowRequestId);

    @Query("SELECT tr.toolId, AVG(tr.rating) FROM ToolReview tr WHERE tr.toolId IN :toolIds GROUP BY tr.toolId")
    List<Object[]> findAverageRatingsByToolIds(@Param("toolIds") List<Long> toolIds);

    @Query("SELECT tr.toolId, COUNT(tr) FROM ToolReview tr WHERE tr.toolId IN :toolIds GROUP BY tr.toolId")
    List<Object[]> countByToolIds(@Param("toolIds") List<Long> toolIds);

    @Query("SELECT tr.borrowRequestId FROM ToolReview tr WHERE tr.borrowRequestId IN :borrowRequestIds")
    List<Long> findReviewedBorrowRequestIds(@Param("borrowRequestIds") List<Long> borrowRequestIds);

    default Map<Long, Double> findAverageRatingMapByToolIds(List<Long> toolIds) {
        return findAverageRatingsByToolIds(toolIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));
    }

    default Map<Long, Long> findReviewCountMapByToolIds(List<Long> toolIds) {
        return countByToolIds(toolIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
