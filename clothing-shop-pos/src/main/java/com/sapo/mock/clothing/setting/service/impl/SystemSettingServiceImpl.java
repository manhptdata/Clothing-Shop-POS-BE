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
    public static final String SETTING_REQUIRE_CANCEL_APPROVAL = "REQUIRE_CANCEL_APPROVAL";
    public static final String SETTING_PAYMENT_BANK_NAME = "PAYMENT_BANK_NAME";
    public static final String SETTING_PAYMENT_BANK_ACCOUNT = "PAYMENT_BANK_ACCOUNT";
    public static final String SETTING_PAYMENT_ACCOUNT_NAME = "PAYMENT_ACCOUNT_NAME";
    public static final String SETTING_MAX_PENDING_ORDERS = "MAX_PENDING_ORDERS_PER_CUSTOMER";

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
            } else if (SETTING_REQUIRE_CANCEL_APPROVAL.equals(key)) {
                newSetting.setDescription("Yêu cầu duyệt hủy đơn hàng bằng mã PIN quản lý");
            } else if (SETTING_PAYMENT_BANK_NAME.equals(key)) {
                newSetting.setDescription("Mã ngân hàng thụ hưởng (ví dụ: MBBank, Vietcombank...)");
            } else if (SETTING_PAYMENT_BANK_ACCOUNT.equals(key)) {
                newSetting.setDescription("Số tài khoản ngân hàng thụ hưởng");
            } else if (SETTING_PAYMENT_ACCOUNT_NAME.equals(key)) {
                newSetting.setDescription("Tên chủ tài khoản ngân hàng thụ hưởng");
            } else if (SETTING_MAX_PENDING_ORDERS.equals(key)) {
                newSetting.setDescription("Số đơn hàng PENDING tối đa mà một khách hàng được phép sở hữu cùng lúc");
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

    @Override
    public boolean isCancelApprovalRequired() {
        SystemSetting setting = getSettingByKey(SETTING_REQUIRE_CANCEL_APPROVAL);
        // Default is true if not configured for better security
        return setting == null || "true".equalsIgnoreCase(setting.getSettingValue());
    }

    @Override
    public int getMaxPendingOrdersLimit() {
        SystemSetting setting = getSettingByKey(SETTING_MAX_PENDING_ORDERS);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return 3; // Mặc định là 3 nếu chưa cấu hình
        }
        try {
            return Integer.parseInt(setting.getSettingValue().trim());
        } catch (NumberFormatException e) {
            return 3;
        }
    }
}
