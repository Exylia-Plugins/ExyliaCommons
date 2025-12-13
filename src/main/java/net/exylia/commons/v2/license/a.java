package net.exylia.commons.v2.license;

import com.hapangama.SunLicenseAPI;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.utils.DebugUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class a {
    private final ExyliaPlugin p;
    private final b ctx;
    private final c chk;
    private SunLicenseAPI x;
    private static final String u;

    static {
        byte[] ub = Base64.getDecoder().decode("aHR0cHM6Ly9saWNlbnNlcy5leHlsaWEubmV0Lw==");
        u = new String(ub);
    }

    public a(ExyliaPlugin p) {
        this.p = p;
        this.ctx = new b();
        this.chk = new c(ctx);
    }

    public boolean v() {
        try {
            m1();
            return m2();
        } catch (Exception e) {
            m3(e.getMessage());
            return false;
        }
    }

    private void m1() throws Exception {
        File f = new File(p.getDataFolder(), "license.yml");

        if (!f.exists()) {
            m4(f);
            throw new Exception(d1() + f.getPath() + d2());
        }

        ctx.s("file", f);
        ctx.s("exists", true);

        String k = m5(f);
        if (k == null) {
            throw new Exception(d3() + f.getPath() + d4());
        }

        ctx.s("key", k);
    }

    private boolean m2() throws Exception {
        String k = (String) ctx.g("key");
        if (k == null) {
            return false;
        }

        try {
            SunLicenseAPI api = SunLicenseAPI.getLicense(
                    k,
                    p.getProductID(),
                    p.getDescription().getVersion(),
                    u
            );

            String ip = m6();
            api.setIp(ip);

            if (!chk.v1(api)) {
                return false;
            }

            p.setSunLicenseAPI(api);
            this.x = api;

            ctx.s("api", api);
            ctx.s("validated", true);

            return true;
        } catch (Exception e) {
            throw new Exception(d5() + e.getMessage() + d6());
        }
    }

    private void m4(File f) throws Exception {
        try {
            f.getParentFile().mkdirs();

            try (FileWriter w = new FileWriter(f)) {
                w.write(c1());
                w.write(c2(p.getName().toUpperCase()));
                w.write(c3());
                w.write(c4());
                w.write(c5());
                w.write(c6());
                w.write(c7());
                w.write(c8());
                w.write(c9());
                w.write(c10());
                w.write(c11());
            }
        } catch (IOException e) {
            throw new Exception(d7() + e.getMessage());
        }
    }

    private String m5(File f) throws Exception {
        try {
            List<String> lines = Files.readAllLines(f.toPath());

            boolean in = false;
            for (String line : lines) {
                line = line.trim();
                if (line.equals("license:")) {
                    in = true;
                    continue;
                }
                if (in && line.startsWith("key:")) {
                    String kv = line.substring(4).trim();
                    if (kv.startsWith("\"") && kv.endsWith("\"")) {
                        kv = kv.substring(1, kv.length() - 1);
                    }
                    if (!kv.isEmpty() && !kv.equals("YOUR-LICENSE-KEY-HERE")) {
                        return kv;
                    }
                }
                if (in && line.endsWith(":") && !line.startsWith("key:")) {
                    in = false;
                }
            }
            return null;
        } catch (IOException e) {
            throw new Exception(d8() + e.getMessage());
        }
    }

    private String m6() {
        try {
            URL url = new URL(d9());
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            return in.readLine();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void m3(String msg) {
        DebugUtils.logInternalError("========================================");
        DebugUtils.logInternalError("LICENSE ERROR - " + p.getName().toUpperCase());
        DebugUtils.logInternalError("========================================");
        DebugUtils.logInternalError("");
        DebugUtils.logInternalError(msg);
        DebugUtils.logInternalError("");
        DebugUtils.logInternalError("Plugin has been disabled.");
        DebugUtils.logInternalError("Join our Discord for support: https://discord.exylia.net/");
        DebugUtils.logInternalError("========================================");
    }

    private String c1() { return "# =================================================\n"; }
    private String c2(String n) { return "# LICENSE CONFIGURATION - " + n + "\n"; }
    private String c3() { return "# =================================================\n"; }
    private String c4() { return "#\n"; }
    private String c5() { return "# This plugin requires a valid license.\n"; }
    private String c6() { return "# Join our Discord server for more information.\n"; }
    private String c7() { return "# https://discord.exylia.net/\n"; }
    private String c8() { return "#\n# =================================================\n"; }
    private String c9() { return "# license:\n"; }
    private String c10() { return "#   key: Your license key (format: XXXX-XXXX-XXXX-XXXX-XXXX)\n"; }
    private String c11() { return "# =================================================\n\nlicense:\n  key: \"\"\n"; }

    private String d1() { return "License file has been created at: "; }
    private String d2() { return " | Please edit the file and add your valid license key, then restart the server."; }
    private String d3() { return "Invalid or missing license key in: "; }
    private String d4() { return " | Please add a valid license key and restart the server."; }
    private String d5() { return "License validation failed: "; }
    private String d6() { return " | Please check your license key or contact support."; }
    private String d7() { return "Failed to create license file: "; }
    private String d8() { return "Failed to read license file: "; }
    private String d9() { return "https://api.ipify.org"; }
}
