package space.xiami.projects.gearsealerpro.api;

import java.util.Objects;

/**
 * One sealing request: which sealed modifier to apply, on which affix side, and where.
 *
 * @param option      the selected sealed modifier
 * @param prefix      whether the modifier lands in a prefix slot (false = suffix)
 * @param targetIndex target position in the server-side list, or {@link #ADD_NEW_AFFIX}
 */
public record SealRequest(SealOptionKey option, boolean prefix, int targetIndex) {

    public static final int ADD_NEW_AFFIX = -1;

    public SealRequest {
        Objects.requireNonNull(option, "option");
        if (targetIndex < ADD_NEW_AFFIX) {
            throw new IllegalArgumentException("target index must be >= " + ADD_NEW_AFFIX);
        }
    }

    public boolean addsAffix() {
        return this.targetIndex == ADD_NEW_AFFIX;
    }
}
