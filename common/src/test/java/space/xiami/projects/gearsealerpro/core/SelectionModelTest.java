package space.xiami.projects.gearsealerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import space.xiami.projects.gearsealerpro.api.SealOptionKey;
import space.xiami.projects.gearsealerpro.api.SealRequest;

class SelectionModelTest {

    @Test
    void startsInAddModeWithPrefixSide() {
        SelectionModel model = new SelectionModel();

        assertTrue(model.isAddMode());
        assertTrue(model.effectivePrefix());
        assertEquals(SelectionModel.ADD_ROW, model.selectedTargetRow());
        assertEquals(-1, model.targetIndex());
        assertNull(model.selectedOption());
    }

    @Test
    void selectingATargetSwitchesSideAndIndex() {
        SelectionModel model = new SelectionModel();

        model.selectTarget(3, false);

        assertFalse(model.isAddMode());
        assertFalse(model.effectivePrefix());
        assertEquals(2, model.targetIndex());
    }

    @Test
    void returningToAddRowUsesPrefixAgain() {
        SelectionModel model = new SelectionModel();
        model.selectTarget(2, false);

        model.selectTarget(SelectionModel.ADD_ROW, false);

        assertTrue(model.isAddMode());
        assertTrue(model.effectivePrefix());
    }

    @Test
    void optionSelectionIsRememberedAndClearable() {
        SelectionModel model = new SelectionModel();
        SealOptionKey key = new SealOptionKey("the_vault:ability_power");

        model.selectOption(key);
        assertTrue(model.hasOption());
        assertEquals(key, model.selectedOption());

        model.clearOption();
        assertFalse(model.hasOption());
    }

    @Test
    void scrollIsClampedToTheVisibleWindow() {
        assertEquals(0, SelectionModel.clampScroll(0, 3, 4, 2));
        assertEquals(3, SelectionModel.clampScroll(0, 3, 4, 10));
        assertEquals(6, SelectionModel.clampScroll(6, 100, 4, 10));
        assertEquals(0, SelectionModel.clampScroll(2, -5, 4, 10));
    }

    @Test
    void scrollingUsesTheClampedHelper() {
        SelectionModel model = new SelectionModel();

        model.scrollOptions(2, 10);
        assertEquals(2, model.optionScroll());

        model.scrollTargets(99, 5);
        assertEquals(1, model.targetScroll());
    }

    @Test
    void negativeRowsAreRejected() {
        SelectionModel model = new SelectionModel();
        assertThrows(IllegalArgumentException.class, () -> model.selectTarget(-1, true));
    }

    @Test
    void requestWithoutSelectionFails() {
        SelectionModel model = new SelectionModel();
        assertThrows(IllegalStateException.class, model::toRequest);
    }

    @Test
    void requestCarriesTheEffectiveSideAndTarget() {
        SelectionModel model = new SelectionModel();
        model.selectOption(new SealOptionKey("the_vault:ability_power"));
        model.selectTarget(2, false);

        SealRequest request = model.toRequest();

        assertEquals("the_vault:ability_power", request.option().id());
        assertFalse(request.prefix());
        assertEquals(1, request.targetIndex());
        assertFalse(request.addsAffix());
    }

    @Test
    void addModeAlwaysUsesPrefix() {
        SelectionModel model = new SelectionModel();
        model.selectTarget(2, false);
        model.selectOption(new SealOptionKey("the_vault:thorns"));
        model.selectTarget(SelectionModel.ADD_ROW, false);

        SealRequest request = model.toRequest();

        assertTrue(request.addsAffix());
        assertTrue(request.prefix());
        assertEquals(SealRequest.ADD_NEW_AFFIX, request.targetIndex());
    }
}
