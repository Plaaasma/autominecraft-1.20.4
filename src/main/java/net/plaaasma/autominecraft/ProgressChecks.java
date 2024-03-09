package net.plaaasma.autominecraft;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ProgressChecks {
    public static boolean hasItem(Item item, Integer amount, ScreenHandler inventoryHandler) {
        for (Slot slot : inventoryHandler.slots) {
            ItemStack slotStack = slot.getStack();
            Item slotItem = slotStack.getItem();
            if (slotItem == item) {
                if (slotStack.getCount() >= amount) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int getItemSlot(Item item, Integer amount, PlayerInventory inventory) {
        int slotNum = inventory.getSlotWithStack(new ItemStack(item));
        if (slotNum > -1) {
            return slotNum;
        }
        else {
            return -1;
        }
    }

    public static boolean hasSmeltableItem(ScreenHandler inventoryHandler) {
        for (Slot slot : inventoryHandler.slots) {
            ItemStack slotStack = slot.getStack();
            if (slotStack.isOf(Items.RAW_IRON)) {
                return true;
            }
        }

        return false;
    }

    public static int getSmeltableID(ScreenHandler inventoryHandler) {
        for (Slot slot : inventoryHandler.slots) {
            ItemStack slotStack = slot.getStack();
            if (slotStack.isOf(Items.RAW_IRON)) {
                return slot.id;
            }
        }

        return -1;
    }

    public static boolean hasFuelItem(ScreenHandler inventoryHandler) {
        for (Slot slot : inventoryHandler.slots) {
            ItemStack slotStack = slot.getStack();
            if (slotStack.isOf(Items.COAL)) {
                return true;
            }
        }

        return false;
    }

    public static int getFuelID(ScreenHandler inventoryHandler) {
        for (Slot slot : inventoryHandler.slots) {
            ItemStack slotStack = slot.getStack();
            if (slotStack.isOf(Items.COAL)) {
                return slot.id;
            }
        }

        return -1;
    }
}
