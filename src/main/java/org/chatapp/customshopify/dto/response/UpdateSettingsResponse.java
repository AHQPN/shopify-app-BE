package org.chatapp.customshopify.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsResponse {
    private boolean success;
    private Boolean discountFeatureEnabled;
    private BatchCalculationResult calculationResult;
    private String warning;
}
