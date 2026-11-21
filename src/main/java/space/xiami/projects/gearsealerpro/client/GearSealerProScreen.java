package space.xiami.projects.gearsealerpro.client;

import space.xiami.projects.gearsealerpro.menu.GearSealerProMenu;
import space.xiami.projects.gearsealerpro.network.MsgCommitSeal;
import space.xiami.projects.gearsealerpro.network.MsgRequestOptions;
import space.xiami.projects.gearsealerpro.network.MsgSyncOptions;
import space.xiami.projects.gearsealerpro.network.NetworkHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.gear.attribute.VaultGearModifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Gear Sealer Pro screen: pick a sealed modifier on the left, pick what it replaces on
 * the right (first row adds a brand new affix), then seal.
 */
public class GearSealerProScreen extends AbstractContainerScreen<GearSealerProMenu> {

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 286;

    private static final int LIST_Y = 96;
    private static final int LIST_ROW_HEIGHT = 18;
    private static final int LIST_ROWS = 4;
    private static final int LIST_HEIGHT = LIST_ROW_HEIGHT * LIST_ROWS;

    private static final int LEFT_LIST_X = 8;
    private static final int LEFT_LIST_W = 80;
    private static final int RIGHT_LIST_X = 92;
    private static final int RIGHT_LIST_W = 76;

    private static final int TOGGLE_Y = 66;
    private static final int TOGGLE_W = 40;
    private static final int TOGGLE_H = 14;

    private static final int SEAL_X = 104;
    private static final int SEAL_Y = 166;
    private static final int SEAL_W = 64;
    private static final int SEAL_H = 22;

    private int selectedTarget;            // 0 = add a new affix, otherwise (index + 1)
    private ResourceLocation selectedOptionId;
    private int selectedOptionType = -1;
    private int optionScroll;
    private int targetScroll;
    private VaultGearModifier.AffixType toggleType = VaultGearModifier.AffixType.PREFIX;

    private ItemStack lastGear = ItemStack.EMPTY;
    private int tickCounter;
    private Component hoveredOption;
    private Component hoveredTarget;

    public GearSealerProScreen(GearSealerProMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelY = 196;
    }

    @Override
    protected void init() {
        super.init();
        NetworkHandler.sendToServer(new MsgRequestOptions());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (++this.tickCounter % 10 != 0) {
            return;
        }
        ItemStack gear = this.menu.getGearInputSlot().getItem();
        if (!ItemStack.matches(gear, this.lastGear)) {
            this.lastGear = gear.copy();
            NetworkHandler.sendToServer(new MsgRequestOptions());
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        MsgSyncOptions data = ClientSealData.get();
        this.drawHeaders(pose);
        this.drawToggles(pose, mouseX, mouseY, data);
        this.drawLists(pose, mouseX, mouseY, data);
        this.drawCost(pose, data);
        this.drawSealButton(pose, mouseX, mouseY, data);
        if (this.hoveredOption != null) {
            this.renderComponentTooltip(pose, java.util.List.of(this.hoveredOption), mouseX, mouseY);
        } else if (this.hoveredTarget != null) {
            this.renderComponentTooltip(pose, java.util.List.of(this.hoveredTarget), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        fill(pose, this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, 0xFF1B1B21);
        fill(pose, this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + 2, 0xFF3C3C46);
        fill(pose, this.leftPos, this.topPos + PANEL_HEIGHT - 2, this.leftPos + PANEL_WIDTH,
                this.topPos + PANEL_HEIGHT, 0xFF3C3C46);
        fill(pose, this.leftPos, this.topPos, this.leftPos + 2, this.topPos + PANEL_HEIGHT, 0xFF3C3C46);
        fill(pose, this.leftPos + PANEL_WIDTH - 2, this.topPos, this.leftPos + PANEL_WIDTH,
                this.topPos + PANEL_HEIGHT, 0xFF3C3C46);

        for (Slot slot : this.menu.slots) {
            this.drawSlotFrame(pose, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
        }

        this.drawListFrame(pose, this.leftPos + LEFT_LIST_X, this.topPos + LIST_Y, LEFT_LIST_W, LIST_HEIGHT);
        this.drawListFrame(pose, this.leftPos + RIGHT_LIST_X, this.topPos + LIST_Y, RIGHT_LIST_W, LIST_HEIGHT);
    }

    private void drawSlotFrame(PoseStack pose, int x, int y) {
        fill(pose, x, y, x + 18, y + 18, 0xFF33333B);
        fill(pose, x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    private void drawListFrame(PoseStack pose, int x, int y, int width, int height) {
        fill(pose, x - 1, y - 1, x + width + 1, y + height + 1, 0xFF33333B);
        fill(pose, x, y, x + width, y + height, 0xFF101014);
    }

    private void drawHeaders(PoseStack pose) {
        this.font.draw(pose, new TranslatableComponent("gui.gearsealerpro.sealed_modifiers"),
                this.leftPos + LEFT_LIST_X, this.topPos + 84, 0xFFD8B45A);
        this.font.draw(pose, new TranslatableComponent("gui.gearsealerpro.targets"),
                this.leftPos + RIGHT_LIST_X, this.topPos + 84, 0xFFD8B45A);
    }

    private void drawToggles(PoseStack pose, int mouseX, int mouseY, MsgSyncOptions data) {
        boolean lockedToTarget = this.selectedTarget > 0;
        this.drawToggle(pose, mouseX, mouseY, LEFT_LIST_X, VaultGearModifier.AffixType.PREFIX, lockedToTarget, data);
        this.drawToggle(pose, mouseX, mouseY, LEFT_LIST_X + TOGGLE_W + 4, VaultGearModifier.AffixType.SUFFIX,
                lockedToTarget, data);
    }

    private void drawToggle(PoseStack pose, int mouseX, int mouseY, int x, VaultGearModifier.AffixType type,
                            boolean lockedToTarget, MsgSyncOptions data) {
        int screenX = this.leftPos + x;
        int screenY = this.topPos + TOGGLE_Y;
        boolean active = currentType(data) == type.ordinal();
        boolean hovered = mouseX >= screenX && mouseX < screenX + TOGGLE_W
                && mouseY >= screenY && mouseY < screenY + TOGGLE_H;
        int background = active ? 0xFF4A6B34 : (hovered && !lockedToTarget ? 0xFF3A3A44 : 0xFF2A2A31);
        fill(pose, screenX, screenY, screenX + TOGGLE_W, screenY + TOGGLE_H, background);
        Component label = new TranslatableComponent(type == VaultGearModifier.AffixType.PREFIX
                ? "gui.gearsealerpro.prefix" : "gui.gearsealerpro.suffix");
        this.font.draw(pose, label, screenX + (TOGGLE_W - this.font.width(label)) / 2F,
                screenY + 3, lockedToTarget && !active ? 0xFF6A6A6A : 0xFFEFEFEF);
    }

    private void drawLists(PoseStack pose, int mouseX, int mouseY, MsgSyncOptions data) {
        List<MsgSyncOptions.OptionEntry> options = visibleOptions(data);
        this.hoveredOption = null;
        this.hoveredTarget = null;

        int leftX = this.leftPos + LEFT_LIST_X;
        int listY = this.topPos + LIST_Y;
        for (int row = 0; row < LIST_ROWS; row++) {
            int index = this.optionScroll + row;
            if (index >= options.size()) {
                break;
            }
            MsgSyncOptions.OptionEntry entry = options.get(index);
            int rowY = listY + row * LIST_ROW_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, leftX, rowY, LEFT_LIST_W, LIST_ROW_HEIGHT);
            if (entry.id().equals(this.selectedOptionId) && entry.type() == this.selectedOptionType) {
                fill(pose, leftX, rowY, leftX + LEFT_LIST_W, rowY + LIST_ROW_HEIGHT, 0x664A8AD8);
            } else if (hovered) {
                fill(pose, leftX, rowY, leftX + LEFT_LIST_W, rowY + LIST_ROW_HEIGHT, 0x33FFFFFF);
            }
            if (hovered) {
                this.hoveredOption = entry.display();
            }
            this.font.draw(pose, fitText(entry.display().getString(), LEFT_LIST_W - 6),
                    leftX + 3, rowY + 5, 0xFFEFEFEF);
        }

        int rightX = this.leftPos + RIGHT_LIST_X;
        List<Row> rows = targetRows(data);
        for (int row = 0; row < LIST_ROWS; row++) {
            int index = this.targetScroll + row;
            if (index >= rows.size()) {
                break;
            }
            Row targetRow = rows.get(index);
            int rowY = listY + row * LIST_ROW_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, rightX, rowY, RIGHT_LIST_W, LIST_ROW_HEIGHT);
            if (index == this.selectedTarget) {
                fill(pose, rightX, rowY, rightX + RIGHT_LIST_W, rowY + LIST_ROW_HEIGHT, 0x664A8AD8);
            } else if (hovered) {
                fill(pose, rightX, rowY, rightX + RIGHT_LIST_W, rowY + LIST_ROW_HEIGHT, 0x33FFFFFF);
            }
            if (hovered) {
                this.hoveredTarget = targetRow.label();
            }
            this.font.draw(pose, fitText(targetRow.label().getString(), RIGHT_LIST_W - 6),
                    rightX + 3, rowY + 5, targetRow.addEntry() ? 0xFF7BD87B : 0xFFEFEFEF);
        }

        if (!data.hasGear) {
            Component hint = new TranslatableComponent("gui.gearsealerpro.insert_gear");
            this.font.draw(pose, hint, this.leftPos + 8, this.topPos + 120, 0xFF8A8A94);
        }
    }

    private void drawCost(PoseStack pose, MsgSyncOptions data) {
        if (!data.hasGear) {
            return;
        }
        this.drawCostLine(pose, 8, 172, data.haveGold, data.costGold, "Vault Gold", "item.the_vault.vault_gold");
        this.drawCostLine(pose, 8, 182, data.havePainite, data.costPainite, "Painite", "item.the_vault.painite_gem");
        // Focus is shown on its own row to keep the numbers readable.
        Component focusLabel = new TranslatableComponent("gui.gearsealerpro.focus",
                data.haveFocus, data.costFocus);
        this.font.draw(pose, focusLabel, this.leftPos + 8, this.topPos + 192,
                data.haveFocus >= data.costFocus ? 0xFF9BE39B : 0xFFE39B9B);
    }

    private void drawCostLine(PoseStack pose, int x, int y, int have, int need, String fallback, String ignoredKey) {
        Component label = new TextComponent(fallback + " " + have + "/" + need);
        this.font.draw(pose, label, this.leftPos + x, this.topPos + y,
                have >= need ? 0xFF9BE39B : 0xFFE39B9B);
    }

    private void drawSealButton(PoseStack pose, int mouseX, int mouseY, MsgSyncOptions data) {
        if (!data.hasGear) {
            return;
        }
        boolean enabled = canSeal(data);
        int x = this.leftPos + SEAL_X;
        int y = this.topPos + SEAL_Y;
        boolean hovered = isInside(mouseX, mouseY, x, y, SEAL_W, SEAL_H);
        int background = enabled ? (hovered ? 0xFF5C8A3E : 0xFF4A6B34) : 0xFF2A2A31;
        fill(pose, x, y, x + SEAL_W, y + SEAL_H, background);
        fill(pose, x, y, x + SEAL_W, y + 1, 0xFF6B6B76);
        Component label = new TranslatableComponent("gui.gearsealerpro.seal");
        this.font.draw(pose, label, x + (SEAL_W - this.font.width(label)) / 2F, y + 7,
                enabled ? 0xFFFFFFFF : 0xFF77777F);
    }

    private boolean canSeal(MsgSyncOptions data) {
        return data.canSeal && this.selectedOptionId != null;
    }

    private List<MsgSyncOptions.OptionEntry> visibleOptions(MsgSyncOptions data) {
        int type = currentType(data);
        List<MsgSyncOptions.OptionEntry> result = new ArrayList<>();
        for (MsgSyncOptions.OptionEntry entry : data.options) {
            if (entry.type() == type) {
                result.add(entry);
            }
        }
        return result;
    }

    private int currentType(MsgSyncOptions data) {
        if (this.selectedTarget > 0 && this.selectedTarget - 1 < data.targets.size()) {
            return data.targets.get(this.selectedTarget - 1).type();
        }
        return this.toggleType.ordinal();
    }

    private record Row(Component label, boolean addEntry) {
    }

    private List<Row> targetRows(MsgSyncOptions data) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(new TranslatableComponent("gui.gearsealerpro.add_affix"), true));
        for (MsgSyncOptions.TargetEntry entry : data.targets) {
            rows.add(new Row(entry.display(), false));
        }
        return rows;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.handleCustomClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCustomClick(double mouseX, double mouseY) {
        MsgSyncOptions data = ClientSealData.get();
        if (!data.hasGear) {
            return false;
        }

        int toggleY = this.topPos + TOGGLE_Y;
        if (mouseY >= toggleY && mouseY < toggleY + TOGGLE_H) {
            int toggleX = this.leftPos + LEFT_LIST_X;
            if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_W) {
                this.toggleType = VaultGearModifier.AffixType.PREFIX;
                this.selectedTarget = 0;
                return true;
            }
            toggleX += TOGGLE_W + 4;
            if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_W) {
                this.toggleType = VaultGearModifier.AffixType.SUFFIX;
                this.selectedTarget = 0;
                return true;
            }
        }

        int leftX = this.leftPos + LEFT_LIST_X;
        int listY = this.topPos + LIST_Y;
        if (isInside(mouseX, mouseY, leftX, listY, LEFT_LIST_W, LIST_HEIGHT)) {
            int row = (int) ((mouseY - listY) / LIST_ROW_HEIGHT);
            List<MsgSyncOptions.OptionEntry> options = visibleOptions(data);
            int index = this.optionScroll + row;
            if (index >= 0 && index < options.size()) {
                MsgSyncOptions.OptionEntry entry = options.get(index);
                this.selectedOptionId = entry.id();
                this.selectedOptionType = entry.type();
                return true;
            }
            return false;
        }

        int rightX = this.leftPos + RIGHT_LIST_X;
        if (isInside(mouseX, mouseY, rightX, listY, RIGHT_LIST_W, LIST_HEIGHT)) {
            int row = (int) ((mouseY - listY) / LIST_ROW_HEIGHT);
            int index = this.targetScroll + row;
            List<Row> rows = targetRows(data);
            if (index >= 0 && index < rows.size()) {
                this.selectedTarget = index;
                this.optionScroll = 0;
                return true;
            }
            return false;
        }

        if (canSeal(data) && isInside(mouseX, mouseY, this.leftPos + SEAL_X, this.topPos + SEAL_Y, SEAL_W, SEAL_H)) {
            VaultGearModifier.AffixType type = VaultGearModifier.AffixType.values()[currentType(data)];
            NetworkHandler.sendToServer(new MsgCommitSeal(this.selectedOptionId, type, this.selectedTarget - 1));
            this.selectedOptionId = null;
            this.selectedOptionType = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listY = this.topPos + LIST_Y;
        int leftX = this.leftPos + LEFT_LIST_X;
        int rightX = this.leftPos + RIGHT_LIST_X;
        MsgSyncOptions data = ClientSealData.get();
        if (isInside(mouseX, mouseY, leftX, listY, LEFT_LIST_W, LIST_HEIGHT)) {
            int max = Math.max(0, visibleOptions(data).size() - LIST_ROWS);
            this.optionScroll = clamp(this.optionScroll + (delta < 0 ? 1 : -1), 0, max);
            return true;
        }
        if (isInside(mouseX, mouseY, rightX, listY, RIGHT_LIST_W, LIST_HEIGHT)) {
            int max = Math.max(0, targetRows(data).size() - LIST_ROWS);
            this.targetScroll = clamp(this.targetScroll + (delta < 0 ? 1 : -1), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String fitText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && this.font.width(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }
}
