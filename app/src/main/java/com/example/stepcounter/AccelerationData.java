package com.example.stepcounter;

public class AccelerationData {
    private double value; // độ lớn của vecto gia tốc
    private float x;
    private float y;
    private float z;
    private long time;

    public double GetValue() {
        return value;
    }

    public void SetValue(double value) {
        this.value = value;
    }

    public float GetX() {
        return x;
    }

    public void SetX(float x) {
        this.x = x;
    }

    public float GetY() {
        return y;
    }

    public void SetY(float y) {
        this.y = y;
    }

    public float GetZ() {
        return z;
    }

    public void SetZ(float z) {
        this.z = z;
    }

    public long GetTime() {
        return time;
    }

    public void SetTime(long time) {
        this.time = time;
    }
}
