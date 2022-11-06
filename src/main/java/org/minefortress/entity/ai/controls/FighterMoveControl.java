package org.minefortress.entity.ai.controls;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.minefortress.entity.BaritonableEntity;

import java.util.Optional;

public class FighterMoveControl {

    private final IBaritone baritone;
    private final float reachRange;
    private final BaritonableEntity baritonableEntity;

    private BlockPos moveTarget;
    private LivingEntity followTarget;

    private boolean stuck = false;

    public FighterMoveControl(BaritonableEntity baritonableEntity) {
        this.baritonableEntity = baritonableEntity;
        this.baritone = BaritoneAPI.getProvider().getBaritone(baritonableEntity);
        this.baritone.getGameEventHandler().registerEventListener(new StuckOnFailEventListener());
        this.reachRange = baritonableEntity.getReachRange();
    }

    public void moveTo(@NotNull BlockPos pos) {
        this.reset();
        this.moveTarget = pos;
        final var goal = new GoalNear(pos, (int) Math.floor(reachRange));
        baritone.getCustomGoalProcess().setGoalAndPath(goal);
    }

    public void moveTo(@NotNull LivingEntity entity) {
        this.reset();
        this.followTarget = entity;
        baritone.getFollowProcess().follow(it -> it.equals(entity));
    }

    public boolean tryingToReachTheGoal() {
        return !baritone.getFollowProcess().following().isEmpty() || baritone.getCustomGoalProcess().isActive();
    }

    public void reset() {
        baritone.getFollowProcess().cancel();
        baritone.getPathingBehavior().cancelEverything();
        moveTarget = null;
        followTarget = null;
        stuck = false;
    }

    public boolean isStuck() {
        return stuck;
    }

    private Optional<BlockPos> getTargetPos() {
        return Optional.ofNullable(moveTarget).or(() -> Optional.ofNullable(followTarget).map(LivingEntity::getBlockPos));
    }

    private boolean moveTargetInRange() {
        return getTargetPos().map(it -> it.isWithinDistance(baritonableEntity.getPos(), reachRange)).orElse(false);
    }

    private class StuckOnFailEventListener implements AbstractGameEventListener {

        private BlockPos lastDestination;
        private int stuckCounter = 0;

        @Override
        public void onPathEvent(PathEvent pathEvent) {
            checkFalseAtGoal(pathEvent);
            checkStuckOnTheSamePlace(pathEvent);
            checkFailedToCalc(pathEvent);
        }

        private void checkFalseAtGoal(PathEvent pathEvent) {
            if(pathEvent == PathEvent.AT_GOAL && !moveTargetInRange()) {
                stuck = true;
            }
        }

        private void checkFailedToCalc(PathEvent pathEvent) {
            if(pathEvent == PathEvent.CALC_FAILED) {
                stuck = true;
            }
        }

        private void checkStuckOnTheSamePlace(PathEvent pathEvent) {
            if(pathEvent == PathEvent.CALC_FINISHED_NOW_EXECUTING){
                final var dest = baritone.getPathingBehavior().getPath().map(IPath::getDest).orElse(BetterBlockPos.ORIGIN);
                if(lastDestination != null) {
                    if (!BlockPos.ORIGIN.equals(lastDestination) && dest.equals(lastDestination)) {
                        stuckCounter++;
                        if (stuckCounter > 1) {
                            stuck = true;
                            stuckCounter = 0;
                            lastDestination = null;
                            baritone.getPathingBehavior().cancelEverything();
                        }
                    } else {
                        stuckCounter = 0;
                    }
                }
                lastDestination = dest;
            }
        }
    }

}
