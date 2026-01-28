package net.exylia.commons.v2.license.provider;

import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.license.result.LicenseValidationResult;

public interface LicenseProvider {
    String getName();
    LicenseValidationResult validate(ExyliaPlugin plugin, String licenseKey);
    default int getPriority() {
        return 0;
    }
}
