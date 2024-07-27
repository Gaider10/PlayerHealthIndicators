package me.andrew.healthindicators;

import net.minecraft.util.Identifier;

public enum HeartType {
    EMPTY(Identifier.ofVanilla("hud/heart/container")),
    RED_FULL(Identifier.ofVanilla("hud/heart/full")),
    RED_HALF(Identifier.ofVanilla("hud/heart/half")),
    YELLOW_FULL(Identifier.ofVanilla("hud/heart/absorbing_full")),
    YELLOW_HALF(Identifier.ofVanilla("hud/heart/absorbing_half"));

    public final Identifier texture;

    HeartType(Identifier texture) {
        this.texture = texture;
    }
}
