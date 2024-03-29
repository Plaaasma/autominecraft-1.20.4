package net.plaaasma.autominecraft;

import baritone.api.IBaritone;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.RecipeBookDataC2SPacket;
import net.minecraft.network.packet.s2c.play.UnlockRecipesS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BaritoneUtil {
    private static HashMap<Item, Integer> itemWhitelist = new HashMap<>() {{
        // MATERIALS
        put(Items.COBBLESTONE, 128);
        put(Items.IRON_INGOT, 64);
        put(Items.RAW_IRON, 64);
        put(Items.FLINT, 64);
        put(Items.DIAMOND, 64);
        put(Items.OAK_LOG, 64);
        put(Items.OAK_WOOD, 64);
        put(Items.OAK_PLANKS, 64);
        put(Items.STICK, 64);
        put(Items.OBSIDIAN, 64);
        put(Items.CRAFTING_TABLE, 64);
        put(Items.FURNACE, 64);
        put(Items.COAL, 64);

        // TOOLS
        put(Items.WOODEN_PICKAXE, 1);
        put(Items.STONE_PICKAXE, 1);
        put(Items.STONE_AXE, 1);
        put(Items.IRON_PICKAXE, 1);
        put(Items.IRON_AXE, 1);
        put(Items.DIAMOND_PICKAXE, 1);
        put(Items.DIAMOND_SHOVEL, 1);
        put(Items.FLINT_AND_STEEL, 1);
        put(Items.BUCKET, 1);
        put(Items.WATER_BUCKET, 1);
        put(Items.LAVA_BUCKET, 1);
    }};

    public static void cancelAllGoals(IBaritone baritone) {
//        baritone.getPathingBehavior().cancelEverything();
        baritone.getCommandManager().execute("stop");
    }

    public static boolean inProcess(IBaritone baritone) {
        return baritone.getBuilderProcess().isActive() || baritone.getMineProcess().isActive() || baritone.getCustomGoalProcess().isActive();
    }

    public static void craftItemSmall(Item item, MinecraftClient client) {
        AutomationUtil.doChatLogMessage(client, "Crafting " + item.getName().getString());
        ClientPlayerEntity player = client.player;
        RecipeManager recipeManager = client.world.getRecipeManager();

        if (recipeManager != null) {
            List<RecipeEntry<CraftingRecipe>> craftingRecipes = recipeManager.listAllOfType(RecipeType.CRAFTING);

            for (RecipeEntry<CraftingRecipe> recipeEntry: craftingRecipes) {
                if (recipeEntry.toString().substring(recipeEntry.toString().indexOf(":") + 1).equals(item.toString())) {
                    List<Ingredient> ingredients = recipeEntry.value().getIngredients();
                    HashMap<Item, Integer> combinedIngredientsMap = new HashMap<>();
                    for (Ingredient ingredient : ingredients) {
                        List<ItemStack> ingredientStacks = Arrays.asList(ingredient.getMatchingStacks());
                        for (ItemStack ingredientStack : ingredientStacks) {
                            if (ProgressChecks.hasItem(ingredientStack.getItem(), ingredientStack.getCount(), player.currentScreenHandler)) {
                                if (combinedIngredientsMap.containsKey(ingredientStack.getItem())) {
                                    combinedIngredientsMap.put(ingredientStack.getItem(), combinedIngredientsMap.get(ingredientStack.getItem()) + ingredientStack.getCount());
                                }
                                else {
                                    combinedIngredientsMap.put(ingredientStack.getItem(), ingredientStack.getCount());
                                }
                                break;
                            }
                        }
                    }
                    int neededIngredientAmount = combinedIngredientsMap.size();
                    int ingredientAmount = 0;
                    for (Item ingredientItem : combinedIngredientsMap.keySet()) {
                        ItemStack ingredientStack = new ItemStack(ingredientItem, combinedIngredientsMap.get(ingredientItem));
                        if (ProgressChecks.hasItem(ingredientStack.getItem(), ingredientStack.getCount(), player.currentScreenHandler)) {
                            ingredientAmount += 1;
                        }
                    }

                    if (ingredientAmount == neededIngredientAmount) {
                        if (!(client.currentScreen instanceof InventoryScreen)) {
                            client.setScreen(new InventoryScreen(player));
                        }
                        client.interactionManager.clickRecipe(client.player.currentScreenHandler.syncId, recipeEntry, false);
                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, player);
                        client.setScreen(null);

                        return;
                    }
                    break;
                }
            }
        }
        else {
            System.out.println("Recipe manager null");
        }
    }

    public static void craftItemLarge(Item item, MinecraftClient client) {
        AutomationUtil.doChatLogMessage(client, "Crafting " + item.getName().getString());
        ClientPlayerEntity player = client.player;
        RecipeManager recipeManager = client.world.getRecipeManager();

        if (recipeManager != null) {
            List<RecipeEntry<CraftingRecipe>> craftingRecipes = recipeManager.listAllOfType(RecipeType.CRAFTING);

            for (RecipeEntry<CraftingRecipe> recipeEntry: craftingRecipes) {
                if (recipeEntry.toString().substring(recipeEntry.toString().indexOf(":") + 1).equals(item.toString())) {
                    List<Ingredient> ingredients = recipeEntry.value().getIngredients();
                    HashMap<Item, Integer> combinedIngredientsMap = new HashMap<>();
                    for (Ingredient ingredient : ingredients) {
                        List<ItemStack> ingredientStacks = Arrays.asList(ingredient.getMatchingStacks());
                        for (ItemStack ingredientStack : ingredientStacks) {
                            if (ProgressChecks.hasItem(ingredientStack.getItem(), ingredientStack.getCount(), player.currentScreenHandler)) {
                                if (combinedIngredientsMap.containsKey(ingredientStack.getItem())) {
                                    combinedIngredientsMap.put(ingredientStack.getItem(), combinedIngredientsMap.get(ingredientStack.getItem()) + ingredientStack.getCount());
                                }
                                else {
                                    combinedIngredientsMap.put(ingredientStack.getItem(), ingredientStack.getCount());
                                }
                                break;
                            }
                        }
                    }
                    int neededIngredientAmount = combinedIngredientsMap.size();
                    int ingredientAmount = 0;
                    for (Item ingredientItem : combinedIngredientsMap.keySet()) {
                        ItemStack ingredientStack = new ItemStack(ingredientItem, combinedIngredientsMap.get(ingredientItem));
                        if (ProgressChecks.hasItem(ingredientStack.getItem(), ingredientStack.getCount(), player.currentScreenHandler)) {
                            ingredientAmount += 1;
                        }
                    }

                    if (ingredientAmount == neededIngredientAmount) {
                        client.interactionManager.clickRecipe(client.player.currentScreenHandler.syncId, recipeEntry, false);
                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, player);
                        client.setScreen(null);

                        return;
                    }
                    break;
                }
            }
        }
        else {
            System.out.println("Recipe manager null");
        }
    }

    public static void yeetItems(MinecraftClient client) {
        if (client.currentScreen == null) {
            InventoryScreen inventoryScreen = new InventoryScreen(client.player);
            if (!(client.currentScreen instanceof InventoryScreen)) {
                client.setScreen(inventoryScreen);
            }

            ScreenHandler inventoryScreenHandler = inventoryScreen.getScreenHandler();

            List<Item> seenItems = new ArrayList<>();
            for (Slot slot : inventoryScreenHandler.slots) {
                ItemStack slotStack = slot.getStack();
                Item slotItem = slotStack.getItem();
                if (slotItem != Items.AIR) {
                    if (!itemWhitelist.containsKey(slotItem)) {
                        client.interactionManager.clickSlot(inventoryScreenHandler.syncId, slot.id, 0, SlotActionType.THROW, client.player);
                    } else {
                        if (seenItems.contains(slotItem)) {
                            client.interactionManager.clickSlot(inventoryScreenHandler.syncId, slot.id, 0, SlotActionType.THROW, client.player);
                        }
                    }
                    if (slotStack.getCount() >= 64) {
                        seenItems.add(slot.getStack().getItem());
                    }
                }
            }
            client.setScreen(null);
        }
    }

    public static void equipItem(Item item, MinecraftClient client, ScreenHandler inventoryHandler) {
        for (Slot slot : inventoryHandler.slots) {
            ItemStack slotStack = slot.getStack();
            Item slotItem = slotStack.getItem();
            if (slotItem == item) {
                client.interactionManager.clickSlot(inventoryHandler.syncId, slot.id, 0, SlotActionType.SWAP, client.player);
                client.player.getInventory().selectedSlot = 0;
                return;
            }
        }
    }

    public static boolean blockInPortalRegion(MinecraftClient client) {
        ClientWorld clientWorld = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        for (int x = -1; x < 1; x++) {
            for (int y = 0; y < 3; y++) {
                Block currentBlock = clientWorld.getBlockState(new BlockPos(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ())).getBlock();
                if (currentBlock != Blocks.AIR && currentBlock != Blocks.NETHER_PORTAL && currentBlock != Blocks.FIRE) {
                    return true;
                }
            }
        }

        return false;
    }
}
