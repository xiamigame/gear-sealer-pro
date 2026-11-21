package com.gearsealerpro.registry;

import com.gearsealerpro.GearSealerProMod;
import com.gearsealerpro.menu.GearSealerProMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, GearSealerProMod.MOD_ID);

    public static final RegistryObject<MenuType<GearSealerProMenu>> VAULT_SEALER_PRO =
            MENUS.register("vault_sealer_pro", () -> IForgeMenuType.create(
                    (windowId, inventory, data) -> new GearSealerProMenu(
                            windowId, inventory.player.level, data.readBlockPos(), inventory)));

    private ModMenus() {
    }
}
