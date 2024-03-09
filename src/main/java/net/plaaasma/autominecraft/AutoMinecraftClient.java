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
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import baritone.api.BaritoneAPI;

import java.util.ArrayList;

public class AutoMinecraftClient implements ClientModInitializer {
    public static boolean ENABLED = false;
    public static boolean ON_MISSION = false;
    public static final Logger C_LOGGER = LoggerFactory.getLogger("autominecraftclient");

    private static ItemStack rootItemStack = new ItemStack(Items.AIR);
    private static BlockPos portalStartPos = null;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IBaritone primBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (primBaritone != null) {
                primBaritone.getSelectionManager().removeAllSelections();
                BaritoneUtil.cancelAllGoals(primBaritone);
                rootItemStack = new ItemStack(Items.AIR);
                portalStartPos = null;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (ENABLED) {
                    IBaritone primBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                    if (primBaritone != null) {
                        BaritoneUtil.yeetItems(client);  // Throw any items out that aren't whitelisted, the whitelist is a private object in the BaritoneUtil class
                        if (!BaritoneUtil.inProcess(primBaritone)) {
                            HitResult hitResult = client.crosshairTarget;

                            boolean hitCraftingTable = false;
                            boolean hitFurnace = false;
                            boolean hitObsidian = false;
                            BlockHitResult blockHitResult = null;
                            if (hitResult instanceof BlockHitResult) {
                                blockHitResult = (BlockHitResult) hitResult;

                                BlockState hitBlockState = client.world.getBlockState(blockHitResult.getBlockPos());

                                if (hitBlockState.getBlock() == Blocks.CRAFTING_TABLE) {
                                    hitCraftingTable = true;
                                }
                                else if (hitBlockState.getBlock() == Blocks.FURNACE) {
                                    hitFurnace = true;
                                }
                                else if (hitBlockState.getBlock() == Blocks.OBSIDIAN) {
                                    hitObsidian = true;
                                }
                            }

                            // PREPARE YOUR EYEBALLS
                            // THIS IS GOING TO HURT
                            // I CANNOT FATHOM A WAY TO METHODIZE THIS STUFF SO FUCK IT WE BALL

                            if (!ProgressChecks.hasItem(Items.CRAFTING_TABLE, 1, client.player.currentScreenHandler) && !hitCraftingTable) {  // If we don't have a crafting table, get the ingredients and make one.
                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 4, client.player.currentScreenHandler)) {
                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                        primBaritone.getMineProcess().mineByName("oak_log");
                                    } else {
                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                    }
                                } else {
                                    BaritoneUtil.craftItemSmall(Items.CRAFTING_TABLE, client);
                                }
                            }
                            else {  // If we do have a crafting table start working on progression.
                                if (client.currentScreen instanceof FurnaceScreen furnaceScreen) {
                                    // Slot Indexes
                                    // slot 2 is output, slot 1 is fuel input slot, slot 0 is material input slot
                                    // Slot IDS
                                    // id 32 is output, id 31 is fuel input slot, id 30 is material input
                                    FurnaceScreenHandler furnaceScreenHandler = furnaceScreen.getScreenHandler();

                                    if (!furnaceScreenHandler.getSlot(2).getStack().isOf(Items.AIR)) {
                                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, furnaceScreenHandler.getSlot(2).id, 0, SlotActionType.QUICK_MOVE, client.player);
                                    }
                                    else {
                                        if (furnaceScreenHandler.getSlot(0).getStack().isOf(Items.AIR)) {
                                            if (ProgressChecks.hasSmeltableItem(client.player.currentScreenHandler)) {
                                                if (furnaceScreenHandler.getSlot(1).getStack().isOf(Items.AIR)) {
                                                    if (ProgressChecks.hasFuelItem(client.player.currentScreenHandler)) {
                                                        int fuelID = ProgressChecks.getFuelID(client.player.currentScreenHandler);
                                                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, fuelID, 0, SlotActionType.QUICK_MOVE, client.player);
                                                        int smeltableID = ProgressChecks.getSmeltableID(client.player.currentScreenHandler);
                                                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, smeltableID, 0, SlotActionType.QUICK_MOVE, client.player);
                                                    }
                                                    else {
                                                        rootItemStack = new ItemStack(Items.FURNACE, 1);
                                                        primBaritone.getMineProcess().mineByName("furnace");
                                                        client.setScreen(null);
                                                    }
                                                } else {
                                                    int smeltableID = ProgressChecks.getSmeltableID(client.player.currentScreenHandler);
                                                    client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, smeltableID, 0, SlotActionType.QUICK_MOVE, client.player);
                                                }
                                            }
                                            else {
                                                rootItemStack = new ItemStack(Items.FURNACE, 1);
                                                primBaritone.getMineProcess().mineByName("furnace");
                                                client.setScreen(null);
                                            }
                                        }
                                    }
                                }
                                else {
                                    // Get flint and steel
                                    if (!ProgressChecks.hasItem(Items.FLINT_AND_STEEL, 1, client.player.currentScreenHandler)) {
                                        if (!ProgressChecks.hasItem(Items.IRON_INGOT, 1, client.player.currentScreenHandler)) {
                                            if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_PICKAXE, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.DIAMOND_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                    }
                                                } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.WOODEN_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                    rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                    primBaritone.getMineProcess().mineByName("oak_log");
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                }
                                                            } else {
                                                                BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                            }
                                                        } else if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 3, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                            } else {
                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
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
                                                                    } else {
                                                                        client.player.setPitch(90f);
                                                                    }
                                                                }
                                                            } else {
                                                                BaritoneUtil.craftItemLarge(Items.WOODEN_PICKAXE, client);
                                                                if (client.currentScreen != null) {
                                                                    client.currentScreen.close();
                                                                    client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                                }
                                                                rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                primBaritone.getMineProcess().mineByName("crafting_table");
                                                            }
                                                        }
                                                    } else {
                                                        rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                        primBaritone.getMineProcess().mineByName("stone");
                                                    }
                                                } else {
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
                                                            } else {
                                                                client.player.setPitch(90f);
                                                            }
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemLarge(Items.STONE_PICKAXE, client);
                                                        rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                        primBaritone.getMineProcess().mineByName("crafting_table");
                                                    }
                                                }
                                            }
                                            // Get everything we need for a stone axe and make one.
                                            else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_AXE, 1, client.player.currentScreenHandler)) {
                                                if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                    }
                                                } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                    rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                    primBaritone.getMineProcess().mineByName("stone");
                                                } else {
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
                                                            } else {
                                                                client.player.setPitch(90f);
                                                            }
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemLarge(Items.STONE_AXE, client);
                                                        rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                        primBaritone.getMineProcess().mineByName("crafting_table");
                                                    }
                                                }
                                            }
                                            // Get 1 coal if we don't have it.
                                            else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                                                rootItemStack = new ItemStack(Items.COAL, 1);
                                                primBaritone.getMineProcess().mineByName("coal_ore");
                                            }
                                            // Get 1 iron if we don't have it.
                                            else if (!ProgressChecks.hasItem(Items.RAW_IRON, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 1, client.player.currentScreenHandler)) {
                                                rootItemStack = new ItemStack(Items.RAW_IRON, 1);
                                                primBaritone.getMineProcess().mineByName("iron_ore");
                                            }
                                            // Get everything we need for a furnace and make one.
                                            else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                                                if (!ProgressChecks.hasItem(Items.COBBLESTONE, 8, client.player.currentScreenHandler)) {
                                                    rootItemStack = new ItemStack(Items.COBBLESTONE, 8);
                                                    primBaritone.getMineProcess().mineByName("stone");
                                                } else {
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
                                                            } else {
                                                                client.player.setPitch(90f);
                                                            }
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemLarge(Items.FURNACE, client);
                                                        rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                        primBaritone.getMineProcess().mineByName("crafting_table");
                                                    }
                                                }
                                            }
                                            // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
                                            else {
                                                if (hitFurnace) {
                                                    primBaritone.getCommandManager().execute("sel clear");
                                                    client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                                } else {
                                                    if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.FURNACE) {
                                                        BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                                                        primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                                                        primBaritone.getCommandManager().execute("sel fill furnace");
                                                        rootItemStack = new ItemStack(Items.AIR);
                                                    } else {
                                                        client.player.setPitch(90f);
                                                    }
                                                }
                                            }
                                        }
                                        else if (!ProgressChecks.hasItem(Items.FLINT, 1, client.player.currentScreenHandler)) {
                                            rootItemStack = new ItemStack(Items.FLINT, 1);
                                            primBaritone.getMineProcess().mineByName("gravel");
                                        }
                                        else {
                                            BaritoneUtil.craftItemSmall(Items.FLINT_AND_STEEL, client);
                                        }
                                    }
                                    else if (!ProgressChecks.hasItem(Items.OBSIDIAN, 10, client.player.currentScreenHandler) && !client.world.getDimension().toString().contains("nether")) {
                                        // Light nether portal and go through it
                                        if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() == Blocks.OBSIDIAN && ProgressChecks.hasItem(Items.FLINT_AND_STEEL, 1, client.player.currentScreenHandler)) {
                                            boolean moveToPortal = false;
                                            if (portalStartPos != null) {
                                                if (Math.sqrt(client.player.getBlockPos().getSquaredDistance(portalStartPos.getX(), portalStartPos.getY(), portalStartPos.getZ())) > 0) {
                                                    moveToPortal = true;
                                                }
                                            }
                                            if (moveToPortal) {
                                                primBaritone.getCommandManager().execute("goto " + portalStartPos.getX() + " " + portalStartPos.getY() + " " + portalStartPos.getZ());
                                                primBaritone.getCommandManager().execute("path");
                                                portalStartPos = null;
                                            }
                                            else {
                                                if (BaritoneUtil.blockInPortalRegion(client)) {
                                                    primBaritone.getSelectionManager().removeAllSelections();
                                                    BlockPos playerPos = client.player.getBlockPos();
                                                    portalStartPos = client.player.getBlockPos();
                                                    // Inside of the nether portal
                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(playerPos.getX() - 1, playerPos.getY(), playerPos.getZ()), new BetterBlockPos(playerPos.getX(), playerPos.getY() + 2, playerPos.getZ()));
                                                    // Execute command to place the obsidian
                                                    primBaritone.getCommandManager().execute("sel fill air");
                                                }
                                                else {
                                                    if (client.player.getStackInHand(client.player.getActiveHand()).isOf(Items.FLINT_AND_STEEL)) {
                                                        client.player.setPitch(90f);
                                                        client.interactionManager.interactBlock(client.player, client.player.getActiveHand(), blockHitResult);
                                                        portalStartPos = null;
                                                    }
                                                    else {
                                                        BaritoneUtil.equipItem(Items.FLINT_AND_STEEL, client, client.player.currentScreenHandler);
                                                    }
                                                }
                                            }
                                        }
                                        else {
                                            // Get obsidian
                                            if (!ProgressChecks.hasItem(Items.DIAMOND_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                if (!ProgressChecks.hasItem(Items.IRON_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                            } else {
                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                        }
                                                    } else if (!ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.WOODEN_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                                            } else {
                                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                        }
                                                                    } else if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 3, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
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
                                                                                } else {
                                                                                    client.player.setPitch(90f);
                                                                                }
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemLarge(Items.WOODEN_PICKAXE, client);
                                                                            if (client.currentScreen != null) {
                                                                                client.currentScreen.close();
                                                                                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                                            }
                                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                                        }
                                                                    }
                                                                } else {
                                                                    rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                    primBaritone.getMineProcess().mineByName("stone");
                                                                }
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_PICKAXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get everything we need for a stone axe and make one.
                                                        else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_AXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get 1 coal if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.COAL, 1);
                                                            primBaritone.getMineProcess().mineByName("coal_ore");
                                                        }
                                                        // Get 6 iron if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.RAW_IRON, 6, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 6, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.RAW_IRON, 6);
                                                            primBaritone.getMineProcess().mineByName("iron_ore");
                                                        }
                                                        // Get everything we need for a furnace and make one.
                                                        else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                                                            if (!ProgressChecks.hasItem(Items.COBBLESTONE, 8, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 8);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.FURNACE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
                                                        else {
                                                            if (hitFurnace) {
                                                                primBaritone.getCommandManager().execute("sel clear");
                                                                client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                                            } else {
                                                                if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.FURNACE) {
                                                                    BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                                                                    primBaritone.getCommandManager().execute("sel fill furnace");
                                                                    rootItemStack = new ItemStack(Items.AIR);
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        }
                                                    } else {
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
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemLarge(Items.IRON_PICKAXE, client);
                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                        }
                                                    }
                                                } else if (!ProgressChecks.hasItem(Items.IRON_AXE, 1, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                            } else {
                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                        }
                                                    } else if (!ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.WOODEN_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                                            } else {
                                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                        }
                                                                    } else if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 3, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
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
                                                                                } else {
                                                                                    client.player.setPitch(90f);
                                                                                }
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemLarge(Items.WOODEN_PICKAXE, client);
                                                                            if (client.currentScreen != null) {
                                                                                client.currentScreen.close();
                                                                                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                                            }
                                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                                        }
                                                                    }
                                                                } else {
                                                                    rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                    primBaritone.getMineProcess().mineByName("stone");
                                                                }
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_PICKAXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get everything we need for a stone axe and make one.
                                                        else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_AXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get 1 coal if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.COAL, 1);
                                                            primBaritone.getMineProcess().mineByName("coal_ore");
                                                        }
                                                        // Get 6 iron if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.RAW_IRON, 6, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 6, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.RAW_IRON, 6);
                                                            primBaritone.getMineProcess().mineByName("iron_ore");
                                                        }
                                                        // Get everything we need for a furnace and make one.
                                                        else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                                                            if (!ProgressChecks.hasItem(Items.COBBLESTONE, 8, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 8);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.FURNACE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
                                                        else {
                                                            if (hitFurnace) {
                                                                primBaritone.getCommandManager().execute("sel clear");
                                                                client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                                            } else {
                                                                if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.FURNACE) {
                                                                    BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                                                                    primBaritone.getCommandManager().execute("sel fill furnace");
                                                                    rootItemStack = new ItemStack(Items.AIR);
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        }
                                                    } else {
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
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemLarge(Items.IRON_AXE, client);
                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                        }
                                                    }
                                                } else if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                    }
                                                } else if (!ProgressChecks.hasItem(Items.DIAMOND, 3, client.player.currentScreenHandler)) {
                                                    rootItemStack = new ItemStack(Items.DIAMOND, 3);
                                                    primBaritone.getMineProcess().mineByName("diamond_ore");
                                                } else {
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
                                                            } else {
                                                                client.player.setPitch(90f);
                                                            }
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemLarge(Items.DIAMOND_PICKAXE, client);
                                                        rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                        primBaritone.getMineProcess().mineByName("crafting_table");
                                                    }
                                                }
                                            }
                                            else if (!ProgressChecks.hasItem(Items.DIAMOND_SHOVEL, 1, client.player.currentScreenHandler)) {
                                                if (!ProgressChecks.hasItem(Items.IRON_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                            } else {
                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                        }
                                                    } else if (!ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.WOODEN_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                                            } else {
                                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                        }
                                                                    } else if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 3, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
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
                                                                                } else {
                                                                                    client.player.setPitch(90f);
                                                                                }
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemLarge(Items.WOODEN_PICKAXE, client);
                                                                            if (client.currentScreen != null) {
                                                                                client.currentScreen.close();
                                                                                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                                            }
                                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                                        }
                                                                    }
                                                                } else {
                                                                    rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                    primBaritone.getMineProcess().mineByName("stone");
                                                                }
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_PICKAXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get everything we need for a stone axe and make one.
                                                        else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_AXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get 1 coal if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.COAL, 1);
                                                            primBaritone.getMineProcess().mineByName("coal_ore");
                                                        }
                                                        // Get 6 iron if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.RAW_IRON, 6, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 6, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.RAW_IRON, 6);
                                                            primBaritone.getMineProcess().mineByName("iron_ore");
                                                        }
                                                        // Get everything we need for a furnace and make one.
                                                        else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                                                            if (!ProgressChecks.hasItem(Items.COBBLESTONE, 8, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 8);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.FURNACE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
                                                        else {
                                                            if (hitFurnace) {
                                                                primBaritone.getCommandManager().execute("sel clear");
                                                                client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                                            } else {
                                                                if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.FURNACE) {
                                                                    BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                                                                    primBaritone.getCommandManager().execute("sel fill furnace");
                                                                    rootItemStack = new ItemStack(Items.AIR);
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        }
                                                    } else {
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
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemLarge(Items.IRON_PICKAXE, client);
                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                        }
                                                    }
                                                }
                                                else if (!ProgressChecks.hasItem(Items.IRON_AXE, 1, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                            } else {
                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                        }
                                                    } else if (!ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.WOODEN_PICKAXE, 1, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                            if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                                rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                                primBaritone.getMineProcess().mineByName("oak_log");
                                                                            } else {
                                                                                BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                        }
                                                                    } else if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 3, client.player.currentScreenHandler)) {
                                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                                        } else {
                                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
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
                                                                                } else {
                                                                                    client.player.setPitch(90f);
                                                                                }
                                                                            }
                                                                        } else {
                                                                            BaritoneUtil.craftItemLarge(Items.WOODEN_PICKAXE, client);
                                                                            if (client.currentScreen != null) {
                                                                                client.currentScreen.close();
                                                                                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
                                                                            }
                                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                                        }
                                                                    }
                                                                } else {
                                                                    rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                    primBaritone.getMineProcess().mineByName("stone");
                                                                }
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_PICKAXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get everything we need for a stone axe and make one.
                                                        else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler)) {
                                                            if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                                if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                                    if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                                        rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                                        primBaritone.getMineProcess().mineByName("oak_log");
                                                                    } else {
                                                                        BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                                }
                                                            } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.STONE_AXE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // Get 1 coal if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.COAL, 1);
                                                            primBaritone.getMineProcess().mineByName("coal_ore");
                                                        }
                                                        // Get 6 iron if we don't have it.
                                                        else if (!ProgressChecks.hasItem(Items.RAW_IRON, 6, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 6, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.RAW_IRON, 6);
                                                            primBaritone.getMineProcess().mineByName("iron_ore");
                                                        }
                                                        // Get everything we need for a furnace and make one.
                                                        else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                                                            if (!ProgressChecks.hasItem(Items.COBBLESTONE, 8, client.player.currentScreenHandler)) {
                                                                rootItemStack = new ItemStack(Items.COBBLESTONE, 8);
                                                                primBaritone.getMineProcess().mineByName("stone");
                                                            } else {
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
                                                                        } else {
                                                                            client.player.setPitch(90f);
                                                                        }
                                                                    }
                                                                } else {
                                                                    BaritoneUtil.craftItemLarge(Items.FURNACE, client);
                                                                    rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                                    primBaritone.getMineProcess().mineByName("crafting_table");
                                                                }
                                                            }
                                                        }
                                                        // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
                                                        else {
                                                            if (hitFurnace) {
                                                                primBaritone.getCommandManager().execute("sel clear");
                                                                client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
                                                            } else {
                                                                if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.FURNACE) {
                                                                    BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                                                                    primBaritone.getCommandManager().execute("sel fill furnace");
                                                                    rootItemStack = new ItemStack(Items.AIR);
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        }
                                                    } else {
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
                                                                } else {
                                                                    client.player.setPitch(90f);
                                                                }
                                                            }
                                                        } else {
                                                            BaritoneUtil.craftItemLarge(Items.IRON_AXE, client);
                                                            rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                            primBaritone.getMineProcess().mineByName("crafting_table");
                                                        }
                                                    }
                                                }
                                                else if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
                                                    if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
                                                        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
                                                            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
                                                            primBaritone.getMineProcess().mineByName("oak_log");
                                                        } else {
                                                            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemSmall(Items.STICK, client);
                                                    }
                                                }
                                                else if (!ProgressChecks.hasItem(Items.DIAMOND, 1, client.player.currentScreenHandler)) {
                                                    rootItemStack = new ItemStack(Items.DIAMOND, 1);
                                                    primBaritone.getMineProcess().mineByName("diamond_ore");
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
                                                            } else {
                                                                client.player.setPitch(90f);
                                                            }
                                                        }
                                                    } else {
                                                        BaritoneUtil.craftItemLarge(Items.DIAMOND_SHOVEL, client);
                                                        rootItemStack = new ItemStack(Items.CRAFTING_TABLE);
                                                        primBaritone.getMineProcess().mineByName("crafting_table");
                                                    }
                                                }
                                            }
                                            else {
                                                rootItemStack = new ItemStack(Items.OBSIDIAN, 10);
                                                primBaritone.getMineProcess().mineByName("obsidian");
                                            }
                                        }
                                    }
                                    else {
                                        if (!client.world.getDimension().toString().contains("nether")) {
                                            boolean moveToPortal = false;
                                            if (portalStartPos != null) {
                                                if (Math.sqrt(client.player.getBlockPos().getSquaredDistance(portalStartPos.getX(), portalStartPos.getY(), portalStartPos.getZ())) > 0) {
                                                    moveToPortal = true;
                                                }
                                            }
                                            if (moveToPortal) {
                                                primBaritone.getCommandManager().execute("goto " + portalStartPos.getX() + " " + portalStartPos.getY() + " " + portalStartPos.getZ());
                                                primBaritone.getCommandManager().execute("path");
                                                portalStartPos = null;
                                            }
                                            else {
                                                if (BaritoneUtil.blockInPortalRegion(client)) {
                                                    primBaritone.getSelectionManager().removeAllSelections();
                                                    BlockPos playerPos = client.player.getBlockPos();
                                                    portalStartPos = client.player.getBlockPos();
                                                    // Inside of the nether portal
                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(playerPos.getX() - 1, playerPos.getY(), playerPos.getZ()), new BetterBlockPos(playerPos.getX(), playerPos.getY() + 2, playerPos.getZ()));
                                                    // Execute command to place the obsidian
                                                    primBaritone.getCommandManager().execute("sel fill air");
                                                } else {
                                                    primBaritone.getSelectionManager().removeAllSelections();
                                                    BlockPos playerPos = client.player.getBlockPos();
                                                    portalStartPos = playerPos;
                                                    // Bottom of the nether portal
                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(playerPos.getX() - 1, playerPos.getY() - 1, playerPos.getZ()), new BetterBlockPos(playerPos.getX(), playerPos.getY() - 1, playerPos.getZ()));
                                                    // Top of the nether portal
                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(playerPos.getX() - 1, playerPos.getY() + 3, playerPos.getZ()), new BetterBlockPos(playerPos.getX(), playerPos.getY() + 3, playerPos.getZ()));
                                                    // Side 1 of the nether portal
                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(playerPos.getX() - 2, playerPos.getY(), playerPos.getZ()), new BetterBlockPos(playerPos.getX() - 2, playerPos.getY() + 2, playerPos.getZ()));
                                                    // Side 2 of the nether portal
                                                    primBaritone.getSelectionManager().addSelection(new BetterBlockPos(playerPos.getX() + 1, playerPos.getY(), playerPos.getZ()), new BetterBlockPos(playerPos.getX() + 1, playerPos.getY() + 2, playerPos.getZ()));
                                                    // Execute command to place the obsidian
                                                    primBaritone.getCommandManager().execute("sel fill obsidian");
                                                }
                                            }
                                        }
                                        else {
                                            primBaritone.getGetToBlockProcess().getToBlock(Blocks.SPAWNER);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (ProgressChecks.hasItem(rootItemStack.getItem(), rootItemStack.getCount(), client.player.currentScreenHandler) && !rootItemStack.isOf(Items.AIR)) {
                                BaritoneUtil.cancelAllGoals(primBaritone);
                                rootItemStack = new ItemStack(Items.AIR);
                            }
                            else{
                                if (rootItemStack.isOf(Items.OBSIDIAN) && client.world.getBlockState(client.player.getBlockPos()).getBlock() == Blocks.NETHER_PORTAL) {
                                    BaritoneUtil.cancelAllGoals(primBaritone);
                                    rootItemStack = new ItemStack(Items.AIR);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
