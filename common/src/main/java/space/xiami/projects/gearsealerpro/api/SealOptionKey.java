package space.xiami.projects.gearsealerpro.api;

import java.util.Objects;

/**
 * Identifies one selectable sealed modifier: the modifier identifier plus the affix side it
 * would occupy. Two entries in the selection list may share an id but differ by side.
 */
public record SealOptionKey(String id, boolean prefix) {

    public SealOptionKey {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("modifier id must not be blank");
        }
    }
}
