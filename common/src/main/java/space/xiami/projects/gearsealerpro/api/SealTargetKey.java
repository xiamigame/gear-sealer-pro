package space.xiami.projects.gearsealerpro.api;

/**
 * Identifies the affix a sealing operation replaces.
 *
 * @param index  position in the server-side target list (prefixes first, then suffixes)
 * @param prefix whether the target is a prefix
 */
public record SealTargetKey(int index, boolean prefix) {

    public SealTargetKey {
        if (index < 0) {
            throw new IllegalArgumentException("target index must be >= 0");
        }
    }
}
