package org.chatapp.customshopify.dto.request;

import lombok.Data;
import org.chatapp.customshopify.enums.HideReason;
import org.chatapp.customshopify.enums.ReviewStatus;

@Data
public class UpdateReviewStatusRequest {
    private Long id;
    ReviewStatus status;
    private HideReason hideReason;
}
