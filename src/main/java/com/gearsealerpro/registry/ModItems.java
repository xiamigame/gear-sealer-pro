package com.gearsealerpro.registry;

import com.gearsealerpro.GearSealerProMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GearSealerProMod.MOD_ID);

    public static final RegistryObject<BlockItem> VAULT_SEALER_PRO = ITEMS.register("vault_sealer_pro",
            () -> new BlockItem(ModBlocks.VAULT_SEALER_PRO.get(),
                    new Item.Properties()
                            .tab(CreativeModeTab.TAB_DECORATIONS)
                            .rarity(Rarity.RARE)
                            .stacksTo(1)));

    private ModItems() {
    }
}
