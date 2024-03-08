package net.plaaasma.autominecraft;

import baritone.api.IBaritone;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    public static void cancelAllGoals(IBaritone baritone) {
//        baritone.getPathingBehavior().cancelEverything();
        baritone.getCommandManager().execute("stop");
    }

    public static boolean inProcess(IBaritone baritone) {
        return baritone.getBuilderProcess().isActive() || baritone.getMineProcess().isActive() || baritone.getCustomGoalProcess().isActive();
    }

    public static boolean craftItemSmall(Item item, MinecraftClient client) {
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

                        return true;
                    }
                    break;
                }
            }
        }
        else {
            System.out.println("Recipe manager null");
        }
        return false;
    }

    public static boolean craftItemLarge(Item item, MinecraftClient client) {
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

                        return true;
                    }
                    break;
                }
            }
        }
        else {
            System.out.println("Recipe manager null");
        }
        return false;
    }
}
