package com.islandtrails.auth.service;

import com.islandtrails.auth.entity.SystemConfig;
import com.islandtrails.auth.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class SystemConfigService {

    public static final String KEY_PROFIT_MARGIN = "PROFIT_MARGIN_PERCENTAGE";

    private final SystemConfigRepository systemConfigRepository;
    private final AuditService auditService;

    public SystemConfigService(SystemConfigRepository systemConfigRepository, AuditService auditService) {
        this.systemConfigRepository = systemConfigRepository;
        this.auditService = auditService;
    }

    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    public Optional<String> getConfigValue(String key) {
        return systemConfigRepository.findByKey(key).map(SystemConfig::getValue);
    }

    public BigDecimal getProfitMarginPercentage() {
        return getConfigValue(KEY_PROFIT_MARGIN)
                .map(val -> {
                    try {
                        return new BigDecimal(val.trim());
                    } catch (NumberFormatException e) {
                        return new BigDecimal("15.00");
                    }
                })
                .orElse(new BigDecimal("15.00"));
    }

    @Transactional
    public SystemConfig setConfig(String key, String value, String description, Long updatedBy, String userEmail, String ipAddress) {
        SystemConfig config = systemConfigRepository.findByKey(key)
                .orElse(new SystemConfig(key, value, description, updatedBy));

        String oldValue = config.getValue();
        config.setValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        config.setUpdatedBy(updatedBy);

        SystemConfig saved = systemConfigRepository.save(config);

        auditService.logAction(
                updatedBy,
                userEmail,
                "SETTING_CHANGED",
                String.format("Changed config '%s' from '%s' to '%s'", key, oldValue, value),
                ipAddress
        );

        return saved;
    }
}
