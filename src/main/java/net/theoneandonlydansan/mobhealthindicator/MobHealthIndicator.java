package net.theoneandonlydansan.mobhealthindicator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class MobHealthIndicator implements ModInitializer {
    public static final String MOD_ID = "mobhealthindicator";

    public static KeyBinding keyBinding;
    public static boolean toggled = true;

    @Override
    public void onInitialize() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".toggle",
                InputUtil.UNKNOWN_KEY.getCode(),
                "key.categories." + MOD_ID
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                toggled = !toggled;
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(MOD_ID + "." + (toggled ? "enabled" : "disabled")), true);
                }
            }
        });
    }
}