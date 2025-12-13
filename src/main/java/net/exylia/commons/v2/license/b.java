package net.exylia.commons.v2.license;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class b {
    private final Map<String, Object> d;
    private final int x;

    public b() {
        this.d = new HashMap<>();
        this.x = 0x5A;
    }

    public void s(String k, Object v) {
        d.put(e(k), v);
    }

    public Object g(String k) {
        return d.get(e(k));
    }

    public boolean h(String k) {
        return d.containsKey(e(k));
    }

    private String e(String s) {
        byte[] b = s.getBytes();
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) (b[i] ^ x);
        }
        return Base64.getEncoder().encodeToString(b);
    }

    public void c() {
        d.clear();
    }

    public int z() {
        return d.size();
    }

    public boolean v() {
        return !d.isEmpty();
    }
}
