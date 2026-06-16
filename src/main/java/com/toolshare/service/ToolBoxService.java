package com.toolshare.service;

import com.toolshare.dto.PageResponse;
import com.toolshare.dto.toolbox.CreateToolBoxRequest;
import com.toolshare.dto.toolbox.ToolBoxResponse;
import com.toolshare.dto.toolbox.UpdateToolBoxRequest;
import com.toolshare.entity.ToolBox;
import com.toolshare.entity.User;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ResourceNotFoundException;
import com.toolshare.repository.ToolBoxRepository;
import com.toolshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolBoxService {

    private final ToolBoxRepository toolBoxRepository;
    private final UserRepository userRepository;

    public ToolBoxService(ToolBoxRepository toolBoxRepository, UserRepository userRepository) {
        this.toolBoxRepository = toolBoxRepository;
        this.userRepository = userRepository;
    }

    public PageResponse<ToolBoxResponse> getAllToolBoxes(String keyword, Boolean isActive, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ToolBox> toolBoxPage = toolBoxRepository.search(keyword, isActive, pageable);
        Page<ToolBoxResponse> responsePage = toolBoxPage.map(this::toResponse);

        return PageResponse.from(responsePage);
    }

    public ToolBoxResponse getToolBoxById(Long id) {
        ToolBox toolBox = toolBoxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具箱不存在"));
        return toResponse(toolBox);
    }

    @Transactional
    public ToolBoxResponse createToolBox(CreateToolBoxRequest request, Long managerId) {
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            if (toolBoxRepository.existsByCode(request.getCode())) {
                throw new BadRequestException("工具箱编码已存在：" + request.getCode());
            }
        }

        ToolBox toolBox = new ToolBox();
        toolBox.setName(request.getName());
        toolBox.setLocation(request.getLocation());
        toolBox.setCode(request.getCode());
        toolBox.setImage(request.getImage());
        toolBox.setManagerId(managerId);
        toolBox.setIsActive(true);

        ToolBox savedToolBox = toolBoxRepository.save(toolBox);
        return toResponse(savedToolBox);
    }

    @Transactional
    public ToolBoxResponse updateToolBox(Long id, UpdateToolBoxRequest request, Long currentUserId) {
        ToolBox toolBox = toolBoxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具箱不存在"));

        if (!toolBox.getManagerId().equals(currentUserId)) {
            throw new BadRequestException("无权修改此工具箱");
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            if (!request.getCode().equals(toolBox.getCode()) && toolBoxRepository.existsByCode(request.getCode())) {
                throw new BadRequestException("工具箱编码已存在：" + request.getCode());
            }
        }

        if (request.getName() != null) {
            toolBox.setName(request.getName());
        }
        if (request.getLocation() != null) {
            toolBox.setLocation(request.getLocation());
        }
        if (request.getCode() != null) {
            toolBox.setCode(request.getCode());
        }
        if (request.getImage() != null) {
            toolBox.setImage(request.getImage());
        }
        if (request.getIsActive() != null) {
            toolBox.setIsActive(request.getIsActive());
        }

        ToolBox savedToolBox = toolBoxRepository.save(toolBox);
        return toResponse(savedToolBox);
    }

    @Transactional
    public void deleteToolBox(Long id, Long currentUserId) {
        ToolBox toolBox = toolBoxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工具箱不存在"));

        if (!toolBox.getManagerId().equals(currentUserId)) {
            throw new BadRequestException("无权删除此工具箱");
        }

        toolBoxRepository.delete(toolBox);
    }

    private ToolBoxResponse toResponse(ToolBox toolBox) {
        ToolBoxResponse response = new ToolBoxResponse();
        response.setId(toolBox.getId());
        response.setName(toolBox.getName());
        response.setLocation(toolBox.getLocation());
        response.setManagerId(toolBox.getManagerId());
        response.setCode(toolBox.getCode());
        response.setImage(toolBox.getImage());
        response.setIsActive(toolBox.getIsActive());
        response.setCreatedAt(toolBox.getCreatedAt());

        userRepository.findById(toolBox.getManagerId()).ifPresent(user ->
                response.setManagerName(user.getUsername())
        );

        return response;
    }

    public boolean isToolBoxManager(Long toolBoxId, Long userId) {
        return toolBoxRepository.findById(toolBoxId)
                .map(toolBox -> toolBox.getManagerId().equals(userId))
                .orElse(false);
    }
}
