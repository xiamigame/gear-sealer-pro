package space.xiami.projects.gearsealerpro.client;

import iskallia.vault.config.gear.VaultGearTierConfig;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.data.VaultGearData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import space.xiami.projects.gearsealerpro.network.MsgSyncOptions;

/**
 * Latest option snapshot pushed by the server, plus the labels rendered for it.
 *
 * <p>The labels are built here, on the client, because The Vault's display helpers are
 * {@code @OnlyIn(Dist.CLIENT)}: the server only sends modifier ids and affix sides.</p>
 */
public final class ClientSealData {

    private static final long DISPLAY_SEED = 0x5EEDL;

    private static MsgSyncOptions current = new MsgSyncOptions();
    private static MsgSyncOptions labelledSnapshot;
    private static ItemStack labelledGear = ItemStack.EMPTY;
    private static Map<String, Component> optionLabels = Map.of();
    private static List<Component> targetLabels = List.of();

    public static MsgSyncOptions get() {
        return current;
    }

    public static void update(MsgSyncOptions msg) {
        current = msg;
    }

    /** Label for one sealed modifier, including its roll range when the client has the config. */
    public static Component optionLabel(ItemStack gear, ResourceLocation id) {
        ensureLabels(gear);
        Component label = optionLabels.get(id.toString());
        return label != null ? label : new TextComponent(id.toString());
    }

    /** Labels for the replaceable affixes, in the same prefix-then-suffix order as the server. */
    public static List<Component> targetLabels(ItemStack gear) {
        ensureLabels(gear);
        return targetLabels;
    }

    private static void ensureLabels(ItemStack gear) {
        if (current == labelledSnapshot && ItemStack.matches(gear, labelledGear)) {
            return;
        }
        Map<String, Component> options = new HashMap<>();
        List<Component> targets = new ArrayList<>();

        VaultGearTierConfig config = VaultGearTierConfig.getConfig(gear).orElse(null);
        if (!gear.isEmpty() && VaultGearData.hasData(gear)) {
            VaultGearData data = VaultGearData.read(gear);
            for (MsgSyncOptions.OptionEntry entry : current.options) {
                options.put(entry.id().toString(), buildOptionLabel(config, data, gear, entry));
            }
            for (VaultGearModifier.AffixType type : new VaultGearModifier.AffixType[]{
                    VaultGearModifier.AffixType.PREFIX, VaultGearModifier.AffixType.SUFFIX}) {
                for (VaultGearModifier<?> modifier : data.getModifiers(type)) {
                    if (modifier.canBeModified()) {
                        targets.add(buildModifierLabel(modifier, data, type, gear));
                    }
                }
            }
        }

        optionLabels = options;
        targetLabels = targets;
        labelledSnapshot = current;
        labelledGear = gear.copy();
    }

    private static Component buildOptionLabel(VaultGearTierConfig config, VaultGearData data, ItemStack gear,
                                              MsgSyncOptions.OptionEntry entry) {
        if (config != null) {
            try {
                VaultGearTierConfig.ModifierTierGroup group = config.getTierGroup(entry.id());
                if (group != null) {
                    List<VaultGearTierConfig.ModifierTier<?>> tiers =
                            group.getModifiersForLevel(data.getItemLevel());
                    if (!tiers.isEmpty()) {
                        VaultGearModifier<?> sample =
                                new VaultGearTierConfig.ModifierOutcome<>(tiers.get(0), group)
                                        .makeModifier(new Random(DISPLAY_SEED));
                        sample.addCategory(VaultGearModifier.AffixCategory.CORRUPTED);
                        VaultGearModifier.AffixType type = entry.allows(true)
                                ? VaultGearModifier.AffixType.PREFIX
                                : VaultGearModifier.AffixType.SUFFIX;
                        Optional<MutableComponent> display = sample.getDisplay(data, type, gear, true);
                        if (display.isPresent()) {
                            return display.get();
                        }
                    }
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Fall through to the identifier; the list must never break because of a label.
            }
        }
        return new TextComponent(entry.id().toString());
    }

    private static Component buildModifierLabel(VaultGearModifier<?> modifier, VaultGearData data,
                                                VaultGearModifier.AffixType type, ItemStack gear) {
        try {
            Optional<MutableComponent> display = modifier.getDisplay(data, type, gear, false);
            if (display.isPresent()) {
                return display.get();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Fall through to the identifier.
        }
        return new TextComponent(String.valueOf(modifier.getModifierIdentifier()));
    }

    private ClientSealData() {
    }
}
