package org.chatapp.customshopify.dto.response;

import lombok.Data;
import java.util.List;

public class ShopifyGraphQLResponses {

    // --- Products Query Response ---
    @Data
    public static class ProductsQueryRoot {
        private ProductsData data;
    }

    @Data
    public static class ProductsData {
        private ProductConnection products;
    }

    @Data
    public static class ProductConnection {
        private PageInfo pageInfo;
        private List<ProductEdge> edges;
    }

    @Data
    public static class PageInfo {
        private boolean hasNextPage;
        private String endCursor;
    }

    @Data
    public static class ProductEdge {
        private ProductNode node;
    }

    @Data
    public static class ProductNode {
        private String id;
        private String title;
        private VariantConnection variants;
    }

    @Data
    public static class VariantConnection {
        private List<VariantEdge> edges;
    }

    @Data
    public static class VariantEdge {
        private VariantNode node;
    }

    @Data
    public static class VariantNode {
        private String price;
        private String compareAtPrice;
    }

    // --- MetafieldsSet Mutation Response ---
    @Data
    public static class MetafieldsSetRoot {
        private MetafieldsSetData data;
    }

    @Data
    public static class MetafieldsSetData {
        private MetafieldsSetPayload metafieldsSet;
    }

    @Data
    public static class MetafieldsSetPayload {
        private List<Metafield> metafields;
        private List<UserError> userErrors;
    }

    @Data
    public static class Metafield {
        private String id;
        private String key;
        private String value;
    }

    // --- MetafieldDefinitionCreate Mutation Response ---
    @Data
    public static class MetafieldDefinitionCreateRoot {
        private MetafieldDefinitionCreateData data;
    }

    @Data
    public static class MetafieldDefinitionCreateData {
        private MetafieldDefinitionCreatePayload metafieldDefinitionCreate;
    }

    @Data
    public static class MetafieldDefinitionCreatePayload {
        private CreatedDefinition createdDefinition;
        private List<UserError> userErrors;
    }

    @Data
    public static class CreatedDefinition {
        private String id;
        private String name;
    }

    @Data
    public static class UserError {
        private List<String> field;
        private String message;
        private String code;
    }
}
