package org.chatapp.customshopify.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {
    private Long totalReviews;
    private Double averageRating;
    private Long oneStar;
    private Long twoStars;
    private Long threeStars;
    private Long fourStars;
    private Long fiveStars;
    private Long unModeratedCount;
}
