package net.plaaasma.autominecraft;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
}
