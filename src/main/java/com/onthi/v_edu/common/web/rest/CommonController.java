package com.onthi.v_edu.common.web.rest;

import com.onthi.v_edu.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommonController {

    private final Environment environment;
    private final com.onthi.v_edu.common.setting.SystemSettingService systemSettingService;

    @Value("${spring.application.name:V-Edu}")
    private String defaultAppName;

    @Value("${app.version:1.0.0}")
    private String defaultAppVersion;

    @Value("${app.description:V-Edu backend service}")
    private String defaultAppDescription;

    @Value("${app.environment:development}")
    private String defaultAppEnvironment;

    public CommonController(
            Environment environment,
            com.onthi.v_edu.common.setting.SystemSettingService systemSettingService) {
        this.environment = environment;
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> data = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();

        data.put("status", "UP");
        data.put("application", systemSettingService.getSettingValue("SYSTEM_APP_NAME", defaultAppName));
        data.put("version", systemSettingService.getSettingValue("SYSTEM_VERSION", defaultAppVersion));
        data.put("environment", systemSettingService.getSettingValue("SYSTEM_ENVIRONMENT", defaultAppEnvironment));
        data.put("timestamp", Instant.now().toString());
        data.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        data.put("availableProcessors", runtime.availableProcessors());
        data.put("freeMemoryBytes", runtime.freeMemory());
        data.put("totalMemoryBytes", runtime.totalMemory());
        data.put("maxMemoryBytes", runtime.maxMemory());
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("javaVendor", System.getProperty("java.vendor"));
        data.put("osName", System.getProperty("os.name"));
        data.put("osVersion", System.getProperty("os.version"));
        data.put("activeProfiles", environment.getActiveProfiles());

        return ResponseEntity.ok(new ApiResponse<>(200, "Application is running", data));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInfo() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("application", systemSettingService.getSettingValue("SYSTEM_APP_NAME", defaultAppName));
        data.put("description", systemSettingService.getSettingValue("SYSTEM_DESCRIPTION", defaultAppDescription));
        data.put("version", systemSettingService.getSettingValue("SYSTEM_VERSION", defaultAppVersion));
        data.put("environment", systemSettingService.getSettingValue("SYSTEM_ENVIRONMENT", defaultAppEnvironment));
        data.put("springBootVersion", SpringBootVersion.getVersion());
        data.put("timestamp", Instant.now().toString());
        data.put("activeProfiles", environment.getActiveProfiles());
        data.put("defaultProfiles", environment.getDefaultProfiles());
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("javaVendor", System.getProperty("java.vendor"));
        data.put("osName", System.getProperty("os.name"));
        data.put("osVersion", System.getProperty("os.version"));
        data.put("osArchitecture", System.getProperty("os.arch"));

        return ResponseEntity.ok(new ApiResponse<>(200, "Application information", data));
    }
}
