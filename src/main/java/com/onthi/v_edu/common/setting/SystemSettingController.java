package com.onthi.v_edu.common.setting;

import com.onthi.v_edu.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/system-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getAllSettings() {
        List<SystemSetting> settings = systemSettingService.getAllSettings();
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy cấu hình hệ thống thành công", settings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> updateSettings(@RequestBody Map<String, String> settings) {
        systemSettingService.updateSettings(settings);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật cấu hình hệ thống thành công", null));
    }
}
