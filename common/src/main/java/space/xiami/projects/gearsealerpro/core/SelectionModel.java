package space.xiami.projects.gearsealerpro.core;

import space.xiami.projects.gearsealerpro.api.SealOptionKey;
import space.xiami.projects.gearsealerpro.api.SealRequest;

/**
 * Pure state machine behind the Gear Sealer Pro screen: what is selected, which affix side is
 * being used, and how far the two lists are scrolled. No rendering, no item stacks, so it can be
 * unit-tested without Minecraft.
 */
public final class SelectionModel {

    /** Right-hand list row that adds a new affix instead of replacing an existing one. */
    public static final int ADD_ROW = 0;

    private SealOptionKey selectedOption;
    private int selectedTargetRow = ADD_ROW;
    private boolean addPrefix = true;
    private boolean targetPrefix = true;
    private int optionScroll;
    private int targetScroll;

    public SealOptionKey selectedOption() {
        return this.selectedOption;
    }

    public boolean hasOption() {
        return this.selectedOption != null;
    }

    public void selectOption(SealOptionKey key) {
        this.selectedOption = key;
    }

    public void clearOption() {
        this.selectedOption = null;
    }

    public int selectedTargetRow() {
        return this.selectedTargetRow;
    }

    public boolean isAddMode() {
        return this.selectedTargetRow == ADD_ROW;
    }

    /** Affix side of the operation: the target's side when replacing, the toggle's side when adding. */
    public boolean effectivePrefix() {
        return isAddMode() ? this.addPrefix : this.targetPrefix;
    }

    public void setAddPrefix(boolean prefix) {
        this.addPrefix = prefix;
        this.selectedTargetRow = ADD_ROW;
    }

    /**
     * Selects a row of the target list.
     *
     * @param row    {@link #ADD_ROW} to add a new affix, otherwise target index + 1
     * @param prefix affix side of that target (ignored for {@link #ADD_ROW})
     */
    public void selectTarget(int row, boolean prefix) {
        if (row < ADD_ROW) {
            throw new IllegalArgumentException("row must be >= " + ADD_ROW);
        }
        this.selectedTargetRow = row;
        if (row > ADD_ROW) {
            this.targetPrefix = prefix;
        }
    }

    /** Target index sent to the server: -1 for "add a new affix". */
    public int targetIndex() {
        return isAddMode() ? SealRequest.ADD_NEW_AFFIX : this.selectedTargetRow - 1;
    }

    public SealRequest toRequest() {
        if (this.selectedOption == null) {
            throw new IllegalStateException("no sealed modifier selected");
        }
        return new SealRequest(this.selectedOption, effectivePrefix(), targetIndex());
    }

    public int optionScroll() {
        return this.optionScroll;
    }

    public int targetScroll() {
        return this.targetScroll;
    }

    public void scrollOptions(int delta, int totalOptions) {
        this.optionScroll = clampScroll(this.optionScroll, delta, PanelLayout.VISIBLE_ROWS, totalOptions);
    }

    public void scrollTargets(int delta, int totalTargets) {
        this.targetScroll = clampScroll(this.targetScroll, delta, PanelLayout.VISIBLE_ROWS, totalTargets);
    }

    /** Resets the option list scroll, e.g. after the visible type filter changed. */
    public void resetOptionScroll() {
        this.optionScroll = 0;
    }

    public static int clampScroll(int current, int delta, int visibleRows, int total) {
        int max = Math.max(0, total - visibleRows);
        int next = current + delta;
        return Math.max(0, Math.min(max, next));
    }
}
