package me.andrew.healthindicators;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class HealthIndicatorsMod implements ModInitializer {
    public static final String MOD_ID = "healthindicators";

    public static final String CONFIG_FILE = "healthindicators.json";

    public static final KeyBinding RENDERING_ENABLED_KEY_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".renderingEnabled",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + MOD_ID
    ));
    public static final KeyBinding HEART_STACKING_ENABLED_KEY_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".heartStackingEnabled",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + MOD_ID
    ));
    public static final KeyBinding INCREASE_HEART_OFFSET_KEY_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".increaseHeartOffset",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + MOD_ID
    ));
    public static final KeyBinding DECREASE_HEART_OFFSET_KEY_BINDING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + MOD_ID + ".decreaseHeartOffset",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + MOD_ID
    ));

    @Override
    public void onInitialize() {
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (RENDERING_ENABLED_KEY_BINDING.wasPressed()) {
                Config.setRenderingEnabled(!Config.getRenderingEnabled());
                if (client.player != null) {
                    client.player.sendMessage(Text.literal((Config.getRenderingEnabled() ? "Enabled" : "Disabled") + " Health Indicators"), true);
                }
            }

            while (HEART_STACKING_ENABLED_KEY_BINDING.wasPressed()) {
                Config.setHeartStackingEnabled(!Config.getHeartStackingEnabled());
                if (client.player != null) {
                    client.player.sendMessage(Text.literal((Config.getHeartStackingEnabled() ? "Enabled" : "Disabled") + " Heart Stacking"), true);
                }
            }

            while (INCREASE_HEART_OFFSET_KEY_BINDING.wasPressed()) {
                Config.setHeartOffset(Config.getHeartOffset() + 1);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Set heart offset to " + Config.getHeartOffset()), true);
                }
            }

            while (DECREASE_HEART_OFFSET_KEY_BINDING.wasPressed()) {
                Config.setHeartOffset(Config.getHeartOffset() - 1);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Set heart offset to " + Config.getHeartOffset()), true);
                }
            }
        });
    }
}
