package com.toolshare.dto.scan;

import com.toolshare.entity.ToolStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanToolItem {
    private Long id;
    private String name;
    private String category;
    private ToolStatus status;
    private String description;
    private String image;
    private LocalDate purchaseDate;
    private Long ownerId;
    private String ownerName;
    private Long currentBorrowerId;
    private String currentBorrowerName;
    private LocalDate expectedReturnDate;
}
