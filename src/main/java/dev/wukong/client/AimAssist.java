package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.InteractionHand;
import dev.wukong.client.combat.BowMotion;
import dev.wukong.client.combat.BowMotion.V;
import dev.wukong.client.combat.VanillaBowModel;

/** Client-side target selection and optional bow release, no custom packets. */
public final class AimAssist {
    private static final dev.wukong.client.combat.BowReleaseCycle RELEASE = new dev.wukong.client.combat.BowReleaseCycle();
    private static int focusEntityId = Integer.MIN_VALUE;
    private static UUID lockedTargetUuid;
    private static final Map<UUID, BowMotion.Tracker> MOTION = new HashMap<>();
    private static BowMotion.Tracker ownMotion = new BowMotion.Tracker();
    private static Object trackedLevel, trackedPlayer;
    private static int sampleCounter, previousTick = Integer.MIN_VALUE;

    private static void resetMotion(Minecraft mc) {
        MOTION.clear(); ownMotion = new BowMotion.Tracker();
        trackedLevel = mc.level; trackedPlayer = mc.player;
        previousTick = Integer.MIN_VALUE; sampleCounter = 0;
        lockedTargetUuid = null; focusEntityId = Integer.MIN_VALUE; RELEASE.reset();
        BowDiagnostics.clear();
    }
    private static V vec(Vec3 v) { return new V(v.x,v.y,v.z); }
    private static BowMotion.Mode motionMode(LivingEntity e) {
        if (e.isFallFlying()) return BowMotion.Mode.ELYTRA;
        if (e.isInWater()) return BowMotion.Mode.WATER;
        return e.onGround() ? BowMotion.Mode.GROUND : BowMotion.Mode.AIR;
    }
    private static void updateMotion(Minecraft mc, boolean enabled) {
        if (mc.level != trackedLevel || mc.player != trackedPlayer) resetMotion(mc);
        if (mc.level == null || mc.player == null) return;
        int tick = mc.player.tickCount;
        if (previousTick != Integer.MIN_VALUE && (tick < previousTick || (long)tick-previousTick > 5)) resetMotion(mc);
        previousTick = tick;
        if (!enabled) { MOTION.clear(); ownMotion = new BowMotion.Tracker(); return; }
        ownMotion.update(vec(mc.player.position()),tick,motionMode(mc.player));
        Set<UUID> alive = (++sampleCounter % 40 == 0) ? new HashSet<>() : null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) continue;
            UUID id=living.getUUID(); if (alive != null) alive.add(id);
            MOTION.computeIfAbsent(id,key->new BowMotion.Tracker()).update(vec(living.position()),tick,motionMode(living));
        }
        if (alive != null) MOTION.keySet().retainAll(alive);
    }
    private static BowMotion.Sample sample(LivingEntity e) {
        BowMotion.Tracker tracked = MOTION.get(e.getUUID());
        return tracked==null ? BowMotion.Sample.empty(motionMode(e)) : tracked.sample();
    }

    public static void tick(Minecraft mc, boolean aimActive) {
        BenchConfig cfg = BenchConfig.INSTANCE;
        if (mc.player == null || mc.level == null) { BowViewSplit.clear(); resetMotion(mc); return; }
        BowDiagnostics.clear();
        updateMotion(mc, cfg.aimAssist && cfg.aimPrediction);
        boolean bowDrawn = mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof BowItem;
        RELEASE.observe(mc.player.tickCount, bowDrawn, bowDrawn ? mc.player.getTicksUsingItem() : 0);
        if (!cfg.aimAssist || !aimActive) { BowViewSplit.clear();
            focusEntityId = Integer.MIN_VALUE;
            lockedTargetUuid = null;
            return;
        }
        if (!cfg.aimTillDeath) lockedTargetUuid = null;
        String mode = cfg.aimType == null ? "STANDARD" : cfg.aimType;

        int chargeTicks = bowDrawn ? mc.player.getTicksUsingItem() : 0;
        // Always model the actual charge. Steady view still smooths camera rotation below.
        double arrowSpeed = bowDrawn ? 3.0 * BowItem.getPowerForTime(chargeTicks) : 0;
        boolean prediction = cfg.aimPrediction && bowDrawn;
        V inherited = prediction ? VanillaBowModel.inherited(vec(mc.player.getKnownMovement()),mc.player.onGround()) : V.ZERO;
        int ping=-1;
        if (mc.hasSingleplayerServer()) ping=0;
        else if (mc.getConnection()!=null) {
            var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info!=null && info.getLatency()>0) ping=info.getLatency();
        }
        // Optional heuristic only. The displayed remote position may already be interpolated.
        double observationAge=cfg.bowPredictionLatency && ping>0?Math.min(3.0,ping/100.0):0.0;
        VanillaBowModel.Result chosenPrediction=null, lockedPrediction=null;
        BowMotion.Sample chosenMotion=null, lockedMotion=null;
        double bestScore = Double.POSITIVE_INFINITY;
        LivingEntity selected = null;
        LivingEntity lockedCandidate = null;
        double chosenYawDiff = 0, chosenPitchDiff = 0;
        double lockedYawDiff = 0, lockedPitchDiff = 0;
        double effectiveRange = "MELEE".equals(mode) ? Math.min(5.0, cfg.aimRange) : cfg.aimRange;
        double radiusSquared = effectiveRange * effectiveRange;
        double effectiveFov = cfg.aimFov >= 360.0 ? Double.POSITIVE_INFINITY : ("PRECISION".equals(mode) ? cfg.aimFov * 0.6 : cfg.aimFov);
        Vec3 shooter = mc.player.getEyePosition();
        V launchOrigin=vec(shooter.add(0,-0.1,0)); // Approximate vanilla arrow spawn height.

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity target) || target == mc.player || !target.isAlive()) continue;
            if (target instanceof Player) { if (!cfg.aimPlayers) continue; }
            else if (target instanceof Mob && target instanceof Enemy) { if (!cfg.aimHostile) continue; }
            else if (target instanceof Mob) { if (!cfg.aimPassive) continue; }
            else continue;
            double distanceSq = mc.player.distanceToSqr(target);
            if (distanceSq > radiusSquared || distanceSq < 0.01) continue;
            // Select a real clear point on the target's hitbox before predicting its motion.
            // Unlike hasLineOfSight(entity), ALL_VISIBLE also accepts a partly exposed edge.
            Vec3 aimPoint = AimSpot.select(mc, target, shooter, cfg.aimSpot, cfg.aimRequireVisible);
            if (aimPoint == null) continue;
            BowMotion.Sample targetMotion = prediction?sample(target):null;
            VanillaBowModel.Result solution=null;
            if (prediction && arrowSpeed>.15 && !mc.player.isInWater() && targetMotion.mode()!=BowMotion.Mode.WATER) {
                solution=VanillaBowModel.solve(launchOrigin,vec(aimPoint),targetMotion,inherited,arrowSpeed,observationAge);
            }
            Vec3 difference = solution==null?aimPoint.subtract(shooter):
                    new Vec3(solution.direction().x(),solution.direction().y(),solution.direction().z());
            double horizontal = Math.hypot(difference.x, difference.z);
            if (horizontal < 0.001) continue;
            double yaw = Math.toDegrees(Math.atan2(-difference.x, difference.z));
            double pitch = -Math.toDegrees(Math.atan2(difference.y, horizontal));
            double yawDiff = Mth.wrapDegrees((float)(yaw - mc.player.getYRot()));
            double pitchDiff = pitch - mc.player.getXRot();
            double angle = Math.hypot(yawDiff, pitchDiff);
            boolean isLocked = cfg.aimTillDeath && lockedTargetUuid != null
                    && lockedTargetUuid.equals(target.getUUID());
            // A valid lock may remain outside the acquisition FOV.
            if (angle > effectiveFov && !isLocked) continue;
            if (isLocked) {
                lockedCandidate = target;
                lockedYawDiff = yawDiff;
                lockedPitchDiff = pitchDiff;
                lockedPrediction=solution; lockedMotion=targetMotion;
            }
            double score = TargetRules.score(cfg.aimTargetPriority,
                    Math.sqrt(distanceSq), target.getHealth(), target.getMaxHealth(),
                    angle, effectiveRange);
            if ("MELEE".equals(mode)) score += Math.sqrt(distanceSq) * 0.10;
            if ("FOCUS".equals(mode) && target.getId() == focusEntityId) score -= 1000.0;
            if (score < bestScore) {
                bestScore = score;
                selected = target;
                chosenYawDiff = yawDiff;
                chosenPitchDiff = pitchDiff;
                chosenPrediction=solution; chosenMotion=targetMotion;
            }
        }
        if (lockedCandidate != null) {
            selected = lockedCandidate;
            chosenYawDiff = lockedYawDiff;
            chosenPitchDiff = lockedPitchDiff;
            chosenPrediction=lockedPrediction; chosenMotion=lockedMotion;
        }
        if (selected == null) { BowViewSplit.clear();
            if(prediction) BowDiagnostics.publish(mc,ownMotion.sample(),null,inherited,null,ping,observationAge,"NO_TARGET");
            focusEntityId = Integer.MIN_VALUE;
            lockedTargetUuid = null;
            return;
        }
        if (cfg.aimTillDeath) lockedTargetUuid = selected.getUUID();
        focusEntityId = "FOCUS".equals(mode) ? selected.getId() : Integer.MIN_VALUE;
        if (aimActive) {
            double strength = dev.wukong.client.combat.AimResponse.fromId(cfg.aimResponseProfile)
                    .strength(cfg.aimAggression, "PRECISION".equals(mode));
            double yawStep = chosenYawDiff * strength;
            double pitchStep = chosenPitchDiff * strength;
            if (cfg.steadyBowView && bowDrawn && cfg.aimPrediction) {
                yawStep = Mth.clamp(yawStep, -4.0, 4.0);
                pitchStep = Mth.clamp(pitchStep, -2.0, 2.0);
            }
            mc.player.setYRot(mc.player.getYRot() + (float) yawStep);
            mc.player.setXRot(Mth.clamp(mc.player.getXRot() + (float) pitchStep, -90f, 90f));
        }
        BowViewSplit.trackIfEnabled(mc, selected);
        if(prediction) BowDiagnostics.publish(mc,ownMotion.sample(),chosenMotion,inherited,chosenPrediction,ping,observationAge,
                cfg.aimTriggerBow && !RELEASE.ready()?"RELEASE_WAIT_NEW_DRAW":cfg.aimTriggerBow && chargeTicks < cfg.aimBowMinChargeTicks?"RELEASE_WAIT_CHARGE":cfg.aimTriggerBow && chosenPrediction!=null && Math.hypot(chosenYawDiff,chosenPitchDiff)>1.75?"RELEASE_WAIT_ALIGNMENT":chosenPrediction!=null?"MODEL_OK":arrowSpeed<=.15?"LOW_CHARGE":chosenMotion==null||!chosenMotion.valid()?"WARMUP":"NO_MODEL_SOLUTION");
        // Optional vanilla melee attack while an acquired target stays within normal reach.
        // Does not bypass server validation, cooldown, range, or line of sight.
        if (cfg.aimHitTillDeath && mc.gameMode != null && !mc.player.isUsingItem()
                && !mc.player.isSpectator() && mc.player.hasLineOfSight(selected)
                && mc.player.distanceToSqr(selected) <= 9.0
                && mc.player.getAttackStrengthScale(0.0f) >= 0.95f) {
            // Check alignment AFTER smoothing, not using the pre-rotation angle.
            Vec3 center = selected.position().add(0, selected.getBbHeight() * 0.60, 0);
            Vec3 delta = center.subtract(mc.player.getEyePosition());
            double desiredYaw = Math.toDegrees(Math.atan2(-delta.x, delta.z));
            double desiredPitch = -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
            double remaining = Math.hypot(Mth.wrapDegrees((float)(desiredYaw - mc.player.getYRot())),
                    desiredPitch - mc.player.getXRot());
            if (remaining <= 5.0) {
                mc.gameMode.attack(mc.player, selected);
                mc.player.swing(InteractionHand.MAIN_HAND,
                        mc.player.getMainHandItem().getAttackAnimation(), false);
            }
        }
        // Only release an already-drawn bow. No automatic charging or attack packets.
        // A tighter angle prevents premature shots while the camera is still moving.
        if (cfg.aimTriggerBow && bowDrawn && RELEASE.ready()
                && chargeTicks >= cfg.aimBowMinChargeTicks
                && (!prediction || chosenPrediction != null)
                && mc.gameMode != null
                && Math.hypot(chosenYawDiff, chosenPitchDiff) <= 1.75) {
            mc.gameMode.releaseUsingItem(mc.player);
            RELEASE.markReleased();
        }
    }

    private AimAssist() {}
}
