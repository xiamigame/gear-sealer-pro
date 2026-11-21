package space.xiami.projects.gearsealerpro.core;

/**
 * Screen geometry of the Gear Sealer Pro, in GUI units relative to the panel origin.
 * Rendering and hit-testing both use these values so layout stays in one place.
 */
public final class PanelLayout {

    public static final int WIDTH = 240;
    public static final int HEIGHT = 286;

    public static final int LIST_Y = 96;
    public static final int ROW_HEIGHT = 18;
    public static final int VISIBLE_ROWS = 4;
    public static final int LIST_HEIGHT = ROW_HEIGHT * VISIBLE_ROWS;

    public static final int LEFT_LIST_X = 8;
    public static final int LEFT_LIST_WIDTH = 110;
    public static final int RIGHT_LIST_X = 122;
    public static final int RIGHT_LIST_WIDTH = 110;

    public static final int TOGGLE_X = 31;
    public static final int TOGGLE_Y = 66;
    public static final int TOGGLE_WIDTH = 36;
    public static final int TOGGLE_HEIGHT = 14;
    public static final int TOGGLE_GAP = 4;

    public static final int SEAL_X = 150;
    public static final int SEAL_Y = 166;
    public static final int SEAL_WIDTH = 82;
    public static final int SEAL_HEIGHT = 22;

    /** Player inventory slots start here so the 9 columns stay centred in the wider panel. */
    public static final int INVENTORY_X = 39;

    public static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private PanelLayout() {
    }
}
