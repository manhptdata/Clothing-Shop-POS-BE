package com.sapo.mock.clothing.setting.service;

import com.sapo.mock.clothing.entity.SystemSetting;
import java.util.List;

public interface SystemSettingService {
    List<SystemSetting> getAllSettings();
    SystemSetting getSettingByKey(String key);
    SystemSetting updateSetting(String key, String value);
    boolean isReturnApprovalRequired();
    boolean isCancelApprovalRequired();
    int getMaxPendingOrdersLimit();
}
