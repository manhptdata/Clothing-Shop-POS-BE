package com.sapo.mock.clothing.setting.service.impl;

import com.sapo.mock.clothing.entity.SystemSetting;
import com.sapo.mock.clothing.setting.repository.SystemSettingRepository;
import com.sapo.mock.clothing.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public static final String SETTING_REQUIRE_RETURN_APPROVAL = "REQUIRE_RETURN_APPROVAL";

    @Override
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAll();
    }

    @Override
    public SystemSetting getSettingByKey(String key) {
        return systemSettingRepository.findById(key).orElse(null);
    }

    @Override
    public SystemSetting updateSetting(String key, String value) {
        SystemSetting setting = systemSettingRepository.findById(key).orElseGet(() -> {
            SystemSetting newSetting = new SystemSetting();
            newSetting.setSettingKey(key);
            if (SETTING_REQUIRE_RETURN_APPROVAL.equals(key)) {
                newSetting.setDescription("Yêu cầu duyệt trả hàng bằng mã PIN quản lý");
            }
            return newSetting;
        });
        setting.setSettingValue(value);
        return systemSettingRepository.save(setting);
    }

    @Override
    public boolean isReturnApprovalRequired() {
        SystemSetting setting = getSettingByKey(SETTING_REQUIRE_RETURN_APPROVAL);
        // Default is true if not configured for better security
        return setting == null || "true".equalsIgnoreCase(setting.getSettingValue());
    }
}
