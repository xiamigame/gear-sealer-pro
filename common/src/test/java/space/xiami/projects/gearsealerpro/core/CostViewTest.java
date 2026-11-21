package space.xiami.projects.gearsealerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CostViewTest {

    @Test
    void affordableOnlyWhenEveryRequirementIsMet() {
        assertTrue(new CostView(10, 10, 2, 2, 1, 1).affordable());
        assertFalse(new CostView(9, 10, 2, 2, 1, 1).affordable());
        assertFalse(new CostView(10, 10, 1, 2, 1, 1).affordable());
        assertFalse(new CostView(10, 10, 2, 2, 0, 1).affordable());
    }

    @Test
    void zeroCostIsAlwaysEnough() {
        CostView free = new CostView(0, 0, 0, 0, 0, 0);
        assertTrue(free.goldEnough());
        assertTrue(free.painiteEnough());
        assertTrue(free.focusEnough());
        assertTrue(free.affordable());
    }

    @Test
    void textShowsHaveOverNeed() {
        CostView view = new CostView(12, 50, 3, 10, 1, 2);

        assertEquals("12/50", view.goldText());
        assertEquals("3/10", view.painiteText());
        assertEquals("1/2", view.focusText());
    }
}
