package com.github.technus.tectech.thing.item.renderElemental;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import static com.github.technus.tectech.util.TT_Utility.getSomeString;

public interface IElementalItem {
    default String getSymbol(ItemStack stack,int index){
        try {
            NBTTagCompound tNBT = stack.getTagCompound();
            if (tNBT != null && tNBT.hasKey("symbols")) {
                return getSomeString(tNBT.getCompoundTag("symbols"),index);
            } else {
                return null;
            }
        } catch (Exception e) {
            return "#!";
        }
    }
}
