package net.exylia.commons.v2.peee;

import com.hapangama.SunLicenseAPI;

public class c {
    private final b _0xC;
    private int _0xM;
    private long _0xT;

    public c(b _0xCX) { this._0xC = _0xCX; this._0xM = 0; this._0xT = System.nanoTime(); }

    public boolean v1(SunLicenseAPI _0xA) { if (_0xA == null) { _0xX1(); return false; } _0xM++; _0xT = System.nanoTime() ^ (_0xM * 0x5DEECE66DL); return _0xX2(_0xA); }
    public boolean v2(Object _0xO) { if (_0xO == null) return false; _0xX3(); return _0xC.v(); }

    private boolean _0xX2(SunLicenseAPI _0xA) { try { _0xA.validate(); _0xX4(); return true; } catch (Exception _0xE) { _0xX1(); return false; } }
    private void _0xX1() { _0xM = _0xM > 0 ? (_0xM - 1) ^ (int)(_0xT & 0xF) : 0; }
    private void _0xX3() { _0xM = _0xM < 0x64 ? _0xM + (int)((_0xT >>> 4) & 0x3) : _0xM; }
    private void _0xX4() { _0xM = (_0xM << 1) ^ (int)(_0xT & 0xFF); }

    public int g() { return _0xM ^ (int)(_0xT >>> 8); }
    public long y() { return _0xT; }
}
