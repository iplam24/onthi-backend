package com.onthi.v_edu.common.ai;

import com.onthi.v_edu.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<AiConfig>> getConfig() {
        AiConfig config = aiConfigService.getConfig();
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy cấu hình AI thành công", config));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiConfig>> updateConfig(@Valid @RequestBody AiConfigRequest request) {
        AiConfig config = aiConfigService.updateConfig(request);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật cấu hình AI thành công", config));
    }
}
