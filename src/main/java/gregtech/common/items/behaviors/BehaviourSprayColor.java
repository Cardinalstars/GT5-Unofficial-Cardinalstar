package gregtech.common.items.behaviors;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.implementations.tiles.IColorableTile;
import appeng.api.util.AEColor;
import appeng.block.networking.BlockCableBus;
import gregtech.api.enums.Dyes;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.MetaBaseItem;
import gregtech.api.util.GTLanguageManager;
import gregtech.api.util.GTUtility;
import gregtech.common.config.Other;

public class BehaviourSprayColor extends BehaviourNone {

    private final ItemStack mEmpty;
    private final ItemStack mUsed;
    private final ItemStack mFull;
    private final long mUses;
    private final byte mColor;
    private final Collection<Block> mAllowedVanillaBlocks = Arrays.asList(
        Blocks.glass,
        Blocks.glass_pane,
        Blocks.stained_glass,
        Blocks.stained_glass_pane,
        Blocks.carpet,
        Blocks.hardened_clay);
    protected String mTooltip;
    private final String mTooltipUses = GTLanguageManager
        .addStringLocalization("gt.behaviour.paintspray.uses", "Remaining Uses:");
    private final String mTooltipUnstackable = GTLanguageManager
        .addStringLocalization("gt.behaviour.unstackable", "Not usable when stacked!");
    protected final String mTooltipChain = GTLanguageManager.addStringLocalization(
        "gt.behaviour.paintspray.chain",
        "If used while sneaking it will spray a chain of blocks");

    protected final String mTooltipChainAmount = GTLanguageManager.addStringLocalization(
        "gt.behaviour.paintspray.chain_amount",
        "Sprays up to %d blocks, in the direction you're looking at");

    public BehaviourSprayColor(ItemStack aEmpty, ItemStack aUsed, ItemStack aFull, long aUses, int aColor) {
        this.mEmpty = aEmpty;
        this.mUsed = aUsed;
        this.mFull = aFull;
        this.mUses = aUses;
        this.mColor = ((byte) aColor);
        this.mTooltip = GTLanguageManager.addStringLocalization(
            "gt.behaviour.paintspray." + this.mColor + ".tooltip",
            "Can Color things in " + Dyes.get(this.mColor).mName);
    }

    public BehaviourSprayColor(ItemStack aEmpty, ItemStack aUsed, ItemStack aFull, long aUses) {
        this.mEmpty = aEmpty;
        this.mUsed = aUsed;
        this.mFull = aFull;
        this.mUses = aUses;
        this.mColor = 0;
        mTooltip = "";
    }

    @Override
    public boolean onItemUseFirst(MetaBaseItem aItem, ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX,
        int aY, int aZ, ForgeDirection side, float hitX, float hitY, float hitZ) {
        if ((aWorld.isRemote) || (aStack.stackSize != 1)) {
            return false;
        }
        boolean rOutput = false;
        if (!aPlayer.canPlayerEdit(aX, aY, aZ, side.ordinal(), aStack)) {
            return false;
        }
        NBTTagCompound tNBT = aStack.getTagCompound();
        if (tNBT == null) {
            tNBT = new NBTTagCompound();
        }
        long tUses = getUses(aStack, tNBT);

        int painted = 0;
        int maxPainted = Other.sprayCanChainRange;
        ForgeDirection lookSide;
        Vec3 look = aPlayer.getLookVec();
        double absX = Math.abs(look.xCoord);
        double absY = Math.abs(look.yCoord);
        double absZ = Math.abs(look.zCoord);
        if (absX > absY && absX > absZ) {
            lookSide = look.xCoord > 0 ? ForgeDirection.EAST : ForgeDirection.WEST;
        } else if (absY > absX && absY > absZ) {
            lookSide = look.yCoord > 0 ? ForgeDirection.UP : ForgeDirection.DOWN;
        } else {
            lookSide = look.zCoord > 0 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
        }
        Block initialBlock = aWorld.getBlock(aX, aY, aZ);
        int initialBlockMeta = aWorld.getBlockMetadata(aX, aY, aZ);
        TileEntity initialTE = aWorld.getTileEntity(aX, aY, aZ);
        while ((GTUtility.areStacksEqual(aStack, this.mUsed, true)) && (colorize(aWorld, aX, aY, aZ, side, aPlayer))) {
            GTUtility.sendSoundToPlayers(aWorld, SoundResource.IC2_TOOLS_PAINTER, 1.0F, 1.0F, aX, aY, aZ);
            if (!aPlayer.capabilities.isCreativeMode) {
                tUses -= 1L;
            }
            rOutput = true;
            painted++;
            if (painted >= maxPainted && maxPainted != -1) break;
            if (!aPlayer.isSneaking() || tUses <= 0) break;
            switch (lookSide) {
                case UP -> aY += 1;
                case DOWN -> aY -= 1;
                case NORTH -> aZ -= 1;
                case SOUTH -> aZ += 1;
                case WEST -> aX -= 1;
                case EAST -> aX += 1;
                default -> throw new IllegalArgumentException("Unexpected value: " + lookSide);
            }

            if (aWorld.getBlock(aX, aY, aZ) != initialBlock) break;
            if (aWorld.getBlockMetadata(aX, aY, aZ) != initialBlockMeta) break;

            /*
             * Check if the initial block had a TE and if the next one does, check if its the same kind.
             * else one does and the other doesnt, thus stop checking.
             */
            TileEntity targetTE = aWorld.getTileEntity(aX, aY, aZ);
            if (initialTE == null ^ targetTE == null) break;
            if (initialTE != null && targetTE != null) {
                if (!initialTE.getClass()
                    .isInstance(targetTE)) break;

                if (initialTE instanceof IGregTechTileEntity currentGTTile
                    && targetTE instanceof IGregTechTileEntity targetGTTile) {
                    if (currentGTTile.getMetaTileID() != targetGTTile.getMetaTileID()) break;
                }
            }
        }
        setRemainingUses(aStack, tNBT, tUses);
        return rOutput;
    }

    protected long getUses(ItemStack aStack, NBTTagCompound tNBT) {
        long tUses = tNBT.getLong("GT.RemainingPaint");
        if (GTUtility.areStacksEqual(aStack, this.mFull, true)) {
            aStack.func_150996_a(this.mUsed.getItem());
            Items.feather.setDamage(aStack, Items.feather.getDamage(this.mUsed));
            tUses = this.mUses;
        }
        return tUses;
    }

    protected void setRemainingUses(ItemStack aStack, NBTTagCompound tNBT, long tUses) {
        tNBT.removeTag("GT.RemainingPaint");
        if (tUses > 0L) {
            tNBT.setLong("GT.RemainingPaint", tUses);
        }
        if (tNBT.hasNoTags()) {
            aStack.setTagCompound(null);
        } else {
            aStack.setTagCompound(tNBT);
        }
        if (tUses <= 0L) {
            if (this.mEmpty == null) {
                aStack.stackSize -= 1;
            } else {
                aStack.func_150996_a(this.mEmpty.getItem());
                Items.feather.setDamage(aStack, Items.feather.getDamage(this.mEmpty));
            }
        }
    }

    protected boolean colorize(World aWorld, int aX, int aY, int aZ, ForgeDirection side, EntityPlayer player) {
        final Block aBlock = aWorld.getBlock(aX, aY, aZ);
        if (aBlock != Blocks.air) {
            if (this.mAllowedVanillaBlocks.contains(aBlock) || aBlock instanceof BlockColored) {
                if (aBlock == Blocks.hardened_clay) {
                    aWorld.setBlock(aX, aY, aZ, Blocks.stained_hardened_clay, (~getColor()) & 0xF, 3);
                    return true;
                }
                if (aBlock == Blocks.glass_pane) {
                    aWorld.setBlock(aX, aY, aZ, Blocks.stained_glass_pane, (~getColor()) & 0xF, 3);
                    return true;
                }
                if (aBlock == Blocks.glass) {
                    aWorld.setBlock(aX, aY, aZ, Blocks.stained_glass, (~getColor()) & 0xF, 3);
                    return true;
                }
                if (aWorld.getBlockMetadata(aX, aY, aZ) == ((~getColor()) & 0xF)) {
                    return false;
                }
                aWorld.setBlockMetadataWithNotify(aX, aY, aZ, (~getColor()) & 0xF, 3);
                return true;
            }

            if (aBlock instanceof IColorableTile) {
                return ((IColorableTile) aBlock).recolourBlock(side, AEColor.values()[(~getColor()) & 0xF], player);
            }

            if (aBlock instanceof BlockCableBus) {
                return ((BlockCableBus) aBlock).recolourBlock(aWorld, aX, aY, aZ, side, (~getColor()) & 0xF, player);
            }
        }
        return aBlock.recolourBlock(aWorld, aX, aY, aZ, side, (~getColor()) & 0xF);
    }

    protected byte getColor() {
        return this.mColor;
    }

    @Override
    public List<String> getAdditionalToolTips(MetaBaseItem aItem, List<String> aList, ItemStack aStack) {
        aList.add(this.mTooltip);
        aList.add(this.mTooltipChain);
        aList.add(String.format(this.mTooltipChainAmount, Other.sprayCanChainRange));
        NBTTagCompound tNBT = aStack.getTagCompound();
        long tRemainingPaint = tNBT == null ? this.mUses
            : GTUtility.areStacksEqual(aStack, this.mFull, true) ? this.mUses : tNBT.getLong("GT.RemainingPaint");
        aList.add(this.mTooltipUses + " " + tRemainingPaint);
        aList.add(this.mTooltipUnstackable);
        return aList;
    }
}
