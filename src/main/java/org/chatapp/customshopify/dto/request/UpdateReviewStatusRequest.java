package org.chatapp.customshopify.dto.request;

import lombok.Data;
import org.chatapp.customshopify.enums.HideReason;

@Data
public class UpdateReviewStatusRequest {
    private Long id;
    private Boolean status;
    private HideReason hideReason;
}
