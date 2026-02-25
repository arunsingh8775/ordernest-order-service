package com.ordernest.order.client;

import com.ordernest.order.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(@Value("${app.payment.base-url}") String paymentBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentBaseUrl)
                .build();
    }

    public void processPayment(PaymentProcessRequest request, String authorization) {
        try {
            RestClient.RequestBodySpec requestSpec = restClient.post().uri("/api/payments/process");
            if (authorization != null && !authorization.isBlank()) {
                requestSpec = requestSpec.header("Authorization", authorization);
            }

            requestSpec
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requestObj, responseObj) -> {
                        throw new BadRequestException("Unable to process payment right now");
                    })
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new BadRequestException("Unauthorized to process payment");
            }
            throw new BadRequestException("Unable to process payment right now");
        } catch (RestClientException ex) {
            throw new BadRequestException("Unable to process payment right now");
        }
    }
}
