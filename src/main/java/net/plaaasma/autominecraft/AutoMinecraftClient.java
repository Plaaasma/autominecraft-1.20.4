package net.plaaasma.autominecraft;

import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import baritone.api.BaritoneAPI;

import java.util.ArrayList;

public class AutoMinecraftClient implements ClientModInitializer {
    public static final Text chatPrefix = Text.literal("AutoMC").withColor(Formatting.AQUA.getColorValue()).append(Text.literal(" -> ").withColor(Formatting.DARK_GRAY.getColorValue()));

    public static boolean NETHER = false;
    public static final Logger C_LOGGER = LoggerFactory.getLogger("autominecraftclient");

    @Override
    public void onInitializeClient() {
        BaritoneAPI.getSettings().logger.value = (component) -> { };
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IBaritone primBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (primBaritone != null) {
                primBaritone.getSelectionManager().removeAllSelections();
                BaritoneUtil.cancelAllGoals(primBaritone);
                AutomationUtil.rootItemStack = new ItemStack(Items.AIR);
                AutomationUtil.portalStartPos = null;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (NETHER) {
                    AutomationUtil.doAutoNether(client);
                }
            }
        });
    }
}
