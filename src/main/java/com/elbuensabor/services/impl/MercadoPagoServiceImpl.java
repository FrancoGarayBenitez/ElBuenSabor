package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.MercadoPagoPreferenceDTO;
import com.elbuensabor.dto.response.MercadoPagoPreferenceResponseDTO;
import com.elbuensabor.dto.response.MercadoPagoPaymentResponseDTO;
import com.elbuensabor.services.IMercadoPagoService;
import com.elbuensabor.services.IPagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MercadoPagoServiceImpl implements IMercadoPagoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoServiceImpl.class);

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${mercadopago.public.key}")
    private String publicKey;

    @Value("${app.base.url}")
    private String baseUrl;

    @Autowired
    private IPagoService pagoService;

    private PreferenceClient preferenceClient;
    private PaymentClient paymentClient;

    private void initializeClients() {
        if (preferenceClient == null) {
            MercadoPagoConfig.setAccessToken(accessToken);
            preferenceClient = new PreferenceClient();
            paymentClient = new PaymentClient();
        }
    }

    @Override
    public MercadoPagoPreferenceResponseDTO crearPreferencia(MercadoPagoPreferenceDTO preferenceDTO) {
        try {
            initializeClients();

            // Convertir items
            List<PreferenceItemRequest> items = preferenceDTO.getItems().stream()
                    .map(this::convertToPreferenceItem)
                    .collect(Collectors.toList());

            // Convertir payer
            PreferencePayerRequest payer = convertToPreferencePayer(preferenceDTO.getPayer());

            // URLs de retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(baseUrl + "/payment/success")
                    .failure(baseUrl + "/payment/failure")
                    .pending(baseUrl + "/payment/pending")
                    .build();

            // Crear request de preferencia
            PreferenceRequest request = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .notificationUrl(baseUrl + "/api/pagos/webhook/mercadopago")
                    .externalReference(preferenceDTO.getExternalReference())
                    .build();

            // Crear preferencia en MP
            Preference preference = preferenceClient.create(request);

            // Convertir respuesta
            return convertToPreferenceResponse(preference);

        } catch (MPException | MPApiException e) {
            logger.error("Error creando preferencia de Mercado Pago: {}", e.getMessage(), e);
            throw new RuntimeException("Error creando preferencia de pago", e);
        }
    }

    @Override
    public MercadoPagoPaymentResponseDTO obtenerPago(Long paymentId) {
        try {
            initializeClients();

            Payment payment = paymentClient.get(paymentId);
            return convertToPaymentResponse(payment);

        } catch (MPException | MPApiException e) {
            logger.error("Error obteniendo pago de Mercado Pago: {}", e.getMessage(), e);
            throw new RuntimeException("Error obteniendo información del pago", e);
        }
    }

    @Override
    public void procesarWebhook(String topic, String id) {
        try {
            logger.info("Procesando webhook - Topic: {}, ID: {}", topic, id);

            if ("payment".equals(topic)) {
                Long paymentId = Long.valueOf(id);
                MercadoPagoPaymentResponseDTO payment = obtenerPago(paymentId);

                // Actualizar el pago en nuestro sistema
                pagoService.confirmarPagoMercadoPago(
                        paymentId,
                        payment.getStatus(),
                        payment.getStatusDetail()
                );

                logger.info("Webhook procesado exitosamente para payment ID: {}", paymentId);
            }

        } catch (Exception e) {
            logger.error("Error procesando webhook: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando webhook de Mercado Pago", e);
        }
    }

    @Override
    public void cancelarPreferencia(String preferenceId) {
        // Mercado Pago no tiene endpoint directo para cancelar preferencias
        // Las preferencias expiran automáticamente después de 6 meses
        logger.info("Preference {} marcada para cancelación (expira automáticamente)", preferenceId);
    }

    @Override
    public void procesarReembolso(Long paymentId, Double amount) {
        try {
            initializeClients();

            // Para reembolsos necesitas usar RefundClient
            // Este es un ejemplo básico, deberías implementar RefundClient
            logger.info("Procesando reembolso para payment {} por monto {}", paymentId, amount);

            // Actualizar estado en nuestro sistema
            // pagoService.procesarReembolso(pagoId);

        } catch (Exception e) {
            logger.error("Error procesando reembolso: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando reembolso", e);
        }
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    // Métodos de conversión
    private PreferenceItemRequest convertToPreferenceItem(MercadoPagoPreferenceDTO.ItemDTO item) {
        return PreferenceItemRequest.builder()
                .title(item.getTitle())
                .quantity(item.getQuantity())
                .unitPrice(BigDecimal.valueOf(item.getUnitPrice()))
                .currencyId(item.getCurrencyId())
                .description(item.getDescription())
                .build();
    }

    private PreferencePayerRequest convertToPreferencePayer(MercadoPagoPreferenceDTO.PayerDTO payer) {
        return PreferencePayerRequest.builder()
                .name(payer.getName())
                .surname(payer.getSurname())
                .email(payer.getEmail())
                .build();
    }

    private MercadoPagoPreferenceResponseDTO convertToPreferenceResponse(Preference preference) {
        MercadoPagoPreferenceResponseDTO response = new MercadoPagoPreferenceResponseDTO();
        response.setId(preference.getId());
        response.setInitPoint(preference.getInitPoint());
        response.setSandboxInitPoint(preference.getSandboxInitPoint());
        response.setClientId(preference.getClientId());
        response.setCollectorId(preference.getCollectorId()); // Ahora es Long, no String
        response.setOperationType(preference.getOperationType());
        response.setExternalReference(preference.getExternalReference()); // Campo que sí existe
        response.setNotificationUrl(preference.getNotificationUrl());     // Campo que sí existe
        return response;
    }

    private MercadoPagoPaymentResponseDTO convertToPaymentResponse(Payment payment) {
        MercadoPagoPaymentResponseDTO response = new MercadoPagoPaymentResponseDTO();
        response.setId(payment.getId());
        response.setStatus(payment.getStatus());
        response.setStatusDetail(payment.getStatusDetail());
        response.setOperationType(payment.getOperationType());
        response.setPaymentMethodId(payment.getPaymentMethodId());
        response.setPaymentTypeId(payment.getPaymentTypeId());
        response.setTransactionAmount(payment.getTransactionAmount().doubleValue());
        response.setCurrencyId(payment.getCurrencyId());
        response.setDateCreated(payment.getDateCreated().toString());
        response.setDateApproved(payment.getDateApproved() != null ? payment.getDateApproved().toString() : null);
        response.setExternalReference(payment.getExternalReference());

        if (payment.getPayer() != null) {
            MercadoPagoPaymentResponseDTO.PayerResponseDTO payer = new MercadoPagoPaymentResponseDTO.PayerResponseDTO();
            payer.setEmail(payment.getPayer().getEmail());
            payer.setFirstName(payment.getPayer().getFirstName());
            payer.setLastName(payment.getPayer().getLastName());
            response.setPayer(payer);
        }

        return response;
    }
}