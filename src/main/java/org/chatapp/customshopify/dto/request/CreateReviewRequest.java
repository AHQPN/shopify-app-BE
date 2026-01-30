package org.chatapp.customshopify.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateReviewRequest {
    private String productId;
    private String customerId; // Optional from FE
    private String customerName;
    private String productName;
    private String avatarUrl;
    private String comment;
    private Integer rating;
    private Long replyTo;
    private Boolean isAnonymous;
    private String captchaToken;
    private List<MediaItem> media;

    @Data
    public static class MediaItem {
        private String url;
        private String type; // IMAGE, VIDEO
        private Long fileSize;
    }
}
