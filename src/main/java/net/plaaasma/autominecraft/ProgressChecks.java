package net.plaaasma.autominecraft;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ProgressChecks {
    public static boolean hasItem(Item item, Integer amount, PlayerInventory inventory) {
        int slotNum = inventory.getSlotWithStack(new ItemStack(item));
        if (slotNum > -1) {
            return inventory.getStack(slotNum).getCount() >= amount;
        }
        else {
            return false;
        }
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
