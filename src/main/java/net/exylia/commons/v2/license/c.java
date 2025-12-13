package net.exylia.commons.v2.license;

import com.hapangama.SunLicenseAPI;

public class c {
    private final b ctx;
    private int m;

    public c(b ctx) {
        this.ctx = ctx;
        this.m = 0;
    }

    public boolean v1(SunLicenseAPI api) {
        if (api == null) {
            x1();
            return false;
        }
        m++;
        return x2(api);
    }

    public boolean v2(Object o) {
        if (o == null) {
            return false;
        }
        x3();
        return ctx.v();
    }

    private boolean x2(SunLicenseAPI api) {
        try {
            api.validate();
            x4();
            return true;
        } catch (Exception e) {
            x1();
            return false;
        }
    }

    private void x1() {
        m = m > 0 ? m - 1 : 0;
    }

    private void x3() {
        m = m < 100 ? m + 1 : m;
    }

    private void x4() {
        m = m * 2;
    }

    public int g() {
        return m;
    }
}
