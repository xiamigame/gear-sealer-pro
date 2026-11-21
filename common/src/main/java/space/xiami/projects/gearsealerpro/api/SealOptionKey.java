package space.xiami.projects.gearsealerpro.api;

import java.util.Objects;

/**
 * Identifies one selectable sealed modifier by its registry id.
 *
 * <p>The affix side is not part of the identity: most sealed modifiers can occupy either a
 * prefix or a suffix slot. The side is chosen when the request is built (see
 * {@link SealRequest#prefix()}).</p>
 */
public record SealOptionKey(String id) {

    public SealOptionKey {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("modifier id must not be blank");
        }
    }
}
