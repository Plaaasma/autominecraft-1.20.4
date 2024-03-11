package net.plaaasma.autominecraft;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Rotation;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class AutomationUtil {
    public static ItemStack rootItemStack = new ItemStack(Items.AIR);
    public static BlockPos portalStartPos = null;
    public static BlockPos waterPos = null;
    public static BlockPos lavaPos = null;
    public static int waterPlaceTime = 0;
    public static int waterPickupTime = 0;

    public static void handleFurnaceMenu(MinecraftClient client, IBaritone primBaritone, FurnaceScreenHandler furnaceScreenHandler) {
        // Slot Indexes
        // slot 2 is output, slot 1 is fuel input slot, slot 0 is material input slot
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
                            int smeltableIndex = ProgressChecks.getSmeltableIndex(client.player.currentScreenHandler);
                            doChatLogMessage(client, "Smelting " + client.player.currentScreenHandler.getSlot(smeltableIndex).getStack().getItem().getName().getString());
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

    public static void refreshInventory(MinecraftClient client) {
        for (Slot slot : client.player.currentScreenHandler.slots) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
        }
    }

    public static void doGetPlanks(MinecraftClient client, IBaritone primBaritone) {
        if (!ProgressChecks.hasItem(Items.OAK_LOG, 1, client.player.currentScreenHandler)) {
            doChatLogMessage(client, "Getting 1 oak log.");
            rootItemStack = new ItemStack(Items.OAK_LOG, 1);
            primBaritone.getMineProcess().mineByName("oak_log");
        } else {
            BaritoneUtil.craftItemSmall(Items.OAK_PLANKS, client);
        }
    }

    public static void doGetSticks(MinecraftClient client, IBaritone primBaritone) {
        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 2, client.player.currentScreenHandler)) {
            doGetPlanks(client, primBaritone);
        } else {
            BaritoneUtil.craftItemSmall(Items.STICK, client);
        }
    }

    public static void doGetCraftingTable(MinecraftClient client, IBaritone primBaritone) {
        if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 4, client.player.currentScreenHandler)) {
            doGetPlanks(client, primBaritone);
        } else {
            BaritoneUtil.craftItemSmall(Items.CRAFTING_TABLE, client);
        }
    }

    public static void doGetWoodenPickaxe(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
            doGetSticks(client, primBaritone);
        } else if (!ProgressChecks.hasItem(Items.OAK_PLANKS, 3, client.player.currentScreenHandler)) {
            doGetPlanks(client, primBaritone);
        } else {
            if (!(client.currentScreen instanceof CraftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.WOODEN_PICKAXE, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetStonePickaxe(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
            doGetSticks(client, primBaritone);
        } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
            if (!ProgressChecks.hasItem(Items.WOODEN_PICKAXE, 1, client.player.currentScreenHandler)) {
                doGetWoodenPickaxe(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                doChatLogMessage(client, "Getting 3 cobblestone.");
                rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
                primBaritone.getMineProcess().mineByName("stone");
            }
        } else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.STONE_PICKAXE, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetStoneAxe(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
            doGetSticks(client, primBaritone);
        } else if (!ProgressChecks.hasItem(Items.COBBLESTONE, 3, client.player.currentScreenHandler)) {
            doChatLogMessage(client, "Getting 3 cobblestone.");
            rootItemStack = new ItemStack(Items.COBBLESTONE, 3);
            primBaritone.getMineProcess().mineByName("stone");
        } else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.STONE_AXE, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetFurnace(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.COBBLESTONE, 8, client.player.currentScreenHandler)) {
            doChatLogMessage(client, "Getting 8 cobblestone.");
            rootItemStack = new ItemStack(Items.COBBLESTONE, 8);
            primBaritone.getMineProcess().mineByName("stone");
        } else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.FURNACE, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetFNS(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.IRON_INGOT, 1, client.player.currentScreenHandler)) {
            if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_PICKAXE, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.DIAMOND_PICKAXE, 1, client.player.currentScreenHandler)) {
                doGetStonePickaxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get everything we need for a stone axe and make one.
            else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_AXE, 1, client.player.currentScreenHandler)) {
                doGetStoneAxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get 1 coal if we don't have it.
            else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 1 coal.");
                rootItemStack = new ItemStack(Items.COAL, 1);
                primBaritone.getMineProcess().mineByName("coal_ore");
            }
            // Get 1 iron if we don't have it.
            else if (!ProgressChecks.hasItem(Items.RAW_IRON, 1, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 1, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 1 iron.");
                rootItemStack = new ItemStack(Items.RAW_IRON, 1);
                primBaritone.getMineProcess().mineByName("iron_ore");
            }
            // Get everything we need for a furnace and make one.
            else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                doGetFurnace(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
            else {
                handleOpenPlaceFurnace(client, primBaritone, hitFurnace, blockHitResult);
            }
        }
        else if (!ProgressChecks.hasItem(Items.FLINT, 1, client.player.currentScreenHandler)) {
            doChatLogMessage(client, "Getting 1 flint.");
            rootItemStack = new ItemStack(Items.FLINT, 1);
            primBaritone.getMineProcess().mineByName("gravel");
        }
        else {
            BaritoneUtil.craftItemSmall(Items.FLINT_AND_STEEL, client);
        }
    }

    public static void doGetIronPickaxe(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
            doGetSticks(client, primBaritone);
        } else if (!ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
            if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler)) {
                doGetStonePickaxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get everything we need for a stone axe and make one.
            else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler)) {
                doGetStoneAxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get 1 coal if we don't have it.
            else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 1 coal.");
                rootItemStack = new ItemStack(Items.COAL, 1);
                primBaritone.getMineProcess().mineByName("coal_ore");
            }
            // Get 6 iron if we don't have it.
            else if (!ProgressChecks.hasItem(Items.RAW_IRON, 6, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 6, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 6 iron.");
                rootItemStack = new ItemStack(Items.RAW_IRON, 6);
                primBaritone.getMineProcess().mineByName("iron_ore");
            }
            // Get everything we need for a furnace and make one.
            else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                doGetFurnace(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
            else {
                handleOpenPlaceFurnace(client, primBaritone, hitFurnace, blockHitResult);
            }
        } else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.IRON_PICKAXE, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetIronAxe(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
            doGetSticks(client, primBaritone);
        } else if (!ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
            if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler)) {
                doGetStonePickaxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get everything we need for a stone axe and make one.
            else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler)) {
                doGetStoneAxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get 1 coal if we don't have it.
            else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 1 coal.");
                rootItemStack = new ItemStack(Items.COAL, 1);
                primBaritone.getMineProcess().mineByName("coal_ore");
            }
            // Get 6 iron if we don't have it.
            else if (!ProgressChecks.hasItem(Items.RAW_IRON, 6, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 6, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 6 iron.");
                rootItemStack = new ItemStack(Items.RAW_IRON, 6);
                primBaritone.getMineProcess().mineByName("iron_ore");
            }
            // Get everything we need for a furnace and make one.
            else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                doGetFurnace(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
            else {
                handleOpenPlaceFurnace(client, primBaritone, hitFurnace, blockHitResult);
            }
        } else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.IRON_AXE, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetBucket(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
            if (!ProgressChecks.hasItem(Items.STONE_PICKAXE, 1, client.player.currentScreenHandler)) {
                doGetStonePickaxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get everything we need for a stone axe and make one.
            else if (!ProgressChecks.hasItem(Items.STONE_AXE, 1, client.player.currentScreenHandler)) {
                doGetStoneAxe(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // Get 1 coal if we don't have it.
            else if (!ProgressChecks.hasItem(Items.COAL, 1, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 1 coal.");
                rootItemStack = new ItemStack(Items.COAL, 1);
                primBaritone.getMineProcess().mineByName("coal_ore");
            }
            // Get 6 iron if we don't have it.
            else if (!ProgressChecks.hasItem(Items.RAW_IRON, 3, client.player.currentScreenHandler) && !ProgressChecks.hasItem(Items.IRON_INGOT, 3, client.player.currentScreenHandler)) {
                doChatLogMessage(client, "Getting 3 iron.");
                rootItemStack = new ItemStack(Items.RAW_IRON, 3);
                primBaritone.getMineProcess().mineByName("iron_ore");
            }
            // Get everything we need for a furnace and make one.
            else if (!ProgressChecks.hasItem(Items.FURNACE, 1, client.player.currentScreenHandler) && !hitFurnace) {
                doGetFurnace(client, primBaritone, hitCraftingTable, blockHitResult);
            }
            // If we have everything to make iron ingots, place the furnace and open it to smelt all ores.
            else {
                handleOpenPlaceFurnace(client, primBaritone, hitFurnace, blockHitResult);
            }
        } else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.BUCKET, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetDiamondPickaxe(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.IRON_PICKAXE, 1, client.player.currentScreenHandler)) {
            doGetIronPickaxe(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
        } else if (!ProgressChecks.hasItem(Items.IRON_AXE, 1, client.player.currentScreenHandler)) {
            doGetIronAxe(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
        } else if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
            doGetSticks(client, primBaritone);
        } else if (!ProgressChecks.hasItem(Items.DIAMOND, 3, client.player.currentScreenHandler)) {
            doChatLogMessage(client, "Getting 3 diamonds.");
            rootItemStack = new ItemStack(Items.DIAMOND, 3);
            primBaritone.getMineProcess().mineByName("diamond_ore");
        } else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.DIAMOND_PICKAXE, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void doGetDiamondShovel(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.IRON_PICKAXE, 1, client.player.currentScreenHandler)) {
            doGetIronPickaxe(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
        }
        else if (!ProgressChecks.hasItem(Items.IRON_AXE, 1, client.player.currentScreenHandler)) {
            doGetIronAxe(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
        }
        else if (!ProgressChecks.hasItem(Items.STICK, 2, client.player.currentScreenHandler)) {
            doGetSticks(client, primBaritone);
        }
        else if (!ProgressChecks.hasItem(Items.DIAMOND, 1, client.player.currentScreenHandler)) {
            doChatLogMessage(client, "Getting 1 diamond.");
            rootItemStack = new ItemStack(Items.DIAMOND, 1);
            primBaritone.getMineProcess().mineByName("diamond_ore");
        }
        else {
            if (!(client.currentScreen instanceof CraftingScreen craftingScreen)) {
                handleOpenPlaceCrafting(client, primBaritone, hitCraftingTable, blockHitResult);
            } else {
                BaritoneUtil.craftItemLarge(Items.DIAMOND_SHOVEL, client);
                handleDestroyCrafting(client, primBaritone);
            }
        }
    }

    public static void handleOpenPlaceCrafting(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, BlockHitResult blockHitResult) {
        if (hitCraftingTable) {
            doChatLogMessage(client, "Opening crafting table GUI.");
            primBaritone.getCommandManager().execute("sel clear");
            client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
        } else {
            if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.CRAFTING_TABLE) {
                doChatLogMessage(client, "Placing crafting table.");
                BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                primBaritone.getCommandManager().execute("sel fill crafting_table");
                rootItemStack = new ItemStack(Items.AIR);
            } else {
                client.player.setPitch(90f);
            }
        }
    }

    public static void handleDestroyCrafting(MinecraftClient client, IBaritone primBaritone) {
        doChatLogMessage(client, "Removing crafting table.");
        if (client.currentScreen != null) {
            client.currentScreen.close();
            client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(client.player.currentScreenHandler.syncId));
        }
        rootItemStack = new ItemStack(Items.CRAFTING_TABLE, 1);
        primBaritone.getMineProcess().mineByName("crafting_table");
    }

    public static void handleOpenPlaceFurnace(MinecraftClient client, IBaritone primBaritone, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (hitFurnace) {
            doChatLogMessage(client, "Opening furnace GUI.");
            primBaritone.getCommandManager().execute("sel clear");
            client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, 0));
        } else {
            if (client.world.getBlockState(client.player.getBlockPos().offset(Direction.Axis.Y, -1)).getBlock() != Blocks.FURNACE) {
                doChatLogMessage(client, "Placing furnace.");
                BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
                primBaritone.getSelectionManager().addSelection(new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()), new BetterBlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ()));
                primBaritone.getCommandManager().execute("sel fill furnace");
                rootItemStack = new ItemStack(Items.AIR);
            } else {
                client.player.setPitch(90f);
            }
        }
    }

    public static void handlePortalNavAndLight(MinecraftClient client, IBaritone primBaritone, BlockHitResult blockHitResult) {
        boolean moveToPortal = false;
        if (portalStartPos != null) {
            if (Math.sqrt(client.player.getBlockPos().getSquaredDistance(portalStartPos.getX(), portalStartPos.getY(), portalStartPos.getZ())) > 0) {
                moveToPortal = true;
            }
        }
        if (moveToPortal) {
            doChatLogMessage(client, "Moving to portal location.");
            primBaritone.getCommandManager().execute("goto " + portalStartPos.getX() + " " + portalStartPos.getY() + " " + portalStartPos.getZ());
            portalStartPos = null;
        }
        else {
            if (BaritoneUtil.blockInPortalRegion(client)) {
                doChatLogMessage(client, "Clearing portal area.");
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
                    if (!client.world.getBlockState(client.player.getBlockPos()).isOf(Blocks.NETHER_PORTAL)) {
                        doChatLogMessage(client, "Lighting portal.");
                        client.player.setPitch(90f);
                        client.interactionManager.interactBlock(client.player, client.player.getActiveHand(), blockHitResult);
                    }
                    portalStartPos = null;
                }
                else {
                    doChatLogMessage(client, "Equipping flint and steel.");
                    BaritoneUtil.equipItem(Items.FLINT_AND_STEEL, client, client.player.currentScreenHandler);
                }
            }
        }
    }

    public static void doBuildPortal(MinecraftClient client, IBaritone primBaritone) {
        boolean moveToPortal = false;
        if (portalStartPos != null) {
            if (Math.sqrt(client.player.getBlockPos().getSquaredDistance(portalStartPos.getX(), portalStartPos.getY(), portalStartPos.getZ())) > 0) {
                moveToPortal = true;
            }
        }
        if (moveToPortal) {
            doChatLogMessage(client, "Moving to portal location.");
            primBaritone.getCommandManager().execute("goto " + portalStartPos.getX() + " " + portalStartPos.getY() + " " + portalStartPos.getZ());
            portalStartPos = null;
        }
        else {
            if (BaritoneUtil.blockInPortalRegion(client)) {
                doChatLogMessage(client, "Clearing portal area.");
                primBaritone.getSelectionManager().removeAllSelections();
                BlockPos playerPos = client.player.getBlockPos();
                portalStartPos = client.player.getBlockPos();
                // Inside of the nether portal
                primBaritone.getSelectionManager().addSelection(new BetterBlockPos(playerPos.getX() - 1, playerPos.getY(), playerPos.getZ()), new BetterBlockPos(playerPos.getX(), playerPos.getY() + 2, playerPos.getZ()));
                // Execute command to place the obsidian
                primBaritone.getCommandManager().execute("sel fill air");
            } else {
                doChatLogMessage(client, "Building portal.");
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

    public static boolean standingInOverworldPortal(MinecraftClient client) {
        return rootItemStack.isOf(Items.OBSIDIAN) && client.world.getBlockState(client.player.getBlockPos()).getBlock() == Blocks.NETHER_PORTAL;
    }

    public static boolean doneWithTask(MinecraftClient client) {
        return ProgressChecks.hasItem(rootItemStack.getItem(), rootItemStack.getCount(), client.player.currentScreenHandler) && !rootItemStack.isOf(Items.AIR);
    }

    public static BlockPos getWaterPos(MinecraftClient client) {
        ClientWorld clientWorld = client.world;
        BlockPos clientPos = client.player.getBlockPos();
        BlockPos fWaterPos = null;
        for (int x_offset = 4; x_offset >= -4 && fWaterPos == null; x_offset--) {
            for (int y_offset = 4; y_offset >= -4 && fWaterPos == null; y_offset--) {
                for (int z_offset = 4; z_offset >= -4 && fWaterPos == null; z_offset--) {
                    BlockPos offsetPos = new BlockPos(clientPos.getX() + x_offset, clientPos.getY() + y_offset, clientPos.getZ() + z_offset);
                    if (offsetPos.getY() <= 40) {
                        BlockPos upOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ());

                        BlockState stateAtOffsetPos = clientWorld.getBlockState(offsetPos);
                        BlockState stateAtupOffsetPos = clientWorld.getBlockState(upOffsetPos);

                        BlockPos xOffsetEndPos = new BlockPos(offsetPos.getX() + 1, offsetPos.getY() + 1, offsetPos.getZ());
                        BlockState stateAtXEndPos = clientWorld.getBlockState(xOffsetEndPos);
                        BlockPos zOffsetEndPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ() + 1);
                        BlockState stateAtZEndPos = clientWorld.getBlockState(zOffsetEndPos);
                        BlockPos negXOffsetEndPos = new BlockPos(offsetPos.getX() - 1, offsetPos.getY() + 1, offsetPos.getZ());
                        BlockState stateAtNegXEndPos = clientWorld.getBlockState(negXOffsetEndPos);
                        BlockPos negZOffsetEndPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ() - 1);
                        BlockState stateAtNegZEndPos = clientWorld.getBlockState(negZOffsetEndPos);

                        boolean waterTouching = (stateAtXEndPos.isOf(Blocks.WATER) || stateAtZEndPos.isOf(Blocks.WATER) || stateAtNegXEndPos.isOf(Blocks.WATER) || stateAtNegZEndPos.isOf(Blocks.WATER));

                        if ((stateAtOffsetPos.isOf(Blocks.WATER) && stateAtOffsetPos.get(FluidBlock.LEVEL) == 0) && !stateAtupOffsetPos.isOf(Blocks.WATER) && !waterTouching) {
                            fWaterPos = offsetPos;
                        }
                    }
                }
            }
        }
        if (fWaterPos == null) {
            for (int x_offset = 32; x_offset >= -32 && fWaterPos == null; x_offset--) {
                for (int y_offset = 32; y_offset >= -32 && fWaterPos == null; y_offset--) {
                    for (int z_offset = 32; z_offset >= -32 && fWaterPos == null; z_offset--) {
                        BlockPos offsetPos = new BlockPos(clientPos.getX() + x_offset, clientPos.getY() + y_offset, clientPos.getZ() + z_offset);
                        if (offsetPos.getY() <= 40) {
                            BlockPos upOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ());

                            BlockState stateAtOffsetPos = clientWorld.getBlockState(offsetPos);
                            BlockState stateAtupOffsetPos = clientWorld.getBlockState(upOffsetPos);

                            BlockPos xOffsetEndPos = new BlockPos(offsetPos.getX() + 1, offsetPos.getY() + 1, offsetPos.getZ());
                            BlockState stateAtXEndPos = clientWorld.getBlockState(xOffsetEndPos);
                            BlockPos zOffsetEndPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ() + 1);
                            BlockState stateAtZEndPos = clientWorld.getBlockState(zOffsetEndPos);
                            BlockPos negXOffsetEndPos = new BlockPos(offsetPos.getX() - 1, offsetPos.getY() + 1, offsetPos.getZ());
                            BlockState stateAtNegXEndPos = clientWorld.getBlockState(negXOffsetEndPos);
                            BlockPos negZOffsetEndPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ() - 1);
                            BlockState stateAtNegZEndPos = clientWorld.getBlockState(negZOffsetEndPos);

                            boolean waterTouching = (stateAtXEndPos.isOf(Blocks.WATER) || stateAtZEndPos.isOf(Blocks.WATER) || stateAtNegXEndPos.isOf(Blocks.WATER) || stateAtNegZEndPos.isOf(Blocks.WATER));

                            if ((stateAtOffsetPos.isOf(Blocks.WATER) && stateAtOffsetPos.get(FluidBlock.LEVEL) == 0) && !stateAtupOffsetPos.isOf(Blocks.WATER) && !waterTouching) {
                                fWaterPos = offsetPos;
                            }
                        }
                    }
                }
            }
        }

        return fWaterPos;
    }

    public static BlockPos getLavaPos(MinecraftClient client) {
        ClientWorld clientWorld = client.world;
        BlockPos clientPos = client.player.getBlockPos();
        BlockPos fLavaPos = null;
        for (int x_offset = 64; x_offset >= -64 && fLavaPos == null; x_offset--) {
            for (int y_offset = 64; y_offset >= -64 && fLavaPos == null; y_offset--) {
                for (int z_offset = 64; z_offset >= -64 && fLavaPos == null; z_offset--) {
                    BlockPos offsetPos = new BlockPos(clientPos.getX() + x_offset, clientPos.getY() + y_offset, clientPos.getZ() + z_offset);
                    BlockPos upOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ());
                    BlockPos xOffsetPos = new BlockPos(offsetPos.getX() + 1, offsetPos.getY(), offsetPos.getZ());
                    BlockPos zOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ() + 1);
                    BlockPos negXOffsetPos = new BlockPos(offsetPos.getX() - 1, offsetPos.getY(), offsetPos.getZ());
                    BlockPos negZOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ() - 1);

                    BlockState stateAtOffsetPos = clientWorld.getBlockState(offsetPos);
                    BlockState stateAtUpOffsetPos = clientWorld.getBlockState(upOffsetPos);
                    BlockState stateAtXOffsetPos = clientWorld.getBlockState(xOffsetPos);
                    BlockState stateAtZOffsetPos = clientWorld.getBlockState(zOffsetPos);
                    BlockState stateAtNegXOffsetPos = clientWorld.getBlockState(negXOffsetPos);
                    BlockState stateAtNegZOffsetPos = clientWorld.getBlockState(negZOffsetPos);

                    if ((stateAtOffsetPos.isOf(Blocks.LAVA) && stateAtOffsetPos.get(FluidBlock.LEVEL) == 0) && (stateAtUpOffsetPos.isOf(Blocks.AIR) || stateAtUpOffsetPos.isOf(Blocks.CAVE_AIR)) && !stateAtXOffsetPos.isOf(Blocks.LAVA) && (stateAtZOffsetPos.isOf(Blocks.LAVA) && stateAtZOffsetPos.get(FluidBlock.LEVEL) == 0) && (stateAtNegXOffsetPos.isOf(Blocks.LAVA) && stateAtNegXOffsetPos.get(FluidBlock.LEVEL) == 0) && (stateAtNegZOffsetPos.isOf(Blocks.LAVA) && stateAtNegZOffsetPos.get(FluidBlock.LEVEL) == 0)) {
                        fLavaPos = xOffsetPos.offset(Direction.Axis.Y, 1);
                    }
                }
            }
        }

        return fLavaPos;
    }

    public static void doGetWaterBucket(MinecraftClient client, IBaritone primBaritone, boolean hitCraftingTable, boolean hitFurnace, BlockHitResult blockHitResult) {
        if (!ProgressChecks.hasItem(Items.BUCKET, 1, client.player.currentScreenHandler)) {
            doGetBucket(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
        }
        else {
            BlockPos offsetPos = client.player.getBlockPos().offset(Direction.Axis.Y, -1);
            if ((client.world.getBlockState(offsetPos).getBlock() == Blocks.WATER && client.world.getBlockState(offsetPos).get(FluidBlock.LEVEL) == 0) || (client.world.getBlockState(client.player.getBlockPos()).getBlock() == Blocks.WATER && client.world.getBlockState(client.player.getBlockPos()).get(FluidBlock.LEVEL) == 0)) {
                if (client.player.getStackInHand(client.player.getActiveHand()).isOf(Items.BUCKET)) {
                    primBaritone.getSelectionManager().removeAllSelections();
                    BaritoneUtil.cancelAllGoals(primBaritone);
                    doChatLogMessage(client, "Filling bucket with water.");
                    client.player.setPitch(90f);
                    client.interactionManager.interactItem(client.player, client.player.getActiveHand());
                    waterPickupTime = (int) client.world.getTime();
                    waterPos = null;
                }
                else {
                    doChatLogMessage(client, "Equipping bucket.");
                    BaritoneUtil.equipItem(Items.BUCKET, client, client.player.currentScreenHandler);
                }
            }
            else {
                if (waterPos == null) {
                    doChatLogMessage(client, "Attempting to find water nearby.");
                    waterPos = getWaterPos(client);
                    if (waterPos == null) {
                        doChatLogMessage(client, "Water search unsuccessful, wandering.");
                        primBaritone.getExploreProcess().explore(16, 0);
                    }
                } else {
                    if (!client.world.getBlockState(waterPos).isOf(Blocks.WATER)) {
                        waterPos = null;
                    }
                    else {
                        if (Math.sqrt(waterPos.getSquaredDistance(client.player.getPos().x, client.player.getPos().y, client.player.getPos().z)) <= 4.5) {
//                            float newYaw = calculateLookYaw(client, waterPos.getX(), waterPos.getZ());
//                            float newPitch = calculateLookPitch(client, waterPos.getX(), waterPos.getY(), waterPos.getZ());
//                            client.player.setYaw(newYaw);
//                            client.player.setPitch(newPitch);
                            client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, waterPos.toCenterPos());
                            if (client.player.getStackInHand(client.player.getActiveHand()).isOf(Items.BUCKET)) {
                                primBaritone.getSelectionManager().removeAllSelections();
                                BaritoneUtil.cancelAllGoals(primBaritone);
                                doChatLogMessage(client, "Filling bucket with water.");
                                client.interactionManager.interactItem(client.player, client.player.getActiveHand());
                                waterPickupTime = (int) client.world.getTime();
                                waterPos = null;
                            }
                            else {
                                doChatLogMessage(client, "Equipping bucket.");
                                BaritoneUtil.equipItem(Items.BUCKET, client, client.player.currentScreenHandler);
                            }
                        }
                        else {
                            doChatLogMessage(client, "Going to water location. " + waterPos);
                            primBaritone.getCommandManager().execute("goto " + waterPos.getX() + " " + waterPos.getY() + " " + waterPos.getZ());
                        }
                    }
                }
            }
        }
    }

    public static float calculateLookYaw(MinecraftClient client, int targetX, int targetZ) {
        double d0 = targetX - client.player.getBlockX();
        double d2 = targetZ - client.player.getBlockZ();

        float f = (float) (MathUtil.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
        return MathUtil.rotlerp(client.player.getYaw(), f, 90F);
    }

    public static float calculateLookPitch(MinecraftClient client, int targetX, int targetY, int targetZ) {
        double d0 = targetX - client.player.getBlockX();
        double d2 = targetZ - client.player.getBlockZ();
        double d1;

        d1 = targetY - client.player.getEyeY();

        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float f1 = (float)(-(MathUtil.atan2(d1, d3) * (double)(180F / (float)Math.PI)));
        return MathUtil.rotlerp(client.player.getPitch(), f1, 90F);
    }

    public static void doLavaToObsidian(MinecraftClient client, IBaritone primBaritone) {
        if (lavaPos == null) {
            doChatLogMessage(client, "Attempting to find lava nearby.");
            lavaPos = getLavaPos(client);
            if (lavaPos == null) {
                doChatLogMessage(client, "Lava search unsuccessful, wandering.");
                primBaritone.getExploreProcess().explore(16, 0);
            }
        } else {
            double distanceToLavaPool = Math.sqrt(lavaPos.getSquaredDistance(client.player.getX(), client.player.getY(), client.player.getZ()));
            if (distanceToLavaPool > 1) {
                doChatLogMessage(client, "Going to Lava location.");
                primBaritone.getCommandManager().execute("goto " + lavaPos.getX() + " " + lavaPos.getY() + " " + lavaPos.getZ());
            }
            else {
                if (client.player.getStackInHand(client.player.getActiveHand()).isOf(Items.WATER_BUCKET)) {
                    primBaritone.getSelectionManager().removeAllSelections();
                    BaritoneUtil.cancelAllGoals(primBaritone);
                    doChatLogMessage(client, "Turning lava into obsidian.");
                    client.player.setPitch(90f);
                    client.interactionManager.interactItem(client.player, client.player.getActiveHand());
                    waterPlaceTime = (int) client.world.getTime();
                    lavaPos = null;
                }
                else {
                    doChatLogMessage(client, "Equipping water bucket.");
                    BaritoneUtil.equipItem(Items.WATER_BUCKET, client, client.player.currentScreenHandler);
                }
            }
        }
    }

    public static int getNearbyObbyCount(MinecraftClient client) {
        ClientWorld clientWorld = client.world;
        BlockPos clientPos = client.player.getBlockPos();
        int obsidianCount = 0;
        for (int x_offset = -32; x_offset <= 32; x_offset++) {
            for (int y_offset = -32; y_offset <= 32; y_offset++) {
                for (int z_offset = -32; z_offset <= 32; z_offset++) {
                    BlockPos offsetPos = new BlockPos(clientPos.getX() + x_offset, clientPos.getY() + y_offset, clientPos.getZ() + z_offset);
                    BlockPos upOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY() + 1, offsetPos.getZ());
                    BlockPos xOffsetPos = new BlockPos(offsetPos.getX() + 1, offsetPos.getY(), offsetPos.getZ());
                    BlockPos zOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ() + 1);
                    BlockPos negXOffsetPos = new BlockPos(offsetPos.getX() - 1, offsetPos.getY(), offsetPos.getZ());
                    BlockPos negZOffsetPos = new BlockPos(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ() - 1);

                    BlockState stateAtOffsetPos = clientWorld.getBlockState(offsetPos);
                    BlockState stateAtUpOffsetPos = clientWorld.getBlockState(upOffsetPos);
                    BlockState stateAtXOffsetPos = clientWorld.getBlockState(xOffsetPos);
                    BlockState stateAtZOffsetPos = clientWorld.getBlockState(zOffsetPos);
                    BlockState stateAtNegXOffsetPos = clientWorld.getBlockState(negXOffsetPos);
                    BlockState stateAtNegZOffsetPos = clientWorld.getBlockState(negZOffsetPos);


                    if (stateAtOffsetPos.isOf(Blocks.OBSIDIAN) && !stateAtUpOffsetPos.isOf(Blocks.WATER) && !stateAtUpOffsetPos.isOf(Blocks.LAVA) && !stateAtXOffsetPos.isOf(Blocks.LAVA) && !stateAtZOffsetPos.isOf(Blocks.LAVA) && !stateAtNegXOffsetPos.isOf(Blocks.LAVA) && !stateAtNegZOffsetPos.isOf(Blocks.LAVA)) {
                        obsidianCount += 1;
                    }
                }
            }
        }

        return obsidianCount;
    }

    public static boolean mineObsidian(MinecraftClient client, IBaritone primBaritone) {
        int obsidianCount = getNearbyObbyCount(client);
        if (obsidianCount > 0) {
            int heldObsidianCount = ProgressChecks.getItemCount(Items.OBSIDIAN, client.player.currentScreenHandler);
            int maxObsidianCount = 10 - heldObsidianCount;
            if (obsidianCount > maxObsidianCount) {
                obsidianCount = maxObsidianCount;
            }

            doChatLogMessage(client, "Getting " + obsidianCount + " obsidian.");
            rootItemStack = new ItemStack(Items.OBSIDIAN, heldObsidianCount + 1);
            primBaritone.getMineProcess().mineByName("obsidian");

            return true;
        }

        return false;
    }

    public static boolean standingInPortalShape(MinecraftClient client) {
        ClientWorld clientWorld = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        boolean bottomMade = false;
        boolean topMade = false;
        boolean side1Made = false;
        boolean side2Made = false;
        if (clientWorld.getBlockState(new BlockPos(playerPos.getX() - 1, playerPos.getY() - 1, playerPos.getZ())).isOf(Blocks.OBSIDIAN)
        && clientWorld.getBlockState(new BlockPos(playerPos.getX(), playerPos.getY() - 1, playerPos.getZ())).isOf(Blocks.OBSIDIAN)) {
            bottomMade = true;
        }

        if (clientWorld.getBlockState(new BlockPos(playerPos.getX() - 1, playerPos.getY() + 3, playerPos.getZ())).isOf(Blocks.OBSIDIAN)
                && clientWorld.getBlockState(new BlockPos(playerPos.getX(), playerPos.getY() + 3, playerPos.getZ())).isOf(Blocks.OBSIDIAN)) {
            topMade = true;
        }

        if (clientWorld.getBlockState(new BlockPos(playerPos.getX() - 2, playerPos.getY(), playerPos.getZ())).isOf(Blocks.OBSIDIAN)
                && clientWorld.getBlockState(new BlockPos(playerPos.getX() - 2, playerPos.getY() + 1, playerPos.getZ())).isOf(Blocks.OBSIDIAN)
                && clientWorld.getBlockState(new BlockPos(playerPos.getX() - 2, playerPos.getY() + 2, playerPos.getZ())).isOf(Blocks.OBSIDIAN)) {
            side1Made = true;
        }

        if (clientWorld.getBlockState(new BlockPos(playerPos.getX() + 1, playerPos.getY(), playerPos.getZ())).isOf(Blocks.OBSIDIAN)
                && clientWorld.getBlockState(new BlockPos(playerPos.getX() + 1, playerPos.getY() + 1, playerPos.getZ())).isOf(Blocks.OBSIDIAN)
                && clientWorld.getBlockState(new BlockPos(playerPos.getX() + 1, playerPos.getY() + 2, playerPos.getZ())).isOf(Blocks.OBSIDIAN)) {
            side2Made = true;
        }
        if (bottomMade && topMade && side1Made && side2Made) {
            return true;
        }
        return false;
    }

    public static void doAutoNether(MinecraftClient client) {
        if (client.world.getDimension().toString().contains("nether")) {
            AutoMinecraftClient.NETHER = false;
            doChatLogMessage(client, "Arrived in nether.");
        }
        IBaritone primBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (primBaritone != null) {
            BaritoneUtil.yeetItems(client);  // Throw any items out that aren't whitelisted, the whitelist is a private object in the BaritoneUtil class
            if (!BaritoneUtil.inProcess(primBaritone)) {
                HitResult hitResult = client.crosshairTarget;

                boolean hitCraftingTable = false;
                boolean hitFurnace = false;
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
                }

                // If we don't have a crafting table, get the ingredients and make one.
                if (!ProgressChecks.hasItem(Items.CRAFTING_TABLE, 1, client.player.currentScreenHandler) && !hitCraftingTable) {
                    doGetCraftingTable(client, primBaritone);
                }
                else {  // If we do have a crafting table do other stuff.
                    if (client.currentScreen instanceof FurnaceScreen furnaceScreen) {
                        FurnaceScreenHandler furnaceScreenHandler = furnaceScreen.getScreenHandler();

                        handleFurnaceMenu(client, primBaritone, furnaceScreenHandler);
                    }
                    else {
                        // Get flint and steel
                        if (!ProgressChecks.hasItem(Items.FLINT_AND_STEEL, 1, client.player.currentScreenHandler)) {
                            doGetFNS(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
                        }
                        else if (!ProgressChecks.hasItem(Items.OBSIDIAN, 10, client.player.currentScreenHandler) && !client.world.getDimension().toString().contains("nether")) {
                            // Light nether portal and go through it
                            if ((standingInPortalShape(client) || portalStartPos != null) && ProgressChecks.hasItem(Items.FLINT_AND_STEEL, 1, client.player.currentScreenHandler)) {
                                handlePortalNavAndLight(client, primBaritone, blockHitResult);
                            }
                            else {
                                if (!ProgressChecks.hasItem(Items.DIAMOND_PICKAXE, 1, client.player.currentScreenHandler)) {
                                    doGetDiamondPickaxe(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
                                }
                                else if (!ProgressChecks.hasItem(Items.DIAMOND_SHOVEL, 1, client.player.currentScreenHandler)) {
                                    doGetDiamondShovel(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
                                }
                                else {
                                    if (!mineObsidian(client, primBaritone)) {
                                        if (!ProgressChecks.hasItem(Items.WATER_BUCKET, 1, client.player.currentScreenHandler)) {
                                            if (client.world.getTime() > waterPlaceTime + 48 || client.world.getTime() < waterPlaceTime) {
                                                doGetWaterBucket(client, primBaritone, hitCraftingTable, hitFurnace, blockHitResult);
                                                waterPlaceTime = 0;
                                            }
                                        }
                                        else {
                                            if (client.world.getTime() > waterPickupTime + 60 || client.world.getTime() < waterPickupTime) {
                                                doLavaToObsidian(client, primBaritone);
                                                waterPickupTime = 0;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            if (!client.world.getDimension().toString().contains("nether")) {
                                doBuildPortal(client, primBaritone);
                            }
                        }
                    }
                }
            } else {
                if (doneWithTask(client)) {
                    doChatLogMessage(client, "Done with baritone task.");
                    BaritoneUtil.cancelAllGoals(primBaritone);
                    rootItemStack = new ItemStack(Items.AIR);
                }
                else if (standingInOverworldPortal(client)) {
                    BaritoneUtil.cancelAllGoals(primBaritone);
                    rootItemStack = new ItemStack(Items.AIR);
                }
            }
        }
    }

    public static void doChatLogMessage(MinecraftClient client, String message) {
        client.player.sendMessage(AutoMinecraftClient.chatPrefix.copy().append(Text.literal(message).withColor(Formatting.RED.getColorValue())));
    }
}
