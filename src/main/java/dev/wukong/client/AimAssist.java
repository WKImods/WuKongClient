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

/** Client-side target selection and optional bow release, no custom packets. */
public final class AimAssist {
    private static boolean firedThisDraw;
    private static int focusEntityId = Integer.MIN_VALUE;
    private static UUID lockedTargetUuid;
    // Observe rendered mobs each client tick. Networked mobs may expose stale delta movement.
    private static final Map<Integer, TrackedMotion> MOTION = new HashMap<>();
    private static Object trackedLevel;
    private static int sampleCounter;

    private static final class TrackedMotion {
        private Vec3 position;
        private Vec3 smoothedVelocity = Vec3.ZERO;
        private int tick;
        private int observations;

        private TrackedMotion(Vec3 position, int tick) {
            this.position = position;
            this.tick = tick;
        }
    }

    private static void updateMotion(Minecraft mc) {
        if (mc.level != trackedLevel) {
            MOTION.clear();
            trackedLevel = mc.level;
            sampleCounter = 0;
            lockedTargetUuid = null;
            focusEntityId = Integer.MIN_VALUE;
        }
        if (mc.level == null || mc.player == null) return;
        int tick = mc.player.tickCount;
        Set<Integer> alive = (++sampleCounter % 40 == 0) ? new HashSet<>() : null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity) || entity == mc.player) continue;
            if (alive != null) alive.add(entity.getId());
            Vec3 position = entity.position();
            TrackedMotion state = MOTION.get(entity.getId());
            if (state == null || tick < state.tick) {
                MOTION.put(entity.getId(), new TrackedMotion(position, tick));
                continue;
            }
            if (tick == state.tick) continue;
            int deltaTicks = tick - state.tick;
            Vec3 measured = position.subtract(state.position).scale(1.0 / deltaTicks);
            // Treat teleports, long pauses, and large correction packets as non-predictable.
            if (deltaTicks > 5 || measured.lengthSqr() > 1.44) {
                state.smoothedVelocity = Vec3.ZERO;
                state.observations = 0;
            } else {
                state.smoothedVelocity = state.observations == 0 ? measured
                        : state.smoothedVelocity.scale(0.65).add(measured.scale(0.35));
                state.observations++;
            }
            state.position = position;
            state.tick = tick;
        }
        if (alive != null) MOTION.keySet().retainAll(alive);
    }

    private static Vec3 measuredVelocity(LivingEntity target) {
        TrackedMotion motion = MOTION.get(target.getId());
        return motion == null || motion.observations == 0 ? Vec3.ZERO : motion.smoothedVelocity;
    }


    public static void tick(Minecraft mc, boolean aimActive) {
        BenchConfig cfg = BenchConfig.INSTANCE;
        if (mc.player == null || mc.level == null) { BowViewSplit.clear(); firedThisDraw = false; MOTION.clear(); trackedLevel = null; lockedTargetUuid = null; focusEntityId = Integer.MIN_VALUE; return; }
        updateMotion(mc);
        boolean bowDrawn = mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof BowItem;
        if (!bowDrawn) firedThisDraw = false;
        if (!cfg.aimAssist || !aimActive) { BowViewSplit.clear();
            focusEntityId = Integer.MIN_VALUE;
            lockedTargetUuid = null;
            return;
        }
        if (!cfg.aimTillDeath) lockedTargetUuid = null;
        String mode = cfg.aimType == null ? "STANDARD" : cfg.aimType;

        int chargeTicks = bowDrawn ? mc.player.getTicksUsingItem() : 0;
        // Fix 11: avoid early draw-time camera jumps by referencing the configured charge.
        int modeledCharge = bowDrawn && cfg.steadyBowView
                ? Math.max(chargeTicks, Math.max(3, cfg.aimBowMinChargeTicks))
                : chargeTicks;
        double arrowSpeed = bowDrawn ? 3.0 * BowItem.getPowerForTime(modeledCharge) : 0;
        double bestScore = Double.POSITIVE_INFINITY;
        LivingEntity selected = null;
        LivingEntity lockedCandidate = null;
        double chosenYawDiff = 0, chosenPitchDiff = 0;
        double lockedYawDiff = 0, lockedPitchDiff = 0;
        double effectiveRange = "MELEE".equals(mode) ? Math.min(5.0, cfg.aimRange) : cfg.aimRange;
        double radiusSquared = effectiveRange * effectiveRange;
        double effectiveFov = "PRECISION".equals(mode) ? cfg.aimFov * 0.6 : cfg.aimFov;
        Vec3 shooter = mc.player.getEyePosition();

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
            if (cfg.aimPrediction && bowDrawn && arrowSpeed > 0.15) {
                aimPoint = predictBow(shooter, aimPoint, measuredVelocity(target), arrowSpeed);
            }
            Vec3 difference = aimPoint.subtract(shooter);
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
            }
        }
        if (lockedCandidate != null) {
            selected = lockedCandidate;
            chosenYawDiff = lockedYawDiff;
            chosenPitchDiff = lockedPitchDiff;
        }
        if (selected == null) { BowViewSplit.clear();
            focusEntityId = Integer.MIN_VALUE;
            lockedTargetUuid = null;
            return;
        }
        if (cfg.aimTillDeath) lockedTargetUuid = selected.getUUID();
        focusEntityId = "FOCUS".equals(mode) ? selected.getId() : Integer.MIN_VALUE;
        if (aimActive) {
            double strength = Math.max(0.10, Math.min(1.0, cfg.aimAggression));
            if ("PRECISION".equals(mode)) strength *= 0.55;
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
        if (cfg.aimTriggerBow && bowDrawn && !firedThisDraw
                && chargeTicks >= cfg.aimBowMinChargeTicks
                && mc.gameMode != null
                && Math.hypot(chosenYawDiff, chosenPitchDiff) <= 1.75) {
            mc.gameMode.releaseUsingItem(mc.player);
            firedThisDraw = true;
        }
    }

    /** Approximate vanilla bow lead. Models drag/gravity per tick; ignores random spread and collision. */
    private static Vec3 predictBow(Vec3 shooter, Vec3 target, Vec3 velocity, double arrowSpeed) {
        Vec3 predicted = target;
        for (int i = 0; i < 4; i++) {
            double horizontal = Math.hypot(predicted.x - shooter.x, predicted.z - shooter.z);
            double flightTicks = Math.min(100.0, horizontal / Math.max(0.15, arrowSpeed));
            // Iteratively solve travel-time with horizontal drag; aimed at ordinary low-arc shots.
            for (int j = 0; j < 3; j++) {
                double distanceFactor = (1.0 - Math.pow(0.99, flightTicks)) / 0.01;
                double nominal = Math.max(0.001, arrowSpeed * distanceFactor);
                flightTicks = Math.min(100.0, flightTicks * horizontal / nominal);
            }
            double drop = 0.0;
            double vy = 0.0;
            int whole = (int)Math.floor(flightTicks);
            for (int step = 0; step < whole; step++) {
                drop += -vy;
                vy = vy * 0.99 - 0.05;
            }
            drop += -vy * (flightTicks - whole);
            predicted = target.add(velocity.scale(flightTicks)).add(0, drop, 0);
        }
        return predicted;
    }

    private AimAssist() {}
}
