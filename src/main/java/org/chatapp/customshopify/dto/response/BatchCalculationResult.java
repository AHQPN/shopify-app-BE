package org.chatapp.customshopify.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCalculationResult {
    private int updated;
    private int failed;
    private int skipped;
    private int total;
}
