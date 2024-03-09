package net.plaaasma.autominecraft.mixin;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.plaaasma.autominecraft.AutoMinecraftClient;
import net.plaaasma.autominecraft.BaritoneUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (chatText.equals("--")) {
            AutoMinecraftClient.ENABLED = !AutoMinecraftClient.ENABLED;
            MinecraftClient.getInstance().player.sendMessage(AutoMinecraftClient.ENABLED ? Text.of("YEET!!") : Text.of("Fine I'll stop :("));

            if (!AutoMinecraftClient.ENABLED) {
                IBaritone primBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                primBaritone.getSelectionManager().removeAllSelections();
                BaritoneUtil.cancelAllGoals(primBaritone);
            }

            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            MinecraftClient.getInstance().setScreen(null);

            cir.setReturnValue(false);
        }
    }
}