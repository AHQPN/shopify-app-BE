package org.chatapp.customshopify.controller;

import org.chatapp.customshopify.dto.model.ProductDTO;
import org.chatapp.customshopify.dto.response.ApiResponse;
import org.chatapp.customshopify.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProducts(jakarta.servlet.http.HttpServletRequest request) {
        String shop = (String) request.getAttribute("shop");
        String accessToken = (String) request.getAttribute("accessToken");

        if (shop == null || accessToken == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<ProductDTO>>builder().message("Session missing").build());
        }

        List<ProductDTO> products = productService.fetchAllProducts(shop, accessToken, false);
        return ResponseEntity.ok().body(ApiResponse.<List<ProductDTO>>builder().data(products).build());
    }
}
