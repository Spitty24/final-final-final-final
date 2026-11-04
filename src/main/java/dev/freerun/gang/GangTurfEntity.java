package dev.freerun.gang;

import dev.freerun.mobs.MoreMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * An invisible marker in the middle of the factory parking lot that runs the gang activity:
 * keeps both crews topped up (new members wander in from their end of the lot when nobody's
 * watching), has them trash-talk each other, and every few minutes starts a shootout.
 */
public class GangTurfEntity extends Entity {
    private static final int RADIUS = 26;
    private static final int CREW_SIZE = 3;
    private static final int SHOOTOUT_TICKS = 400;

    private BlockPos homeA = BlockPos.ZERO;
    private BlockPos homeB = BlockPos.ZERO;
    private int nextShootout = 1200;
    private int shootoutLeft;
    private int respawnA = 200, respawnB = 200;
    private int tauntTimer = 300;

    public GangTurfEntity(EntityType<? extends GangTurfEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public void setHomes(BlockPos a, BlockPos b) {
        homeA = a.immutable();
        homeB = b.immutable();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || tickCount % 10 != 0) return;
        if (homeA.equals(BlockPos.ZERO)) {   // summoned by hand: crews on either side of it
            setHomes(blockPosition().offset(-12, 0, 0), blockPosition().offset(12, 0, 0));
        }
        long now = server.getGameTime();
        List<GangsterEntity> all = server.getEntitiesOfClass(GangsterEntity.class, new AABB(blockPosition()).inflate(RADIUS));
        long rats = all.stream().filter(g -> g.crew() == Crew.RUST_RATS).count();
        long kings = all.size() - rats;

        // Reinforcements, only when no player is close enough to see them appear.
        respawnA -= 10;
        respawnB -= 10;
        if (rats < CREW_SIZE && respawnA <= 0 && !watched(homeA)) {
            spawnMember(server, Crew.RUST_RATS, homeA);
            respawnA = 600 + random.nextInt(600);
        }
        if (kings < CREW_SIZE && respawnB <= 0 && !watched(homeB)) {
            spawnMember(server, Crew.CHROME_KINGS, homeB);
            respawnB = 600 + random.nextInt(600);
        }

        if (shootoutLeft > 0) {
            shootoutLeft -= 10;
            for (GangsterEntity g : all) g.goToWar(now);
            if (shootoutLeft <= 0 || rats == 0 || kings == 0) {
                shootoutLeft = 0;
                announce(server, "§7The shooting in the parking lot stops. For now.");
            }
            return;
        }

        nextShootout -= 10;
        if (nextShootout <= 0) {
            nextShootout = 2400 + random.nextInt(3600);   // every 2 to 5 minutes
            if (rats > 0 && kings > 0) {
                shootoutLeft = SHOOTOUT_TICKS;
                announce(server, "§4§l*BANG* *BANG* §r§cShots fired in the factory parking lot!");
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 3.0f, 0.6f);
                for (GangsterEntity g : all) g.goToWar(now);
            }
            return;
        }

        if ((tauntTimer -= 10) <= 0 && !all.isEmpty()) {
            tauntTimer = 400 + random.nextInt(600);
            all.get(random.nextInt(all.size())).taunt();
        }
    }

    private boolean watched(BlockPos pos) {
        Player p = level().getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 10.0, false);
        return p != null;
    }

    private void spawnMember(ServerLevel server, Crew crew, BlockPos home) {
        GangsterEntity g = new GangsterEntity(MoreMobs.GANGSTER, server);
        BlockPos at = home.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
        g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
        if (!server.noCollision(g)) return;
        g.setup(crew, random.nextInt(2), GangsterEntity.randomGun(random.nextFloat()), home);
        server.addFreshEntity(g);
    }

    private void announce(ServerLevel server, String text) {
        Component msg = Component.literal(text);
        for (Player p : server.players()) {
            if (p.distanceToSqr(this) < 72 * 72) p.displayClientMessage(msg, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        homeA = new BlockPos(tag.getInt("AX"), tag.getInt("AY"), tag.getInt("AZ"));
        homeB = new BlockPos(tag.getInt("BX"), tag.getInt("BY"), tag.getInt("BZ"));
        nextShootout = tag.contains("Next") ? tag.getInt("Next") : 1200;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("AX", homeA.getX());
        tag.putInt("AY", homeA.getY());
        tag.putInt("AZ", homeA.getZ());
        tag.putInt("BX", homeB.getX());
        tag.putInt("BY", homeB.getY());
        tag.putInt("BZ", homeB.getZ());
        tag.putInt("Next", nextShootout);
    }
}
