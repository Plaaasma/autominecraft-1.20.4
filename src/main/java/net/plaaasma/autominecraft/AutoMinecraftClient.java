package net.plaaasma.autominecraft;

import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import baritone.api.BaritoneAPI;

import java.util.ArrayList;

public class AutoMinecraftClient implements ClientModInitializer {
    public static boolean ENABLED = false;
    public static boolean ON_MISSION = false;
    public static final Logger C_LOGGER = LoggerFactory.getLogger("autominecraftclient");

    private static ItemStack rootItemStack = new ItemStack(Items.AIR);

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (ENABLED) {
                    IBaritone primBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                    if (primBaritone != null) {
                        if (!BaritoneUtil.inProcess(primBaritone)) {
                            HitResult hitResult = client.crosshairTarget;

                            boolean hitCraftingTable = false;
                            BlockHitResult blockHitResult = null;
                            if (hitResult != null) {
                                blockHitResult = (BlockHitResult) hitResult;

                                BlockState hitBlockState = client.world.getBlockState(blockHitResult.getBlockPos());

                                if (hitBlockState.getBlock() == Blocks.CRAFTING_TABLE) {
                                    hitCraftingTable = true;
                                }
                            }

                            if (!ProgressChecks.hasItem(Items.CRAFTING_TABLE, 1, client.player.getInventory()) && !hitCraftingTable) {
                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 4, client.player.getInventory())) {
                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.getInventory())) {
                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                        primBaritone.getMineProcess().mineByName("oak_log");
                                    } else {
                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                        client.currentScreen.close();
                                        client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                    }
                                } else {
                                    BaritoneUtil.craftItemSmall(Items.CRAFTING_TABLE, client);
                                    client.currentScreen.close();
                                    client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                }
                            }
                            else {
                                if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.getInventory())) {
                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.getInventory())) {
                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.getInventory())) {
                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.getInventory())) {
                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                primBaritone.getMineProcess().mineByName("oak_log");
                                            } else {
                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                client.currentScreen.close();
                                                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                            }
                                        } else {
                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                            client.currentScreen.close();
                                            client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                        }
                                    }
                                    else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.getInventory())) {
                                        if (!ProgressChecks.hasItem(Items.WOODEN_PICKAXE, 1, client.player.getInventory())) {
                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.getInventory())) {
                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.getInventory())) {
                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.getInventory())) {
                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                    } else {
                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                        client.currentScreen.close();
                                                        client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                    }
                                                } else {
                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                    client.currentScreen.close();
                                                    client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                }
                                            } else if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 3, client.player.getInventory())) {
                                                if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.getInventory())) {
                                                    rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                    primBaritone.getMineProcess().mineByName("oak_log");
                                                } else {
                                                    BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                    client.currentScreen.close();
                                                    client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                }
                                            } else {
                                                if (!(client.currentScreen instanceof CraftingScreen)) {
                                                    if (hitCraftingTable) {
                                                        primBaritone.getCommandManager().execute("sel clear");
                                                        client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                                    } else {
                                                        if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.CRAFTING_TABLE) {
                                                            BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                                                            primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                                                            primBaritone.getCommandManager().execute("sel fill crafting_table");
                                                            rootItemStack = new ItemStack(Items.AIR);
                                                        }
                                                        else {
                                                            client.player.setPitch(90f);
                                                        }
                                                    }
                                                } else {
                                                    BaritoneUtil.craftItemLarge(Items.WOODEN_PICKAXE, client);
//                                                    client.currentScreen.close();
//                                                    client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                }
                                            }
                                        } else {
                                            rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                            primBaritone.getMineProcess().mineByName("stone");
                                        }
                                    }
                                    else {
                                        if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                                            if (hitCraftingTable) {
                                                primBaritone.getCommandManager().execute("sel clear");
                                                client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                            } else {
                                                if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.CRAFTING_TABLE) {
                                                    BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                                                    primBaritone.getCommandManager().execute("sel fill crafting_table");
                                                    rootItemStack = new ItemStack(Items.AIR);
                                                }
                                                else {
                                                    client.player.setPitch(90f);
                                                }
                                            }
                                        }
                                        else {
                                            BaritoneUtil.craftItemLarge(Items.STONE_PICKAXE, client);
//                                            client.currentScreen.close();
//                                            client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                        }
                                    }
                                }
                            }
                        } else {
                            if (ProgressChecks.hasItem(rootItemStack.getItem(), rootItemStack.getCount(), client.player.getInventory()) && !rootItemStack.isOf(Items.AIR)) {
                                BaritoneUtil.cancelAllGoals(primBaritone);
                            }
                        }
                    }
                }
            }
        });
    }
}
