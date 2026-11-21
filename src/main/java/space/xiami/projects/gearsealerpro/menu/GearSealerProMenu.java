package space.xiami.projects.gearsealerpro.menu;

import space.xiami.projects.gearsealerpro.block.GearSealerProBlockEntity;
import space.xiami.projects.gearsealerpro.logic.SealerLogic;
import space.xiami.projects.gearsealerpro.network.NetworkHandler;
import space.xiami.projects.gearsealerpro.registry.ModMenus;
import iskallia.vault.container.oversized.OverSizedSlotContainer;
import iskallia.vault.container.oversized.OverSizedTabSlot;
import iskallia.vault.container.slot.RecipeOutputSlot;
import iskallia.vault.container.slot.TabSlot;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Container of the Gear Sealer Pro. Slot layout and filters mirror the vanilla sealer:
 * gold / vorpal focus / painite inputs, one gear input, one output.
 */
public class GearSealerProMenu extends OverSizedSlotContainer {

    private static final int PLAYER_SLOT_COUNT = 36;

    private final BlockPos tilePos;
    @Nullable
    private final GearSealerProBlockEntity tileEntity;

    private Slot goldSlot;
    private Slot focusSlot;
    private Slot painiteSlot;
    private Slot gearInputSlot;
    private Slot outputSlot;

    public GearSealerProMenu(int containerId, Level level, BlockPos pos, Inventory playerInventory) {
        super(ModMenus.VAULT_SEALER_PRO.get(), containerId, playerInventory.player);
        this.tilePos = pos;
        this.tileEntity = level.getBlockEntity(pos) instanceof GearSealerProBlockEntity sealer ? sealer : null;
        this.initSlots(playerInventory);
    }

    private void initSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new TabSlot(playerInventory, column + row * 9 + 9, 8 + column * 18, 206 + row * 18));
            }
        }
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new TabSlot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 264));
        }

        if (this.tileEntity == null) {
            // Keep slot indices stable even if the block entity vanished client-side.
            this.goldSlot = this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, 13, 26));
            this.focusSlot = this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, 31, 35));
            this.painiteSlot = this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, 13, 44));
            this.gearInputSlot = this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, 82, 35));
            this.outputSlot = this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, 142, 35));
            return;
        }

        Container invContainer = this.tileEntity.getInventory();
        this.goldSlot = this.addSlot(new OverSizedTabSlot(invContainer, 0, 13, 26)
                .setFilter(stack -> stack.is(ModBlocks.VAULT_GOLD)));
        this.focusSlot = this.addSlot(new OverSizedTabSlot(invContainer, 1, 31, 35)
                .setFilter(stack -> stack.is(ModItems.VORPAL_FOCUS)));
        this.painiteSlot = this.addSlot(new OverSizedTabSlot(invContainer, 2, 13, 44)
                .setFilter(stack -> stack.is(ModItems.PAINITE_GEM)));

        Container inputContainer = this.tileEntity.getGearInput();
        this.gearInputSlot = this.addSlot(new TabSlot(inputContainer, 0, 82, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GearSealerProBlockEntity.isValidGear(stack);
            }
        });
        this.outputSlot = this.addSlot(new RecipeOutputSlot(this.tileEntity.getOutput(), 0, 142, 35));
    }

    public Slot getGoldSlot() {
        return this.goldSlot;
    }

    public Slot getFocusSlot() {
        return this.focusSlot;
    }

    public Slot getPainiteSlot() {
        return this.painiteSlot;
    }

    public Slot getGearInputSlot() {
        return this.gearInputSlot;
    }

    public Slot getOutputSlot() {
        return this.outputSlot;
    }

    public BlockPos getTilePos() {
        return this.tilePos;
    }

    @Nullable
    public GearSealerProBlockEntity getTileEntity() {
        return this.tileEntity;
    }

    /** Recomputes the selectable options on the server and pushes them to the client. */
    public void sendOptions(ServerPlayer player) {
        NetworkHandler.sendTo(player, SealerLogic.buildSync(this));
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(player.level, this.tilePos)
                .evaluate((level, pos) -> level.getBlockEntity(pos) == this.tileEntity
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                        true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();
        int machineStart = PLAYER_SLOT_COUNT;
        int machineEnd = this.slots.size();
        if (index >= machineStart && index < machineEnd) {
            if (!this.moveItemStackTo(slotStack, 0, PLAYER_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(slotStack, machineStart, machineEnd - 1, false)) {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (slotStack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, slotStack);
        return result;
    }
}
