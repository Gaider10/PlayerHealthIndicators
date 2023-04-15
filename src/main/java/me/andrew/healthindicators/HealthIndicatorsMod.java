package me.andrew.healthindicators;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class HealthIndicatorsMod implements ModInitializer {
    public static final String MOD_ID = "healthindicators";

    public static final KeyBinding RENDERING_ENABLED_KEY_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".toggle",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + MOD_ID
    ));
    public static final KeyBinding HEART_STACKING_ENABLED_KEY_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".heartStacking",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + MOD_ID
    ));

    public static boolean rendingEnabled = true;
    public static boolean heartStackingEnabled = true;

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (RENDERING_ENABLED_KEY_BINDING.wasPressed()) {
                rendingEnabled = !rendingEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal((rendingEnabled ? "Enabled" : "Disabled") + " Health Indicators"), true);
                }
            }

            while (HEART_STACKING_ENABLED_KEY_BINDING.wasPressed()) {
                heartStackingEnabled = !heartStackingEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal((heartStackingEnabled ? "Enabled" : "Disabled") + " Heart Stacking"), true);
                }
            }
        });
    }
}
