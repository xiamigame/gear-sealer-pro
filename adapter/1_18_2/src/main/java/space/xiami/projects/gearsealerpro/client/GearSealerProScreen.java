package space.xiami.projects.gearsealerpro.client;

import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.gear.attribute.VaultGearModifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import space.xiami.projects.gearsealerpro.api.SealOptionKey;
import space.xiami.projects.gearsealerpro.api.SealRequest;
import space.xiami.projects.gearsealerpro.core.CostView;
import space.xiami.projects.gearsealerpro.core.PanelLayout;
import space.xiami.projects.gearsealerpro.core.SelectionModel;
import space.xiami.projects.gearsealerpro.menu.GearSealerProMenu;
import space.xiami.projects.gearsealerpro.network.MsgCommitSeal;
import space.xiami.projects.gearsealerpro.network.MsgRequestOptions;
import space.xiami.projects.gearsealerpro.network.MsgSyncOptions;
import space.xiami.projects.gearsealerpro.network.NetworkHandler;

/**
 * The Gear Sealer Pro screen: pick a sealed modifier on the left, pick what it replaces on
 * the right (first row adds a brand new affix), then seal.
 */
public class GearSealerProScreen extends AbstractContainerScreen<GearSealerProMenu> {

    private static final int PANEL_WIDTH = PanelLayout.WIDTH;
    private static final int PANEL_HEIGHT = PanelLayout.HEIGHT;
    private static final int LIST_Y = PanelLayout.LIST_Y;
    private static final int LIST_ROW_HEIGHT = PanelLayout.ROW_HEIGHT;
    private static final int LIST_ROWS = PanelLayout.VISIBLE_ROWS;
    private static final int LIST_HEIGHT = PanelLayout.LIST_HEIGHT;
    private static final int LEFT_LIST_X = PanelLayout.LEFT_LIST_X;
    private static final int LEFT_LIST_W = PanelLayout.LEFT_LIST_WIDTH;
    private static final int RIGHT_LIST_X = PanelLayout.RIGHT_LIST_X;
    private static final int RIGHT_LIST_W = PanelLayout.RIGHT_LIST_WIDTH;
    private static final int TOGGLE_X = PanelLayout.TOGGLE_X;
    private static final int TOGGLE_Y = PanelLayout.TOGGLE_Y;
    private static final int TOGGLE_W = PanelLayout.TOGGLE_WIDTH;
    private static final int TOGGLE_H = PanelLayout.TOGGLE_HEIGHT;
    private static final int TOGGLE_GAP = PanelLayout.TOGGLE_GAP;
    private static final int SEAL_X = PanelLayout.SEAL_X;
    private static final int SEAL_Y = PanelLayout.SEAL_Y;
    private static final int SEAL_W = PanelLayout.SEAL_WIDTH;
    private static final int SEAL_H = PanelLayout.SEAL_HEIGHT;

    private final SelectionModel selection = new SelectionModel();

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
        this.drawSideToggles(pose, mouseX, mouseY, data);
        this.drawLists(pose, mouseX, mouseY, data);
        this.drawCost(pose, data);
        this.drawSealButton(pose, mouseX, mouseY, data);

        // Item tooltips are drawn inside super.render(), then covered by our own panel
        // drawing above, so redraw the hovered slot's tooltip on top.
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            this.renderTooltip(pose, this.hoveredSlot.getItem(), mouseX, mouseY);
        }
        if (this.hoveredOption != null) {
            this.renderComponentTooltip(pose, List.of(this.hoveredOption), mouseX, mouseY);
        } else if (this.hoveredTarget != null) {
            this.renderComponentTooltip(pose, List.of(this.hoveredTarget), mouseX, mouseY);
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

    /**
     * The affix side is only a real choice when adding a new affix; when replacing, the side
     * is decided by the target row the player picked.
     */
    private void drawSideToggles(PoseStack pose, int mouseX, int mouseY, MsgSyncOptions data) {
        if (!this.selection.isAddMode() || !data.hasGear) {
            return;
        }
        boolean prefix = this.selection.effectivePrefix();
        this.drawToggle(pose, mouseX, mouseY, TOGGLE_X, prefix, true);
        this.drawToggle(pose, mouseX, mouseY, TOGGLE_X + TOGGLE_W + TOGGLE_GAP, !prefix, false);
    }

    private void drawToggle(PoseStack pose, int mouseX, int mouseY, int x, boolean prefix, boolean active) {
        int screenX = this.leftPos + x;
        int screenY = this.topPos + TOGGLE_Y;
        boolean hovered = PanelLayout.inside(mouseX, mouseY, screenX, screenY, TOGGLE_W, TOGGLE_H);
        int background = active ? 0xFF4A6B34 : (hovered ? 0xFF3A3A44 : 0xFF2A2A31);
        fill(pose, screenX, screenY, screenX + TOGGLE_W, screenY + TOGGLE_H, background);
        Component label = new TranslatableComponent(prefix
                ? "gui.gearsealerpro.prefix" : "gui.gearsealerpro.suffix");
        this.font.draw(pose, label, screenX + (TOGGLE_W - this.font.width(label)) / 2F,
                screenY + 3, active ? 0xFFEFEFEF : 0xFF9A9AA4);
    }

    private void drawLists(PoseStack pose, int mouseX, int mouseY, MsgSyncOptions data) {
        List<MsgSyncOptions.OptionEntry> options = visibleOptions(data);
        ItemStack gear = this.menu.getGearInputSlot().getItem();
        this.hoveredOption = null;
        this.hoveredTarget = null;

        int leftX = this.leftPos + LEFT_LIST_X;
        int listY = this.topPos + LIST_Y;
        for (int row = 0; row < LIST_ROWS; row++) {
            int index = this.selection.optionScroll() + row;
            if (index >= options.size()) {
                break;
            }
            MsgSyncOptions.OptionEntry entry = options.get(index);
            int rowY = listY + row * LIST_ROW_HEIGHT;
            boolean hovered = PanelLayout.inside(mouseX, mouseY, leftX, rowY, LEFT_LIST_W, LIST_ROW_HEIGHT);
            if (isSelected(entry)) {
                fill(pose, leftX, rowY, leftX + LEFT_LIST_W, rowY + LIST_ROW_HEIGHT, 0x664A8AD8);
            } else if (hovered) {
                fill(pose, leftX, rowY, leftX + LEFT_LIST_W, rowY + LIST_ROW_HEIGHT, 0x33FFFFFF);
            }
            Component label = ClientSealData.optionLabel(gear, entry.id());
            if (hovered) {
                this.hoveredOption = label;
            }
            this.font.draw(pose, fitText(label.getString(), LEFT_LIST_W - 6),
                    leftX + 3, rowY + 5, 0xFFEFEFEF);
        }

        int rightX = this.leftPos + RIGHT_LIST_X;
        List<Row> rows = targetRows(data, gear);
        for (int row = 0; row < LIST_ROWS; row++) {
            int index = this.selection.targetScroll() + row;
            if (index >= rows.size()) {
                break;
            }
            Row targetRow = rows.get(index);
            int rowY = listY + row * LIST_ROW_HEIGHT;
            boolean hovered = PanelLayout.inside(mouseX, mouseY, rightX, rowY, RIGHT_LIST_W, LIST_ROW_HEIGHT);
            if (index == this.selection.selectedTargetRow()) {
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
        CostView cost = new CostView(data.haveGold, data.costGold, data.havePainite, data.costPainite,
                data.haveFocus, data.costFocus);

        Component gold = new TextComponent("Vault Gold " + cost.goldText());
        this.font.draw(pose, gold, this.leftPos + 8, this.topPos + 172,
                cost.goldEnough() ? 0xFF9BE39B : 0xFFE39B9B);

        Component painite = new TextComponent("Painite " + cost.painiteText());
        this.font.draw(pose, painite, this.leftPos + 8, this.topPos + 182,
                cost.painiteEnough() ? 0xFF9BE39B : 0xFFE39B9B);

        Component focus = new TranslatableComponent("gui.gearsealerpro.focus",
                data.haveFocus, data.costFocus);
        this.font.draw(pose, focus, this.leftPos + 8, this.topPos + 192,
                cost.focusEnough() ? 0xFF9BE39B : 0xFFE39B9B);
    }

    private void drawSealButton(PoseStack pose, int mouseX, int mouseY, MsgSyncOptions data) {
        if (!data.hasGear) {
            return;
        }
        boolean enabled = canSeal(data);
        int x = this.leftPos + SEAL_X;
        int y = this.topPos + SEAL_Y;
        boolean hovered = PanelLayout.inside(mouseX, mouseY, x, y, SEAL_W, SEAL_H);
        int background = enabled ? (hovered ? 0xFF5C8A3E : 0xFF4A6B34) : 0xFF2A2A31;
        fill(pose, x, y, x + SEAL_W, y + SEAL_H, background);
        fill(pose, x, y, x + SEAL_W, y + 1, 0xFF6B6B76);
        Component label = new TranslatableComponent("gui.gearsealerpro.seal");
        this.font.draw(pose, label, x + (SEAL_W - this.font.width(label)) / 2F, y + 7,
                enabled ? 0xFFFFFFFF : 0xFF77777F);
    }

    private boolean canSeal(MsgSyncOptions data) {
        return data.canSeal && this.selection.hasOption();
    }

    private boolean isSelected(MsgSyncOptions.OptionEntry entry) {
        return this.selection.hasOption()
                && this.selection.selectedOption().id().equals(entry.id().toString());
    }

    private List<MsgSyncOptions.OptionEntry> visibleOptions(MsgSyncOptions data) {
        boolean prefix = this.selection.effectivePrefix();
        List<MsgSyncOptions.OptionEntry> result = new ArrayList<>();
        for (MsgSyncOptions.OptionEntry entry : data.options) {
            if (entry.allows(prefix)) {
                result.add(entry);
            }
        }
        return result;
    }

    private record Row(Component label, boolean addEntry) {
    }

    private List<Row> targetRows(MsgSyncOptions data, ItemStack gear) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(new TranslatableComponent("gui.gearsealerpro.add_affix"), true));
        List<Component> labels = ClientSealData.targetLabels(gear);
        for (int i = 0; i < data.targets.size(); i++) {
            boolean prefix = data.targets.get(i).prefix();
            Component label = i < labels.size()
                    ? labels.get(i)
                    : new TextComponent((prefix ? "prefix" : "suffix") + " #" + (i + 1));
            rows.add(new Row(label, false));
        }
        return rows;
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

        if (this.selection.isAddMode()) {
            int toggleY = this.topPos + TOGGLE_Y;
            if (mouseY >= toggleY && mouseY < toggleY + TOGGLE_H) {
                int toggleX = this.leftPos + TOGGLE_X;
                if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_W) {
                    this.selection.setAddPrefix(true);
                    this.selection.resetOptionScroll();
                    return true;
                }
                toggleX += TOGGLE_W + TOGGLE_GAP;
                if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_W) {
                    this.selection.setAddPrefix(false);
                    this.selection.resetOptionScroll();
                    return true;
                }
            }
        }

        int leftX = this.leftPos + LEFT_LIST_X;
        int listY = this.topPos + LIST_Y;
        if (PanelLayout.inside(mouseX, mouseY, leftX, listY, LEFT_LIST_W, LIST_HEIGHT)) {
            int row = (int) ((mouseY - listY) / LIST_ROW_HEIGHT);
            List<MsgSyncOptions.OptionEntry> options = visibleOptions(data);
            int index = this.selection.optionScroll() + row;
            if (index >= 0 && index < options.size()) {
                this.selection.selectOption(new SealOptionKey(options.get(index).id().toString()));
                return true;
            }
            return false;
        }

        int rightX = this.leftPos + RIGHT_LIST_X;
        if (PanelLayout.inside(mouseX, mouseY, rightX, listY, RIGHT_LIST_W, LIST_HEIGHT)) {
            int row = (int) ((mouseY - listY) / LIST_ROW_HEIGHT);
            int index = this.selection.targetScroll() + row;
            List<Row> rows = targetRows(data, this.menu.getGearInputSlot().getItem());
            if (index >= 0 && index < rows.size()) {
                boolean prefix = index == SelectionModel.ADD_ROW
                        || data.targets.get(index - 1).prefix();
                this.selection.selectTarget(index, prefix);
                if (this.selection.hasOption() && !optionStillAllows(data, prefix)) {
                    this.selection.clearOption();
                }
                this.selection.resetOptionScroll();
                return true;
            }
            return false;
        }

        if (canSeal(data) && PanelLayout.inside(mouseX, mouseY, this.leftPos + SEAL_X,
                this.topPos + SEAL_Y, SEAL_W, SEAL_H)) {
            this.sendSeal(data);
            return true;
        }
        return false;
    }

    private boolean optionStillAllows(MsgSyncOptions data, boolean prefix) {
        String id = this.selection.selectedOption().id();
        for (MsgSyncOptions.OptionEntry entry : data.options) {
            if (entry.id().toString().equals(id) && entry.allows(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void sendSeal(MsgSyncOptions data) {
        SealRequest request = this.selection.toRequest();
        MsgSyncOptions.OptionEntry chosen = null;
        for (MsgSyncOptions.OptionEntry entry : data.options) {
            if (entry.id().toString().equals(request.option().id()) && entry.allows(request.prefix())) {
                chosen = entry;
                break;
            }
        }
        if (chosen == null) {
            return;
        }
        VaultGearModifier.AffixType type = request.prefix()
                ? VaultGearModifier.AffixType.PREFIX
                : VaultGearModifier.AffixType.SUFFIX;
        NetworkHandler.sendToServer(new MsgCommitSeal(chosen.id(), type, request.targetIndex()));
        this.selection.clearOption();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listY = this.topPos + LIST_Y;
        int leftX = this.leftPos + LEFT_LIST_X;
        int rightX = this.leftPos + RIGHT_LIST_X;
        MsgSyncOptions data = ClientSealData.get();
        int step = delta < 0 ? 1 : -1;
        if (PanelLayout.inside(mouseX, mouseY, leftX, listY, LEFT_LIST_W, LIST_HEIGHT)) {
            this.selection.scrollOptions(step, visibleOptions(data).size());
            return true;
        }
        if (PanelLayout.inside(mouseX, mouseY, rightX, listY, RIGHT_LIST_W, LIST_HEIGHT)) {
            this.selection.scrollTargets(step,
                    targetRows(data, this.menu.getGearInputSlot().getItem()).size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
