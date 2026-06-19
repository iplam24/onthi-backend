package com.onthi.v_edu.common.setting;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.application.name:V-Edu}")
    private String defaultAppName;

    @Value("${app.description:V-Edu backend service}")
    private String defaultAppDescription;

    @Value("${app.version:1.0.0}")
    private String defaultAppVersion;

    @Value("${app.environment:development}")
    private String defaultAppEnvironment;

    @Value("${app.jwt-secret:YourSuperSecretKeyThatIsAtLeast256BitsLongAndShouldBeStoredSecurelyInProduction}")
    private String defaultJwtSecret;

    @Value("${app.jwt-expiration-milliseconds:86400000}")
    private long defaultJwtExpiration;

    @Value("${app.push.vapid.public-key:}")
    private String defaultVapidPublicKey;

    @Value("${app.push.vapid.private-key:}")
    private String defaultVapidPrivateKey;

    @Value("${app.push.vapid.subject:mailto:admin@vuxuanlam.me}")
    private String defaultVapidSubject;

    @Value("${app.payos.client-id:}")
    private String defaultPayosClientId;

    @Value("${app.payos.api-key:}")
    private String defaultPayosApiKey;

    @Value("${app.payos.checksum-key:}")
    private String defaultPayosChecksumKey;

    @Value("${app.payos.return-url:http://localhost:5173/payment/success}")
    private String defaultPayosReturnUrl;

    @Value("${app.payos.cancel-url:http://localhost:5173/payment/cancel}")
    private String defaultPayosCancelUrl;

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/onthi}")
    private String defaultMongodbUri;

    private final Map<String, String> cachedSettings = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            log.info("[SYSTEM SETTING] Initializing settings from database...");
            List<SystemSetting> dbSettings = systemSettingRepository.findAll();
            
            Map<String, SystemSetting> dbMap = new HashMap<>();
            for (SystemSetting setting : dbSettings) {
                dbMap.put(setting.getKey(), setting);
            }

            // Define all default settings
            initializeSetting(dbMap, "SYSTEM_APP_NAME", defaultAppName, "SYSTEM", "Tên ứng dụng hệ thống");
            initializeSetting(dbMap, "SYSTEM_DESCRIPTION", defaultAppDescription, "SYSTEM", "Mô tả ngắn của ứng dụng");
            initializeSetting(dbMap, "SYSTEM_VERSION", defaultAppVersion, "SYSTEM", "Phiên bản của ứng dụng");
            initializeSetting(dbMap, "SYSTEM_ENVIRONMENT", defaultAppEnvironment, "SYSTEM", "Môi trường chạy ứng dụng (development, production)");
            initializeSetting(dbMap, "SYSTEM_HOTLINE", "1900 1234", "SYSTEM", "Hotline chăm sóc khách hàng");
            initializeSetting(dbMap, "SYSTEM_EMAIL", "support@vuxuanlam.me", "SYSTEM", "Email liên hệ hỗ trợ");

            initializeSetting(dbMap, "JWT_SECRET", defaultJwtSecret, "SECURITY", "Khóa bí mật JWT (dạng Base64)");
            initializeSetting(dbMap, "JWT_EXPIRATION_MS", String.valueOf(defaultJwtExpiration), "SECURITY", "Thời gian hết hạn JWT (ms)");
            initializeSetting(dbMap, "SECURITY_MAX_VIOLATIONS", "3", "SECURITY", "Số lần vi phạm tối đa trong phòng thi trước khi khóa");

            initializeSetting(dbMap, "PUSH_VAPID_PUBLIC_KEY", defaultVapidPublicKey, "NOTIFICATION", "Khóa công khai VAPID Web Push");
            initializeSetting(dbMap, "PUSH_VAPID_PRIVATE_KEY", defaultVapidPrivateKey, "NOTIFICATION", "Khóa bảo mật VAPID Web Push");
            initializeSetting(dbMap, "PUSH_VAPID_SUBJECT", defaultVapidSubject, "NOTIFICATION", "Tiêu đề VAPID Web Push (ví dụ: mailto:admin@vuxuanlam.me)");

            initializeSetting(dbMap, "PAYOS_CLIENT_ID", defaultPayosClientId, "PAYMENT", "Client ID của cổng thanh toán PayOS");
            initializeSetting(dbMap, "PAYOS_API_KEY", defaultPayosApiKey, "PAYMENT", "API Key của cổng thanh toán PayOS");
            initializeSetting(dbMap, "PAYOS_CHECKSUM_KEY", defaultPayosChecksumKey, "PAYMENT", "Checksum Key của cổng thanh toán PayOS");
            initializeSetting(dbMap, "PAYOS_RETURN_URL", defaultPayosReturnUrl, "PAYMENT", "URL redirect khi thanh toán thành công");
            initializeSetting(dbMap, "PAYOS_CANCEL_URL", defaultPayosCancelUrl, "PAYMENT", "URL redirect khi hủy thanh toán");

            initializeSetting(dbMap, "MONGODB_URI", defaultMongodbUri, "DATABASE", "Đường dẫn kết nối cơ sở dữ liệu MongoDB (Atlas hoặc local)");

            // Auto-migrate MONGODB_URI category if it was previously set as SYSTEM
            SystemSetting mongoSetting = dbMap.get("MONGODB_URI");
            if (mongoSetting != null && !"DATABASE".equals(mongoSetting.getCategory())) {
                mongoSetting.setCategory("DATABASE");
                systemSettingRepository.save(mongoSetting);
                log.info("[SYSTEM SETTING] Migrated MONGODB_URI setting category to DATABASE");
            }

            // Refresh local cache
            dbSettings = systemSettingRepository.findAll();
            for (SystemSetting setting : dbSettings) {
                cachedSettings.put(setting.getKey(), setting.getValue() != null ? setting.getValue() : "");
            }
            log.info("[SYSTEM SETTING] Loaded {} settings into memory cache.", cachedSettings.size());
        } catch (Exception e) {
            log.error("[SYSTEM SETTING] Error initializing system settings: {}", e.getMessage(), e);
        }
    }

    private void initializeSetting(Map<String, SystemSetting> dbMap, String key, String defaultValue, String category, String description) {
        if (!dbMap.containsKey(key)) {
            SystemSetting setting = new SystemSetting(key, defaultValue != null ? defaultValue : "", category, description);
            systemSettingRepository.save(setting);
            dbMap.put(key, setting);
            log.info("[SYSTEM SETTING] Pre-populated default setting key: {}", key);
        }
    }

    public String getSettingValue(String key, String defaultValue) {
        return cachedSettings.getOrDefault(key, defaultValue);
    }

    public long getSettingValueAsLong(String key, long defaultValue) {
        String val = cachedSettings.get(key);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getSettingValueAsInt(String key, int defaultValue) {
        String val = cachedSettings.get(key);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAll();
    }

    public synchronized void updateSettings(Map<String, String> newSettings) {
        boolean mongoUriChanged = false;
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            SystemSetting setting = systemSettingRepository.findById(key).orElse(null);
            if (setting != null) {
                setting.setValue(value);
                systemSettingRepository.save(setting);
                cachedSettings.put(key, value != null ? value : "");
                log.info("[SYSTEM SETTING] Updated setting {} = {}", key, value);
                if ("MONGODB_URI".equals(key)) {
                    mongoUriChanged = true;
                }
            }
        }
        if (mongoUriChanged) {
            String newUri = cachedSettings.get("MONGODB_URI");
            log.info("[SYSTEM SETTING] MongoDB URI changed to: {}. Publishing change event...", newUri != null ? newUri.replaceAll("mongodb(\\+srv)?://([^:]+):([^@]+)@", "mongodb$1://$2:******@") : "null");
            eventPublisher.publishEvent(new com.onthi.v_edu.config.MongoUriChangedEvent(this, newUri));
        }
    }
}
