package com.toolshare.dto.tool;

import com.toolshare.entity.ToolStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResponse {
    private Long id;
    private Long boxId;
    private String boxName;
    private String name;
    private String category;
    private ToolStatus status;
    private String description;
    private String image;
    private LocalDate purchaseDate;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private Double averageRating;
    private Long reviewCount;
}
