package com.gearsealerpro;

import com.gearsealerpro.network.NetworkHandler;
import com.gearsealerpro.registry.ModBlockEntities;
import com.gearsealerpro.registry.ModBlocks;
import com.gearsealerpro.registry.ModItems;
import com.gearsealerpro.registry.ModMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gear Sealer Pro: an upgraded Gear Sealer.
 *
 * <p>Sealing costs are identical to the vanilla Gear Sealer (same {@code VaultSealerCost}),
 * but instead of rolling a random sealed modifier on a random affix, the player picks both
 * the sealed modifier and the affix it replaces (or picks to add an extra affix).</p>
 */
@Mod(GearSealerProMod.MOD_ID)
public final class GearSealerProMod {

    public static final String MOD_ID = "gearsealerpro";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GearSealerProMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModMenus.MENUS.register(bus);
        bus.addListener(GearSealerProMod::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::init);
    }
}
