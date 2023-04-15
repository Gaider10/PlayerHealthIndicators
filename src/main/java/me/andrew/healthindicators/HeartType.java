package me.andrew.healthindicators;

public enum HeartType {
    EMPTY(16 + 0 * 9, 0),
    RED_FULL(16 + 4 * 9, 0),
    RED_HALF(16 + 5 * 9, 0),
    YELLOW_FULL(16 + 16 * 9, 0),
    YELLOW_HALF(16 + 17 * 9, 0);

    public final int u;
    public final int v;

    HeartType(int u, int v) {
        this.u = u;
        this.v = v;
    }
}
