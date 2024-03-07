package net.plaaasma.autominecraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AutoMinecraftClient implements ClientModInitializer {
    public static boolean ENABLED = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                AutoMinecraft.LOGGER.info("Enabled: " + ENABLED);
            }
        });
    }
}
