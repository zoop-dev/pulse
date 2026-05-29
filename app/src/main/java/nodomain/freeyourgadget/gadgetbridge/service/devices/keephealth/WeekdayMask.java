package nodomain.freeyourgadget.gadgetbridge.service.devices.keephealth;

public class WeekdayMask {
    public static final int ONCE = 0x01;
    public static final int MON  = 0x03;
    public static final int TUE  = 0x05;
    public static final int WED  = 0x09;
    public static final int THU  = 0x11;
    public static final int FRI  = 0x21;
    public static final int SAT  = 0x41;
    public static final int SUN  = 0x81;

    // single-bit constants (ONCE cleared) — use these for presence tests
    public static final int MON_BIT = MON & ~ONCE; // 0x02
    public static final int TUE_BIT = TUE & ~ONCE; // 0x04
    public static final int WED_BIT = WED & ~ONCE; // 0x08
    public static final int THU_BIT = THU & ~ONCE; // 0x10
    public static final int FRI_BIT = FRI & ~ONCE; // 0x20
    public static final int SAT_BIT = SAT & ~ONCE; // 0x40
    public static final int SUN_BIT = SUN & ~ONCE; // 0x80

}
