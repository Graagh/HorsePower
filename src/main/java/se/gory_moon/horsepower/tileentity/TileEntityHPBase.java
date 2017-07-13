package se.gory_moon.horsepower.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import se.gory_moon.horsepower.recipes.HPRecipeBase;

import javax.annotation.Nullable;

public abstract class TileEntityHPBase extends TileEntity {

    protected NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);

    protected IHPInventory inventory;

    private EnumFacing forward = null;

    public TileEntityHPBase(int inventorySize) {
        itemStacks = NonNullList.withSize(inventorySize, ItemStack.EMPTY);

        inventory = new IHPInventory() {
            @Override
            public int getSizeInventory() {
                return itemStacks.size();
            }

            @Override
            public boolean isEmpty() {
                for (ItemStack itemstack : itemStacks) {
                    if (!itemstack.isEmpty()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public ItemStack getStackInSlot(int index) {
                return itemStacks.get(index);
            }

            @Override
            public ItemStack decrStackSize(int index, int count) {
                ItemStack stack = ItemStackHelper.getAndSplit(itemStacks, index, count);
                if (!stack.isEmpty())
                    markDirty();
                return stack;
            }

            @Override
            public ItemStack removeStackFromSlot(int index) {
                ItemStack stack = ItemStackHelper.getAndRemove(itemStacks, index);
                return stack;
            }

            @Override
            public void setInventorySlotContents(int index, ItemStack stack) {
                TileEntityHPBase.this.setInventorySlotContents(index, stack);
            }

            @Override
            public void setSlotContent(int index, ItemStack stack) {
                itemStacks.set(index, stack);

                if (index == 0 && stack.getCount() > this.getInventoryStackLimit()) {
                    stack.setCount(this.getInventoryStackLimit());
                }
            }

            @Override
            public int getInventoryStackLimit() {
                return TileEntityHPBase.this.getInventoryStackLimit();
            }

            @Override
            public void markDirty() {
                TileEntityHPBase.this.markDirty();
            }

            @Override
            public boolean isUsableByPlayer(EntityPlayer player) {
                return getWorld().getTileEntity(getPos()) == TileEntityHPBase.this && player.getDistanceSq((double) getPos().getX() + 0.5D, (double) getPos().getY() + 0.5D, (double) getPos().getZ() + 0.5D) <= 64.0D;
            }

            @Override
            public void openInventory(EntityPlayer player) {}

            @Override
            public void closeInventory(EntityPlayer player) {}

            @Override
            public boolean isItemValidForSlot(int index, ItemStack stack) {
                return TileEntityHPBase.this.isItemValidForSlot(index, stack);
            }

            @Override
            public int getField(int id) {
                return TileEntityHPBase.this.getField(id);
            }

            @Override
            public void setField(int id, int value) {TileEntityHPBase.this.setField(id, value);}

            @Override
            public int getFieldCount() {
                return TileEntityHPBase.this.getFieldCount();
            }

            @Override
            public void clear() {
                itemStacks.clear();
            }

            @Override
            public String getName() {
                return TileEntityHPBase.this.getName();
            }

            @Override
            public boolean hasCustomName() {
                return false;
            }

            @Override
            public ITextComponent getDisplayName() {
                return TileEntityHPBase.this.getDisplayName();
            }
        };
        handlerTop = new RangedWrapper(new InvWrapper(inventory), 0, 1);
        handlerBottom = new RangedWrapper(new InvWrapper(inventory), 1, getOutputSlot() + 1);
    }

    public abstract HPRecipeBase getRecipe();

    public abstract ItemStack getRecipeItemStack();

    public abstract int getInventoryStackLimit();

    public abstract boolean isItemValidForSlot(int index, ItemStack stack);

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {}

    public int getFieldCount() {
        return 0;
    }

    public abstract String getName();

    public abstract int getOutputSlot();

    public ItemStack getStackInSlot(int index) {
        return inventory.getStackInSlot(index);
    }

    public ItemStack removeStackFromSlot(int index) {
        return inventory.removeStackFromSlot(index);
    }

    public IHPInventory getInventory() {
        return inventory;
    }

    public void setInventorySlotContents(int index, ItemStack stack) {
        inventory.setSlotContent(index, stack);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        itemStacks = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, itemStacks);

        if (canBeRotated()) {
            forward = EnumFacing.byName(compound.getString("forward"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, itemStacks);

        if (canBeRotated()) {
            compound.setString("forward", getForward().getName());
        }
        return compound;
    }

    @Override
    public void markDirty() {
        if (!getWorld().isRemote) {
            final IBlockState state = getWorld().getBlockState(getPos());
            getWorld().notifyBlockUpdate(getPos(), state, state, 8);
            super.markDirty();
        }
    }

    public boolean canWork() {
        if (inventory.getStackInSlot(0).isEmpty()) {
            return false;
        } else {
            HPRecipeBase recipeBase = getRecipe();
            if (recipeBase == null) return false;

            ItemStack itemstack = recipeBase.getOutput();
            ItemStack secondary = recipeBase.getSecondary();

            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack output = inventory.getStackInSlot(1);
                ItemStack outputSecondary = inventory.getStackInSlot(2);
                if (!secondary.isEmpty() && !outputSecondary.isEmpty()) {
                    if (!outputSecondary.isItemEqual(secondary)) return false;
                    if (outputSecondary.getCount() + secondary.getCount() > secondary.getMaxStackSize()) return false;
                }
                return output.isEmpty() || output.isItemEqual(itemstack) && output.getCount() + itemstack.getCount() <= output.getMaxStackSize();
            }
        }
    }

    public static boolean canCombine(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && (stack1.getMetadata() == stack2.getMetadata() && (stack1.getCount() <= stack1.getMaxStackSize() && ItemStack.areItemStackTagsEqual(stack1, stack2)));
    }

    public boolean canBeRotated() {
        return false;
    }

    public EnumFacing getForward() {
        if (forward == null)
            return EnumFacing.NORTH;
        return forward;
    }

    public void setForward(EnumFacing forward) {
        this.forward = forward;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), -999, getUpdateTag());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
        markDirty();
    }

    private IItemHandler handlerTop = null;
    private IItemHandler handlerBottom = null;

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return (facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && (facing == EnumFacing.DOWN || facing == EnumFacing.UP)) || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            if (facing == EnumFacing.DOWN)
                return (T) handlerBottom;
            else if (facing == EnumFacing.UP)
                return (T) handlerTop;
        return super.getCapability(capability, facing);
    }
}
