package com.onthi.v_edu.wallet.service;

import com.onthi.v_edu.wallet.entity.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class PayOSService {

    private static final Logger logger = LoggerFactory.getLogger(PayOSService.class);
    private final com.onthi.v_edu.common.setting.SystemSettingService systemSettingService;

    @Value("${app.payos.return-url}")
    private String defaultReturnUrl;

    @Value("${app.payos.cancel-url}")
    private String defaultCancelUrl;

    @Value("${app.payos.client-id}")
    private String defaultClientId;

    @Value("${app.payos.api-key}")
    private String defaultApiKey;

    @Value("${app.payos.checksum-key}")
    private String defaultChecksumKey;

    public PayOSService(com.onthi.v_edu.common.setting.SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    private PayOS getPayOS() {
        String clientId = systemSettingService.getSettingValue("PAYOS_CLIENT_ID", defaultClientId);
        String apiKey = systemSettingService.getSettingValue("PAYOS_API_KEY", defaultApiKey);
        String checksumKey = systemSettingService.getSettingValue("PAYOS_CHECKSUM_KEY", defaultChecksumKey);
        return new PayOS(clientId, apiKey, checksumKey);
    }

    public CreatePaymentLinkResponse createPaymentLink(Transaction transaction) throws Exception {
        String description = "Nap tien vao vi V-Edu";
        if (description.length() > 25) description = description.substring(0, 25);

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name("Nap tiền V-Edu")
                .quantity(1)
                .price(transaction.getAmount().longValue())
                .build();

        // Thời gian hết hạn: 10 phút (600 giây) từ bây giờ
        long expiredAt = System.currentTimeMillis() / 1000 + 600;

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(transaction.getOrderCode())
                .amount(transaction.getAmount().longValue())
                .description(description)
                .returnUrl(systemSettingService.getSettingValue("PAYOS_RETURN_URL", defaultReturnUrl))
                .cancelUrl(systemSettingService.getSettingValue("PAYOS_CANCEL_URL", defaultCancelUrl))
                .expiredAt(expiredAt)
                .items(List.of(item))
                .build();

        logger.info("[PayOS] Đang gọi API PayOS để tạo link thanh toán cho OrderCode: {}", transaction.getOrderCode());
        return getPayOS().paymentRequests().create(paymentData);
    }

    public WebhookData verifyWebhookData(Webhook webhookBody) throws Exception {
        return getPayOS().webhooks().verify(webhookBody);
    }

    public PaymentLink getPaymentLinkInformation(long orderCode) throws Exception {
        return getPayOS().paymentRequests().get(orderCode);
    }
}
