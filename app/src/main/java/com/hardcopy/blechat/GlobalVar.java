package com.hardcopy.blechat;

import android.app.Application;

public class GlobalVar extends Application{
    private static float ax;
    private static float ay;
    private static float az;

    private static float gx;
    private static float gy;
    private static float gz;

    private static float mx;
    private static float my;
    private static float mz;

    private static float yaw;
    private static float pitch;
    private static float roll;

    @Override
    public void onCreate(){

        super.onCreate();
    }
    @Override
    public void onTerminate(){
        super.onTerminate();
    }

    public static void setAx(float ax) { GlobalVar.ax = ax; }
    public static float getAx() { return GlobalVar.ax; }

    public static void setAy(float ay) { GlobalVar.ay = ay; }
    public static float getAy() { return GlobalVar.ay; }

    public static void setAz(float az) { GlobalVar.az = az; }
    public static float getAz() { return GlobalVar.az; }


    public static void setGx(float gx) { GlobalVar.gx = gx; }
    public static float getGx() { return GlobalVar.gx; }

    public static void setGy(float gy) { GlobalVar.gy = gy; }
    public static float getGy() { return GlobalVar.gy; }

    public static void setGz(float gz) { GlobalVar.gz = gz; }
    public static float getGz() { return GlobalVar.gz; }


    public static void setMx(float mx) { GlobalVar.mx = mx; }
    public static float getMx() { return GlobalVar.mx; }

    public static void setMy(float my) { GlobalVar.my = my; }
    public static float getMy() { return GlobalVar.my; }

    public static void setMz(float mz) { GlobalVar.mz = mz; }
    public static float getMz() { return GlobalVar.mz; }

    public static void setYaw(float yaw) { GlobalVar.yaw = yaw; }
    public static float getYaw() { return GlobalVar.yaw; }

    public static void setPitch(float pitch) { GlobalVar.pitch = pitch; }
    public static float getPitch() { return GlobalVar.pitch; }

    public static void setRoll(float roll) { GlobalVar.roll = roll; }
    public static float getRoll() { return GlobalVar.roll; }
}
