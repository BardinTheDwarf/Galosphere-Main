package net.orcinus.galosphere.events;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.orcinus.galosphere.Galosphere;
import net.orcinus.galosphere.api.IBanner;
import net.orcinus.galosphere.blocks.WarpedAnchorBlock;
import net.orcinus.galosphere.entities.SparkleEntity;
import net.orcinus.galosphere.init.GBlocks;
import net.orcinus.galosphere.init.GEntityTypes;
import net.orcinus.galosphere.init.GItems;
import net.orcinus.galosphere.items.SterlingArmorItem;
import net.orcinus.galosphere.util.BannerRendererUtil;

import java.util.List;

@Mod.EventBusSubscriber(modid = Galosphere.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobEvents {

    @SubscribeEvent
    public static void registerEntityAttribute(EntityAttributeCreationEvent event) {
        SpawnPlacements.register(GEntityTypes.SPARKLE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SparkleEntity::checkSparkleSpawnRules);

        event.put(GEntityTypes.SPARKLE.get(), SparkleEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onBreakSpeedChanged(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        if (state.getBlock() == Blocks.BUDDING_AMETHYST) {
            event.setNewSpeed(2.0F);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntityLiving();
        if (livingEntity instanceof Horse horse) {
            if (!((IBanner)horse).getBanner().isEmpty() && horse.getArmor().is(GItems.STERLING_HORSE_ARMOR.get())) {
                ItemStack copy = ((IBanner) horse).getBanner();
                horse.spawnAtLocation(copy);
                ((IBanner) horse).setBanner(ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        boolean flag = event.getSource().isExplosion();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (flag) {
                float reductionAmount = 0.0F;
                Item item = entity.getItemBySlot(slot).getItem();
                if (entity instanceof Horse horse) {
                    Item horseItem = horse.getArmor().getItem();
                    if (horseItem == GItems.STERLING_HORSE_ARMOR.get()) {
                        float damageReduction = 3.0F;
                        reductionAmount = event.getAmount() - damageReduction;
                    }
                }
                if (item instanceof SterlingArmorItem sterlingArmorItem) {
                    float damageReduction = sterlingArmorItem.getExplosionResistance(slot);
                    reductionAmount = event.getAmount() - damageReduction;
                }
                if (item instanceof SterlingArmorItem || (entity instanceof Horse && ((Horse)entity).getArmor().getItem() == GItems.STERLING_HORSE_ARMOR.get())){
                    event.setAmount(reductionAmount);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getPlayer();
        InteractionHand hand = event.getHand();
        Entity target = event.getTarget();
        BannerRendererUtil util = new BannerRendererUtil();
        if (target instanceof Horse horse) {
            if (horse.getArmor().is(GItems.STERLING_HORSE_ARMOR.get())) {
                if (((IBanner) horse).getBanner().isEmpty()) {
                    if (util.isTapestryStack(stack) || stack.getItem() instanceof BannerItem) {
                        if (!horse.level.isClientSide()) {
                            event.setCanceled(true);
                            ItemStack copy = stack.copy();
                            if (!player.getAbilities().instabuild) {
                                stack.shrink(1);
                            }
                            copy.setCount(1);
                            horse.level.playSound(null, horse, SoundEvents.HORSE_ARMOR, SoundSource.PLAYERS, 1.0F, 1.0F);
                            ((IBanner) horse).setBanner(copy);
                            player.swing(hand);
                        }
                    }
                } else {
                    if (player.isShiftKeyDown() && stack.isEmpty()) {
                        if (!horse.level.isClientSide()) {
                            event.setCanceled(true);
                            ItemStack copy = ((IBanner) horse).getBanner();
                            player.setItemInHand(hand, copy);
                            horse.level.playSound(null, horse, SoundEvents.HORSE_ARMOR, SoundSource.PLAYERS, 1.0F, 1.0F);
                            ((IBanner) horse).setBanner(ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity instanceof IBanner bannerEntity) {
            if (!bannerEntity.getBanner().isEmpty()) {
                if (entity instanceof Horse horse) {
                    if (!((IBanner)horse).getBanner().isEmpty() && !horse.getArmor().is(GItems.STERLING_HORSE_ARMOR.get())) {
                        ItemStack copy = ((IBanner) horse).getBanner();
                        horse.spawnAtLocation(copy);
                        ((IBanner) horse).setBanner(ItemStack.EMPTY);
                    }
                } else {
                    if (!entity.getItemBySlot(EquipmentSlot.HEAD).is(GItems.STERLING_HELMET.get())) {
                        ItemStack copy = bannerEntity.getBanner();
                        entity.spawnAtLocation(copy);
                        bannerEntity.setBanner(ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onTeleportEvent(EntityTeleportEvent.EnderPearl event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            BlockPos pos = new BlockPos(event.getTarget());
            List<BlockPos> possibles = Lists.newArrayList();
            Level world = entity.level;
            int radius = 8;
            int height = 6;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -height; y <= height; y++) {
                        BlockPos checkPos = new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                        BlockState state = world.getBlockState(checkPos);
                        if (state.getBlock() == GBlocks.WARPED_ANCHOR.get()) {
                            if (state.getValue(WarpedAnchorBlock.WARPED_CHARGE) > 0) {
                                possibles.add(checkPos);
                            }
                        }
                    }
                }
            }
            if (!possibles.isEmpty()) {
                BlockPos lockPosition = possibles.get(((Player) entity).getRandom().nextInt(possibles.size()));
                BlockState state = world.getBlockState(lockPosition);
                if (state.getBlock() == GBlocks.WARPED_ANCHOR.get()) {
                    world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                    player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                    event.setTargetX(lockPosition.getX() + 0.5D);
                    event.setTargetY(lockPosition.getY() + 0.5D);
                    event.setTargetZ(lockPosition.getZ() + 0.5D);
                    world.playSound(null, lockPosition, SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    world.setBlock(lockPosition, state.setValue(WarpedAnchorBlock.WARPED_CHARGE, state.getValue(WarpedAnchorBlock.WARPED_CHARGE) - 1), 2);
                }
            }
        }
    }

}
