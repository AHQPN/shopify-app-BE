package org.chatapp.customshopify.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetafieldUpdateInput {
    private String ownerId;
    private String namespace;
    private String key;
    private String type;
    private Double value;
}
