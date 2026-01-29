package net.exylia.commons.v2.peee;

import lombok.Getter;

@Getter
public class r {
    private final boolean _0xV;
    private final String _0xP;
    private final String _0xM;
    private final t _0xT;
    private final String _0xD;

    public r(boolean _0x1, String _0x2, String _0x3, t _0x4, String _0x5) { this._0xV = _0x1; this._0xP = _0x2; this._0xM = _0x3; this._0xT = _0x4; this._0xD = _0x5; }

    public boolean isValid() { return _0xV; }
    public String getProvider() { return _0xP; }
    public String getMessage() { return _0xM; }
    public t getErrorType() { return _0xT; }
    public String getDetailedReason() { return _0xD; }

    public static r s(String _0x1) { return new r(true, _0x1, null, t.N, null); }
    public static r f(String _0x1, t _0x2, String _0x3, String _0x4) { return new r(false, _0x1, _0x3, _0x2, _0x4); }

    public enum t { N, MF, IK, LE, LS, PM, HM, IM, CE, SE, SI, RL, U }
}
