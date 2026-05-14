package com.ekokoy.portal.announcement.dto;

import com.ekokoy.portal.announcement.entity.AttachmentFileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddAttachmentRequest(
        @NotBlank @Size(max = 512) String fileUrl,
        @Size(max = 255) String fileName,
        @NotNull AttachmentFileType fileType,
        Integer fileSize,
        int displayOrder
) {}
