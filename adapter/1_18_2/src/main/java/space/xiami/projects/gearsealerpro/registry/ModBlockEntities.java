package space.xiami.projects.gearsealerpro.registry;

import space.xiami.projects.gearsealerpro.GearSealerProMod;
import space.xiami.projects.gearsealerpro.block.GearSealerProBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, GearSealerProMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<GearSealerProBlockEntity>> VAULT_SEALER_PRO =
            BLOCK_ENTITIES.register("vault_sealer_pro",
                    () -> BlockEntityType.Builder
                            .of(GearSealerProBlockEntity::new, ModBlocks.VAULT_SEALER_PRO.get())
                            .build(null));

    private ModBlockEntities() {
    }
}
