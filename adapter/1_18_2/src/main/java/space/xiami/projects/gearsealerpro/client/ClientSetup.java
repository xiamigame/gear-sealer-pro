package space.xiami.projects.gearsealerpro.client;

import space.xiami.projects.gearsealerpro.GearSealerProMod;
import space.xiami.projects.gearsealerpro.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GearSealerProMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.VAULT_SEALER_PRO.get(), GearSealerProScreen::new));
    }

    private ClientSetup() {
    }
}
