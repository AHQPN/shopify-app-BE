package org.chatapp.customshopify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.client.ShopifyGraphQLClient;
import org.chatapp.customshopify.config.ShopifyConfig;
import org.chatapp.customshopify.entity.ShopifySession;
import org.chatapp.customshopify.repository.AppSettingsRepository;
import org.chatapp.customshopify.repository.ShopifySessionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import org.chatapp.customshopify.dto.model.ProductDTO;
import org.chatapp.customshopify.dto.model.VariantDTO;
import org.chatapp.customshopify.dto.request.MetafieldUpdateInput;
import org.chatapp.customshopify.dto.response.BatchCalculationResult;
import org.chatapp.customshopify.dto.response.ShopifyGraphQLResponses;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final AppSettingsRepository settingsRepository;
    private final ShopifySessionRepository sessionRepository;
    private final ShopifyConfig shopifyConfig;
    private final ShopifyGraphQLClient graphQLClient;

    // Clear all discounts (set metafield to 0) for a shop
    public BatchCalculationResult clearAllDiscounts(String shop, String accessToken) {
        log.info("Clearing discounts for shop: {}", shop);

        // Fetch ALL products with variants for discount calculation
        List<ProductDTO> products = fetchAllProducts(shop, accessToken, true);

        List<MetafieldUpdateInput> batchUpdates = new ArrayList<>();
        int totalUpdated = 0;
        int totalFailed = 0;

        for (ProductDTO product : products) {
            batchUpdates.add(MetafieldUpdateInput.builder()
                    .ownerId(product.getId())
                    .namespace("custom")
                    .key("discount_percentage")
                    .type("number_decimal")
                    .value(0.0)
                    .build());

            if (batchUpdates.size() >= 25) {
                boolean success = sendBatchMetafieldUpdate(shop, accessToken, batchUpdates);
                if (success)
                    totalUpdated += batchUpdates.size();
                else
                    totalFailed += batchUpdates.size();
                batchUpdates.clear();
            }
        }

        if (!batchUpdates.isEmpty()) {
            boolean success = sendBatchMetafieldUpdate(shop, accessToken, batchUpdates);
            if (success)
                totalUpdated += batchUpdates.size();
            else
                totalFailed += batchUpdates.size();
        }

        log.info("Clear complete: {} cleared, {} failed", totalUpdated, totalFailed);

        return BatchCalculationResult.builder()
                .updated(totalUpdated)
                .failed(totalFailed)
                .skipped(0)
                .total(products.size())
                .build();
    }

    public BatchCalculationResult calculateAllDiscounts(String shop, String accessToken) {
        log.info("Calculating discounts for shop: {} (with provided token)", shop);

        ensureMetafieldDefinition(shop, accessToken);

        // Fetch ALL products with GraphQL (pagination) and variants
        List<ProductDTO> products = fetchAllProducts(shop, accessToken, true);

        List<MetafieldUpdateInput> batchUpdates = new ArrayList<>();
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (ProductDTO product : products) {
            String productId = product.getId();
            List<VariantDTO> variants = product.getVariants();

            double discountPercent = 0.0;
            boolean calculated = false;

            if (variants != null && !variants.isEmpty()) {
                VariantDTO firstVariant = variants.get(0);
                String priceStr = firstVariant.getPrice();
                String comparePriceStr = firstVariant.getCompareAtPrice();

                if (priceStr != null && comparePriceStr != null && !comparePriceStr.isEmpty()) {
                    try {
                        BigDecimal price = new BigDecimal(priceStr);
                        BigDecimal comparePrice = new BigDecimal(comparePriceStr);

                        if (comparePrice.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(comparePrice) < 0) {
                            discountPercent = calculateDiscountPercent(price, comparePrice);
                            calculated = true;
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing price for product {}: {}", productId, e.getMessage());
                    }
                }
            }

            if (calculated || discountPercent == 0.0) {
                batchUpdates.add(MetafieldUpdateInput.builder()
                        .ownerId(productId)
                        .namespace("custom")
                        .key("discount_percentage")
                        .type("number_decimal")
                        .value(discountPercent)
                        .build());
            } else {
                skippedCount++;
            }

            if (batchUpdates.size() >= 25) {
                boolean success = sendBatchMetafieldUpdate(shop, accessToken, batchUpdates);
                if (success)
                    updatedCount += batchUpdates.size();
                else
                    failedCount += batchUpdates.size();
                batchUpdates.clear();
            }
        }

        if (!batchUpdates.isEmpty()) {
            boolean success = sendBatchMetafieldUpdate(shop, accessToken, batchUpdates);
            if (success)
                updatedCount += batchUpdates.size();
            else
                failedCount += batchUpdates.size();
        }

        log.info("Discount calculation complete: {} updated, {} failed, {} skipped", updatedCount, failedCount,
                skippedCount);

        return BatchCalculationResult.builder()
                .updated(updatedCount)
                .failed(failedCount)
                .skipped(skippedCount)
                .total(products.size())
                .build();
    }

    private boolean sendBatchMetafieldUpdate(String shop, String accessToken, List<MetafieldUpdateInput> updates) {
        if (updates.isEmpty())
            return true;

        StringBuilder inputs = new StringBuilder();
        for (MetafieldUpdateInput update : updates) {
            inputs.append(String.format("""
                        {
                            ownerId: "%s",
                            namespace: "%s",
                            key: "%s",
                            type: "%s",
                            value: "%.2f"
                        },
                    """, update.getOwnerId(), update.getNamespace(), update.getKey(), update.getType(),
                    update.getValue()));
        }

        String mutation = String.format("""
                mutation {
                  metafieldsSet(metafields: [
                    %s
                  ]) {
                    userErrors {
                      field
                      message
                      code
                    }
                  }
                }
                """, inputs.toString());

        try {
            ShopifyGraphQLResponses.MetafieldsSetRoot root = graphQLClient.execute(
                    shop, accessToken, mutation, ShopifyGraphQLResponses.MetafieldsSetRoot.class);

            if (root != null && root.getData() != null && root.getData().getMetafieldsSet() != null) {
                List<ShopifyGraphQLResponses.UserError> userErrors = root.getData().getMetafieldsSet().getUserErrors();
                if (userErrors != null && !userErrors.isEmpty()) {
                    log.error("Batch update partial userErrors: {}", userErrors);
                    return false;
                }
            }

            log.info("✅ Batch update of {} items successful", updates.size());
            return true;

        } catch (Exception e) {
            log.error("Error sending batch update", e);
            return false;
        }
    }

    public double calculateDiscountPercent(BigDecimal price, BigDecimal compareAtPrice) {
        if (compareAtPrice == null || compareAtPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        BigDecimal discount = compareAtPrice.subtract(price);
        BigDecimal percentage = discount.divide(compareAtPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percentage.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public List<ProductDTO> fetchAllProducts(String shop, String accessToken, boolean includeVariants) {
        List<ProductDTO> allProducts = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            String cursorParam = (cursor == null) ? "" : ", after: " + "\"" + cursor + "\"";

            String variantsField = "";
            if (includeVariants) {
                variantsField = """
                        variants(first: 10) {
                          edges {
                            node {
                              price
                              compareAtPrice
                            }
                          }
                        }
                        """;
            }

            String query = String.format("""
                    {
                      products(first: 50%s) {
                        pageInfo {
                          hasNextPage
                          endCursor
                        }
                        edges {
                          node {
                            id
                            title
                            %s
                          }
                        }
                      }
                    }
                    """, cursorParam, variantsField);

            try {
                ShopifyGraphQLResponses.ProductsQueryRoot root = executeGraphQLInternal(shop, accessToken, query,
                        ShopifyGraphQLResponses.ProductsQueryRoot.class);
                if (root == null || root.getData() == null || root.getData().getProducts() == null)
                    break;

                ShopifyGraphQLResponses.ProductConnection productConnection = root.getData().getProducts();
                List<ShopifyGraphQLResponses.ProductEdge> edges = productConnection.getEdges();

                if (edges != null) {
                    for (ShopifyGraphQLResponses.ProductEdge edge : edges) {
                        ShopifyGraphQLResponses.ProductNode node = edge.getNode();
                        if (node == null)
                            continue;

                        List<VariantDTO> variants = new ArrayList<>();
                        if (includeVariants && node.getVariants() != null && node.getVariants().getEdges() != null) {
                            for (ShopifyGraphQLResponses.VariantEdge vEdge : node.getVariants().getEdges()) {
                                ShopifyGraphQLResponses.VariantNode vNode = vEdge.getNode();
                                if (vNode != null) {
                                    variants.add(VariantDTO.builder()
                                            .price(vNode.getPrice())
                                            .compareAtPrice(vNode.getCompareAtPrice())
                                            .build());
                                }
                            }
                        }

                        allProducts.add(ProductDTO.builder()
                                .id(node.getId())
                                .title(node.getTitle())
                                .variants(includeVariants ? variants : null)
                                .build());
                    }
                }

                if (productConnection.getPageInfo() != null) {
                    hasNextPage = productConnection.getPageInfo().isHasNextPage();
                    cursor = productConnection.getPageInfo().getEndCursor();
                } else {
                    hasNextPage = false;
                }

            } catch (Exception e) {
                log.error("Error fetching products page", e);
                hasNextPage = false;
            }
        }

        return allProducts;
    }

    private <T> T executeGraphQLInternal(String shop, String accessToken, String query, Class<T> responseType) {
        return graphQLClient.execute(shop, accessToken, query, responseType);
    }

    public void handleProductUpdate(String shop, String productId, String priceStr, String compareAtPriceStr) {
        List<ShopifySession> sessions = sessionRepository.findByShop(shop);
        if (sessions.isEmpty()) {
            log.warn("No session found for shop: {}", shop);
            return;
        }
        String accessToken = sessions.get(0).getAccessToken();

        double discountPercent = 0.0;

        if (priceStr != null && compareAtPriceStr != null && !compareAtPriceStr.isEmpty()) {
            try {
                BigDecimal price = new BigDecimal(priceStr);
                BigDecimal comparePrice = new BigDecimal(compareAtPriceStr);

                if (comparePrice.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(comparePrice) < 0) {
                    discountPercent = calculateDiscountPercent(price, comparePrice);
                }
            } catch (Exception e) {
                log.warn("Error parsing price for product update {}: {}", productId, e.getMessage());
            }
        }

        updateProductMetafield(shop, accessToken, productId, discountPercent);
        log.info("Updated discount for product {} to {}%", productId, discountPercent);
    }

    private boolean updateProductMetafield(String shop, String accessToken, String productId, double discountPercent) {
        String mutation = String.format("""
                mutation {
                  metafieldsSet(metafields: [{
                    ownerId: "%s",
                    namespace: "custom",
                    key: "discount_percentage",
                    type: "number_decimal",
                    value: "%.2f"
                  }]) {
                    metafields {
                      id
                      key
                      value
                    }
                    userErrors {
                      field
                      message
                    }
                  }
                }
                """, productId, discountPercent);

        try {
            ShopifyGraphQLResponses.MetafieldsSetRoot root = graphQLClient.execute(
                    shop, accessToken, mutation, ShopifyGraphQLResponses.MetafieldsSetRoot.class);

            if (root == null || root.getData() == null) {
                log.error("No data in response for product {}", productId);
                return false;
            }

            ShopifyGraphQLResponses.MetafieldsSetPayload payload = root.getData().getMetafieldsSet();
            if (payload == null || (payload.getUserErrors() != null && !payload.getUserErrors().isEmpty())) {
                log.error("Error updating metafield for product {}: {}", productId,
                        payload != null ? payload.getUserErrors() : "payload null");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error updating metafield for product: {}", productId, e);
            return false;
        }
    }

    private void ensureMetafieldDefinition(String shop, String accessToken) {
        String mutation = """
                mutation {
                  metafieldDefinitionCreate(definition: {
                    name: "Discount Percentage",
                    namespace: "custom",
                    key: "discount_percentage",
                    type: "number_decimal",
                    ownerType: PRODUCT,
                    description: "Auto-calculated discount percentage by app",
                    pin: true
                  }) {
                    createdDefinition {
                      id
                      name
                    }
                    userErrors {
                      field
                      message
                      code
                    }
                  }
                }
                """;

        try {
            ShopifyGraphQLResponses.MetafieldDefinitionCreateRoot root = graphQLClient.execute(
                    shop, accessToken, mutation, ShopifyGraphQLResponses.MetafieldDefinitionCreateRoot.class);

            if (root == null || root.getData() == null || root.getData().getMetafieldDefinitionCreate() == null)
                return;

            ShopifyGraphQLResponses.MetafieldDefinitionCreatePayload payload = root.getData()
                    .getMetafieldDefinitionCreate();

            if (payload.getUserErrors() != null && !payload.getUserErrors().isEmpty()) {
                ShopifyGraphQLResponses.UserError error = payload.getUserErrors().get(0);
                if (!"TAKEN".equals(error.getCode())
                        && (error.getMessage() == null || !error.getMessage().contains("taken"))) {
                    log.error("❌ Error creating definition: {}", payload.getUserErrors());
                }
                return;
            }
        } catch (Exception e) {
            log.error("Error ensuring metafield definition", e);
        }
    }
}
