package org.minefortress;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.RaycastContext;

public class CameraManager {

    private final MinecraftClient minecraft;

    private boolean neededRotSet = false;
    private float neededXRot = 60f;
    private float neededYRot = 125f;

    private boolean yRotDirty = false;

    private Float moveDistance = null;

    public CameraManager(MinecraftClient minecraft) {
        this.minecraft = minecraft;
    }

    public void updateCameraPosition() {
        ClientPlayerEntity player = this.minecraft.player;
        if(player != null) {
            Input playerInput = player.input;

            if(this.minecraft.options.keySprint.isPressed()) {
                float speed = 30f * minecraft.getLastFrameDuration() / 16f;
                float deltaAngleY = playerInput.pressingLeft == playerInput.pressingRight ? 0.0F : playerInput.pressingLeft ? 1.0F : -1.0F;

                if(deltaAngleY != 0) {
                    if(moveDistance == null) {
                        calculateMoveDistance(player);
                    }
                    neededYRot += deltaAngleY * speed;
                    this.yRotDirty = true;
                } else {
                    resetMoveDistance();
                }

                float deltaAngleX = playerInput.pressingForward == playerInput.pressingBack ? 0.0F : playerInput.pressingForward ? 1.0F : -1.0F;
                neededXRot += deltaAngleX * speed * 2;
                if(neededXRot > 89) {
                    neededXRot = 89;
                }
                if(neededXRot < 0) {
                    neededXRot = 0;
                }
            } else {
                resetMoveDistance();
            }

            updateXRotation(player);
            updateYRotation(player);
        }
    }

    public boolean isNeededRotSet() {
        return neededRotSet;
    }

    public void setRot(float x, float y) {
        this.neededXRot = x;
        this.neededYRot = y;
        this.neededRotSet = true;
    }

    private void updateYRotation(ClientPlayerEntity player) {
        float yRot = player.getYaw();
        float xRot = player.getPitch();

        if(neededYRot != yRot) {
            player.setYaw(neededYRot);
            if(!yRotDirty) return;

            yRotDirty = false;

            Vec3d playerDirection = Vec3d.fromPolar(xRot, yRot).normalize();

            if(moveDistance != null) {
                float deltaRot = neededYRot - yRot;
                float moveScale = (float) (Math.sin(Math.toRadians(deltaRot / 2)) * moveDistance * 2);
                Vec3f moveVector = new Vec3f(playerDirection);
                Quaternion rotationQ = Vec3f.POSITIVE_Y.getDegreesQuaternion(deltaRot / 2);
                moveVector.rotate(rotationQ);
                moveVector.cross(Vec3f.POSITIVE_Y);
                moveVector.normalize();
                moveVector.scale(moveScale);
                moveVector.scale(-1);

                player.move(MovementType.SELF, new Vec3d(moveVector));
            }
        }
    }


    private void updateXRotation(ClientPlayerEntity player) {
        if(neededXRot != player.getPitch()) {
            player.setPitch(neededXRot);
        }
    }

    private void calculateMoveDistance(ClientPlayerEntity player) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        Vec3d playerDirection = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d eyePosition = player.getEyePos();
        Vec3d scaledViewVector = playerDirection.multiply(60.0);
        Vec3d movedEyePosition = eyePosition.add(scaledViewVector);

        RaycastContext clipContext = new RaycastContext(eyePosition, movedEyePosition, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, player);
        ClientWorld level = this.minecraft.world;
        BlockHitResult blockHitResult = level != null ? level.raycast(clipContext) : null;
        if(blockHitResult != null) {
            Vec3d hitPos = blockHitResult.getPos();
            moveDistance = (float) flatDistanceBetween(eyePosition, hitPos);
        }
    }

    private void resetMoveDistance() {
        moveDistance = null;
    }

    private double flatDistanceBetween(Vec3d a, Vec3d b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.z - b.z, 2));
    }

}
