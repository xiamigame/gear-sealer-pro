package space.xiami.projects.gearsealerpro.logic;

import space.xiami.projects.gearsealerpro.menu.GearSealerProMenu;
import space.xiami.projects.gearsealerpro.network.MsgSyncOptions;
import iskallia.vault.config.gear.VaultGearCraftingConfig.ProficiencyStep;
import iskallia.vault.config.gear.VaultGearTierConfig;
import iskallia.vault.gear.GearRollHelper;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.crafting.VaultGearCraftingHelper;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.gear.modification.VaultSealerCost;
import iskallia.vault.gear.VaultGearModifierHelper;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.init.ModGearModifications;
import iskallia.vault.init.ModItems;
import iskallia.vault.init.ModSounds;
import iskallia.vault.item.gear.IVaultUsesItem;
import iskallia.vault.util.CoinDefinition;
import iskallia.vault.util.InventoryUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The sealing logic of the Gear Sealer Pro.
 *
 * <p>Cost, accepted gear and the final "corrupted" lock are copied from
 * {@code CorruptGearModification} / {@code VaultSealerRequestModificationMessage}; the only
 * behavioural difference is that the sealed modifier and the replaced affix are chosen by
 * the player instead of being rolled.</p>
 */
public final class SealerLogic {

    private static final long DISPLAY_SEED = 0x5EEDL;

    public record Option(ResourceLocation id, boolean prefixAllowed, boolean suffixAllowed, Component display,
                         VaultGearTierConfig.ModifierTierGroup group,
                         List<VaultGearTierConfig.ModifierTier<?>> tiers) {

        public boolean allows(VaultGearModifier.AffixType type) {
            return type == VaultGearModifier.AffixType.PREFIX ? this.prefixAllowed : this.suffixAllowed;
        }

        public VaultGearModifier<?> roll(Random random) {
            int total = 0;
            for (VaultGearTierConfig.ModifierTier<?> tier : tiers) {
                total += Math.max(1, tier.getWeight());
            }
            int pick = total <= 0 ? 0 : random.nextInt(total);
            VaultGearTierConfig.ModifierTier<?> chosen = tiers.get(0);
            for (VaultGearTierConfig.ModifierTier<?> tier : tiers) {
                pick -= Math.max(1, tier.getWeight());
                if (pick < 0) {
                    chosen = tier;
                    break;
                }
            }
            return new VaultGearTierConfig.ModifierOutcome<>(chosen, group).makeModifier(random);
        }
    }

    public record Target(VaultGearModifier.AffixType type, VaultGearModifier<?> modifier, Component display) {
    }

    private SealerLogic() {
    }

    public static boolean hasUnfilledAffixSlots(VaultGearData data) {
        int maxPrefixes = data.<Integer>getFirstValue(ModGearAttributes.PREFIXES).orElse(0);
        int maxSuffixes = data.<Integer>getFirstValue(ModGearAttributes.SUFFIXES).orElse(0);
        return data.getModifiers(VaultGearModifier.AffixType.PREFIX).size() < maxPrefixes
                || data.getModifiers(VaultGearModifier.AffixType.SUFFIX).size() < maxSuffixes;
    }

    /**
     * All sealed modifiers this piece of gear can receive. One entry per modifier id; the
     * entry records which affix sides it may occupy (the corrupted-implicit pool allows both,
     * the generic fallback pool pins one side).
     */
    public static List<Option> options(ItemStack gear) {
        VaultGearTierConfig config = VaultGearTierConfig.getConfig(gear).orElse(null);
        if (config == null) {
            return List.of();
        }
        VaultGearData data = VaultGearData.read(gear);
        int level = data.getItemLevel();
        Map<ResourceLocation, Option> byId = new LinkedHashMap<>();

        VaultGearTierConfig.AttributeGroup corrupted =
                config.getModifierGroup(VaultGearTierConfig.ModifierAffixTagGroup.CORRUPTED_IMPLICIT);
        if (corrupted != null) {
            for (VaultGearTierConfig.ModifierTierGroup group : corrupted) {
                if (group == null || group.getIdentifier() == null
                        || byId.containsKey(group.getIdentifier())
                        || !ProficiencyStep.isModifierAllowed(gear, group.getIdentifier())) {
                    continue;
                }
                List<VaultGearTierConfig.ModifierTier<?>> tiers = group.getModifiersForLevel(level);
                if (tiers.isEmpty()) {
                    continue;
                }
                byId.put(group.getIdentifier(), new Option(group.getIdentifier(), true, true,
                        display(group, tiers, VaultGearModifier.AffixType.PREFIX, gear, data), group, tiers));
            }
        }

        if (byId.isEmpty()) {
            for (Tuple<VaultGearTierConfig.ModifierAffixTagGroup, VaultGearTierConfig.ModifierTierGroup> tuple
                    : config.getGenericGroupsFulfilling(group -> !group.getTags().contains("noLegendary"))) {
                VaultGearTierConfig.ModifierAffixTagGroup affixGroup = tuple.getA();
                VaultGearTierConfig.ModifierTierGroup group = tuple.getB();
                if (!affixGroup.isGenericGroup()) {
                    continue;
                }
                VaultGearModifier.AffixType type = affixGroup.getTargetAffixType();
                if (type == null || group.getIdentifier() == null
                        || byId.containsKey(group.getIdentifier())
                        || !ProficiencyStep.isModifierAllowed(gear, group.getIdentifier())) {
                    continue;
                }
                List<VaultGearTierConfig.ModifierTier<?>> tiers = group.getModifiersForLevel(level);
                if (tiers.isEmpty()) {
                    continue;
                }
                boolean prefix = type == VaultGearModifier.AffixType.PREFIX;
                byId.put(group.getIdentifier(), new Option(group.getIdentifier(), prefix, !prefix,
                        display(group, tiers, type, gear, data), group, tiers));
            }
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * Builds the list label. {@code displayDetail = true} appends the modifier's config range
     * (e.g. {@code (1 - 3)}), which is what a sealed modifier can roll at this item level.
     */
    private static Component display(VaultGearTierConfig.ModifierTierGroup group,
                                     List<VaultGearTierConfig.ModifierTier<?>> tiers,
                                     VaultGearModifier.AffixType type, ItemStack gear, VaultGearData data) {
        try {
            Random random = new Random(DISPLAY_SEED);
            VaultGearModifier<?> sample = new VaultGearTierConfig.ModifierOutcome<>(tiers.get(0), group)
                    .makeModifier(random);
            sample.addCategory(VaultGearModifier.AffixCategory.CORRUPTED);
            Optional<MutableComponent> rendered = sample.getDisplay(data, type, gear, true);
            if (rendered.isPresent()) {
                return rendered.get();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // The Vault's display helpers are @OnlyIn(CLIENT) and get stripped on a dedicated
            // server (NoSuchMethodError is a LinkageError). Degrade to the identifier instead
            // of breaking the option list.
        }
        return new TextComponent(group.getIdentifier().toString()).withStyle(ChatFormatting.GRAY);
    }

    /** Affixes of the gear that can be replaced, in prefix-then-suffix order. */
    public static List<Target> targets(ItemStack gear, VaultGearData data) {
        List<Target> result = new ArrayList<>();
        for (VaultGearModifier.AffixType type : new VaultGearModifier.AffixType[]{
                VaultGearModifier.AffixType.PREFIX, VaultGearModifier.AffixType.SUFFIX}) {
            for (VaultGearModifier<?> modifier : data.getModifiers(type)) {
                if (!modifier.canBeModified()) {
                    continue;
                }
                Component display;
                try {
                    display = modifier.getDisplay(data, type, gear, false)
                            .<Component>map(component -> component)
                            .orElseGet(() -> new TextComponent(String.valueOf(modifier.getModifierIdentifier())));
                } catch (RuntimeException | LinkageError exception) {
                    display = new TextComponent(String.valueOf(modifier.getModifierIdentifier()));
                }
                result.add(new Target(type, modifier, display));
            }
        }
        return result;
    }

    public static boolean canSeal(GearSealerProMenu menu, Player player) {
        ItemStack gear = menu.getGearInputSlot().getItem();
        if (gear.isEmpty() || !menu.getOutputSlot().getItem().isEmpty()) {
            return false;
        }
        ItemStack focus = menu.getFocusSlot().getItem();
        if (focus.isEmpty() || !focus.is(ModItems.VORPAL_FOCUS)) {
            return false;
        }
        VaultGearData data = VaultGearData.read(gear);
        if (!data.isModifiable() || hasUnfilledAffixSlots(data)) {
            return false;
        }
        if (gear.getItem() instanceof IVaultUsesItem) {
            return false;
        }
        if (!VaultGearCraftingHelper.reducePotential(gear.copy(), player, ModGearModifications.CORRUPT_GEAR)) {
            return false;
        }
        VaultSealerCost cost = VaultSealerCost.of(data);
        if (player.isCreative()) {
            return true;
        }
        if (focus.getCount() < cost.vorpalFocus()) {
            return false;
        }
        if (menu.getPainiteSlot().getItem().getCount() < cost.painite()) {
            return false;
        }
        if (menu.getGoldSlot().getItem().getCount() < cost.vaultGold()
                && !CoinDefinition.hasEnoughCurrency(InventoryUtil.findAllItems(player),
                new ItemStack(ModBlocks.VAULT_GOLD, cost.vaultGold()))) {
            return false;
        }
        return !options(gear).isEmpty();
    }

    public static MsgSyncOptions buildSync(GearSealerProMenu menu) {
        MsgSyncOptions msg = new MsgSyncOptions();
        ItemStack gear = menu.getGearInputSlot().getItem();
        if (gear.isEmpty()) {
            msg.hasGear = false;
            return msg;
        }
        msg.hasGear = true;
        VaultGearData data = VaultGearData.read(gear);
        msg.modifiable = data.isModifiable();
        VaultSealerCost cost = VaultSealerCost.of(data);
        msg.costGold = cost.vaultGold();
        msg.costPainite = cost.painite();
        msg.costFocus = cost.vorpalFocus();
        msg.haveGold = menu.getGoldSlot().getItem().getCount();
        msg.havePainite = menu.getPainiteSlot().getItem().getCount();
        msg.haveFocus = menu.getFocusSlot().getItem().getCount();
        for (Option option : options(gear)) {
            byte sides = (byte) ((option.prefixAllowed() ? MsgSyncOptions.SIDE_PREFIX : 0)
                    | (option.suffixAllowed() ? MsgSyncOptions.SIDE_SUFFIX : 0));
            msg.options.add(new MsgSyncOptions.OptionEntry(option.id(), sides, option.display()));
        }
        for (Target target : targets(gear, data)) {
            msg.targets.add(new MsgSyncOptions.TargetEntry(
                    target.type() == VaultGearModifier.AffixType.PREFIX, target.display()));
        }
        msg.canSeal = canSeal(menu, menu.getPlayer());
        return msg;
    }

    /**
     * Applies one sealing operation. Returns true when the gear was sealed.
     *
     * @param targetIndex index into {@link #targets(ItemStack, VaultGearData)}, or -1 to add a new affix
     */
    public static boolean seal(GearSealerProMenu menu, ServerPlayer player, ResourceLocation optionId,
                               VaultGearModifier.AffixType affixType, int targetIndex) {
        ItemStack gear = menu.getGearInputSlot().getItem();
        if (gear.isEmpty() || !menu.getOutputSlot().getItem().isEmpty()) {
            return false;
        }
        VaultGearData data = VaultGearData.read(gear);
        if (!data.isModifiable() || hasUnfilledAffixSlots(data) || gear.getItem() instanceof IVaultUsesItem) {
            return false;
        }

        VaultSealerCost cost = VaultSealerCost.of(data);
        ItemStack focus = menu.getFocusSlot().getItem();
        if (focus.isEmpty() || !focus.is(ModItems.VORPAL_FOCUS) || focus.getCount() < cost.vorpalFocus()) {
            return false;
        }
        if (!player.isCreative() && !hasMaterials(menu, player, cost)) {
            return false;
        }

        Option option = options(gear).stream()
                .filter(candidate -> candidate.id().equals(optionId) && candidate.allows(affixType))
                .findFirst().orElse(null);
        if (option == null) {
            return false;
        }

        ItemStack output = gear.copy();
        VaultGearData outData = VaultGearData.read(output);

        // The target must be resolved against the copied data: VaultGearData#removeModifier
        // compares modifier instances by identity, not by value.
        VaultGearModifier<?> replaced = null;
        if (targetIndex >= 0) {
            List<Target> targets = targets(gear, outData);
            if (targetIndex >= targets.size()) {
                return false;
            }
            Target target = targets.get(targetIndex);
            if (target.type() != affixType) {
                return false;
            }
            replaced = target.modifier();
        }

        VaultGearModifier<?> sealedModifier = option.roll(GearRollHelper.rand);
        sealedModifier.addCategory(VaultGearModifier.AffixCategory.CORRUPTED);

        if (replaced != null) {
            if (!outData.removeModifier(replaced)) {
                return false;
            }
        } else if (affixType == VaultGearModifier.AffixType.PREFIX) {
            outData.createOrReplaceAttributeValue(ModGearAttributes.PREFIXES,
                    outData.<Integer>getFirstValue(ModGearAttributes.PREFIXES).orElse(0) + 1);
        } else if (affixType == VaultGearModifier.AffixType.SUFFIX) {
            outData.createOrReplaceAttributeValue(ModGearAttributes.SUFFIXES,
                    outData.<Integer>getFirstValue(ModGearAttributes.SUFFIXES).orElse(0) + 1);
        } else {
            return false;
        }

        if (!outData.addModifierFirst(affixType, sealedModifier)) {
            return false;
        }
        if (outData.hasAttribute(ModGearAttributes.LIVING)) {
            outData.removeAttribute(ModGearAttributes.LIVING);
        }
        outData.write(output);

        if (!VaultGearModifierHelper.setGearCorrupted(output).success()) {
            return false;
        }
        VaultGearCraftingHelper.reducePotential(output, player, ModGearModifications.CORRUPT_GEAR);

        if (!player.isCreative()) {
            consume(menu, player, cost);
        }

        menu.getGearInputSlot().set(ItemStack.EMPTY);
        menu.getOutputSlot().set(output);
        menu.broadcastChanges();
        player.level.playSound(null, menu.getTilePos(), ModSounds.GEAR_SEAL, SoundSource.BLOCKS, 0.6F, 1.0F);
        return true;
    }

    private static boolean hasMaterials(GearSealerProMenu menu, ServerPlayer player, VaultSealerCost cost) {
        if (menu.getPainiteSlot().getItem().getCount() < cost.painite()) {
            return false;
        }
        if (menu.getGoldSlot().getItem().getCount() < cost.vaultGold()) {
            Optional<CoinDefinition> coin = CoinDefinition.getCoinDefinition(ModBlocks.VAULT_GOLD);
            if (coin.isEmpty()) {
                return false;
            }
            int remaining = cost.vaultGold() - menu.getGoldSlot().getItem().getCount();
            if (!CoinDefinition.hasEnoughCurrency(InventoryUtil.findAllItems(player),
                    new ItemStack(ModBlocks.VAULT_GOLD, remaining))) {
                return false;
            }
        }
        return true;
    }

    private static void consume(GearSealerProMenu menu, ServerPlayer player, VaultSealerCost cost) {
        ItemStack focus = menu.getFocusSlot().getItem();
        focus.shrink(cost.vorpalFocus());
        menu.getFocusSlot().setChanged();

        ItemStack gold = menu.getGoldSlot().getItem();
        if (gold.getCount() >= cost.vaultGold()) {
            gold.shrink(cost.vaultGold());
        } else {
            int remaining = cost.vaultGold() - gold.getCount();
            gold.setCount(0);
            CoinDefinition.getCoinDefinition(ModBlocks.VAULT_GOLD).ifPresent(coin ->
                    CoinDefinition.deductCoins(InventoryUtil.findAllItems(player),
                            remaining * coin.coinValue, coin));
        }
        menu.getGoldSlot().setChanged();

        if (cost.painite() > 0) {
            menu.getPainiteSlot().getItem().shrink(cost.painite());
            menu.getPainiteSlot().setChanged();
        }
    }
}
