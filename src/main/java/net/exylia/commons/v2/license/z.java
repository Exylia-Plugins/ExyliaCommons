package net.exylia.commons.v2.license;

import com.hapangama.SunLicenseAPI;
import net.exylia.commons.ExyliaPlugin;

public class z {

    public static boolean a(ExyliaPlugin plugin) {
        LicenseVerifier verifier = new LicenseVerifier(plugin);
        return verifier.validate();
    }

    public static boolean b(ExyliaPlugin plugin) {
        SunLicenseAPI api = plugin.getSunLicenseAPI();
        return api != null;
    }
}
