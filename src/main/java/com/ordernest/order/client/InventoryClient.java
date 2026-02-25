package com.ordernest.order.client;

import com.ordernest.order.exception.BadRequestException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(@Value("${app.inventory.base-url}") String inventoryBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(inventoryBaseUrl)
                .build();
    }

    public InventoryProductResponse getProductById(UUID productId, String authorization) {
        try {
            RestClient.RequestHeadersSpec<?> requestSpec = restClient.get().uri("/api/products/{id}", productId);
            if (authorization != null && !authorization.isBlank()) {
                requestSpec = requestSpec.header("Authorization", authorization);
            }
            InventoryProductResponse response = requestSpec.retrieve().body(InventoryProductResponse.class);

            if (response == null || response.id() == null) {
                throw new BadRequestException("Inventory response is invalid for product id: " + productId);
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new BadRequestException("Product not found in inventory: " + productId);
            }
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new BadRequestException("Unauthorized to verify product in inventory");
            }
            throw new BadRequestException("Unable to verify inventory right now");
        } catch (RestClientException ex) {
            throw new BadRequestException("Unable to verify inventory right now");
        }
    }
}
