package com.gearsealerpro.block;

import com.gearsealerpro.menu.GearSealerProMenu;
import com.gearsealerpro.registry.ModBlockEntities;
import iskallia.vault.container.oversized.OverSizedInventory;
import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.gear.item.VaultGearItem;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Inventory of the Gear Sealer Pro.
 *
 * <p>Slot 0 = Vault Gold, 1 = Vorpal Focus, 2 = Painite, plus a single gear input
 * and a single output slot. This mirrors {@code VaultSealerTileEntity} so that the
 * sealing cost and the accepted materials are exactly the same.</p>
 */
public class GearSealerProBlockEntity extends BlockEntity implements MenuProvider {

    private static final String KEY_GEAR_INPUT = "gearInput";
    private static final String KEY_OUTPUT = "output";

    private final OverSizedInventory inventory =
            new OverSizedInventory.FilteredInsert(4, this, this::canInsertInput);

    private final SimpleContainer gearInput = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            GearSealerProBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return isValidGear(stack);
        }
    };

    private final SimpleContainer output = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            GearSealerProBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return false;
        }
    };

    public GearSealerProBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAULT_SEALER_PRO.get(), pos, state);
    }

    /** Same acceptance rule the vanilla sealer's gear slot uses. */
    public static boolean isValidGear(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof VaultGearItem) || !VaultGearData.hasData(stack)) {
            return false;
        }
        VaultGearData data = VaultGearData.read(stack);
        return data.getState() == VaultGearState.IDENTIFIED
                && !data.isSealed()
                && !data.isCorrupted()
                && stack.getItem() != ModItems.JEWEL;
    }

    public boolean canInsertInput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case 0 -> stack.is(ModBlocks.VAULT_GOLD);
            case 1 -> stack.is(ModItems.VORPAL_FOCUS);
            case 2 -> stack.is(ModItems.PAINITE_GEM);
            default -> false;
        };
    }

    public OverSizedInventory getInventory() {
        return this.inventory;
    }

    public SimpleContainer getGearInput() {
        return this.gearInput;
    }

    public SimpleContainer getOutput() {
        return this.output;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            }
        }
        for (int i = 0; i < this.gearInput.getContainerSize(); i++) {
            ItemStack stack = this.gearInput.getItem(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            }
        }
        for (int i = 0; i < this.output.getContainerSize(); i++) {
            ItemStack stack = this.output.getItem(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.inventory.load(tag);
        this.gearInput.setItem(0, ItemStack.of(tag.getCompound(KEY_GEAR_INPUT)));
        this.output.setItem(0, ItemStack.of(tag.getCompound(KEY_OUTPUT)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.inventory.save(tag);
        tag.put(KEY_GEAR_INPUT, this.gearInput.getItem(0).save(new CompoundTag()));
        tag.put(KEY_OUTPUT, this.output.getItem(0).save(new CompoundTag()));
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GearSealerProMenu(containerId, player.level, this.worldPosition, playerInventory);
    }
}
