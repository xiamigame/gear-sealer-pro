package space.xiami.projects.gearsealerpro.registry;

import space.xiami.projects.gearsealerpro.GearSealerProMod;
import space.xiami.projects.gearsealerpro.block.GearSealerProBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, GearSealerProMod.MOD_ID);

    public static final RegistryObject<GearSealerProBlock> VAULT_SEALER_PRO =
            BLOCKS.register("vault_sealer_pro", GearSealerProBlock::new);

    private ModBlocks() {
    }
}
