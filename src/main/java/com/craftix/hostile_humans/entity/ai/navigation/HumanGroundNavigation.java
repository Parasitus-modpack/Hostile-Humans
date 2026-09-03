package com.craftix.hostile_humans.entity.ai.navigation;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class HumanGroundNavigation extends GroundPathNavigation {
    private static final int MAX_CATCH_UP_ADVANCES_PER_TICK = 4;
    private static final double PASSED_ALONG_THRESHOLD = -0.30D;

    public HumanGroundNavigation(Human mob, Level level) {
        super(mob, level);
    }

    @Override
    protected void followThePath() {
        super.followThePath();
        this.catchUpToOvershotNodes();
    }

    private void catchUpToOvershotNodes() {
        Path path = this.path;
        if (path == null || path.isDone()) {
            return;
        }
        int count = path.getNodeCount();
        if (count < 2) {
            return;
        }

        double centerOffset = ((double) this.mob.getBbWidth() + 1.0D) / 2.0D;
        int advances = 0;
        while (!path.isDone() && advances < MAX_CATCH_UP_ADVANCES_PER_TICK) {
            int index = path.getNextNodeIndex();
            Node current = path.getNode(index);

            double dirX;
            double dirZ;
            if (index + 1 < count) {
                Node next = path.getNode(index + 1);
                dirX = ((double) next.x + centerOffset) - ((double) current.x + centerOffset);
                dirZ = ((double) next.z + centerOffset) - ((double) current.z + centerOffset);
            } else {
                Node previous = path.getNode(count - 2);
                dirX = ((double) current.x + centerOffset) - ((double) previous.x + centerOffset);
                dirZ = ((double) current.z + centerOffset) - ((double) previous.z + centerOffset);
            }

            double dirLengthSq = dirX * dirX + dirZ * dirZ;
            if (dirLengthSq < 1.0E-6D) {
                return;
            }

            double dx = this.mob.getX() - ((double) current.x + centerOffset);
            double dz = this.mob.getZ() - ((double) current.z + centerOffset);
            double alongDirection = (dx * dirX + dz * dirZ) / Math.sqrt(dirLengthSq);
            if (alongDirection <= PASSED_ALONG_THRESHOLD) {
                return;
            }

            path.advance();
            advances++;
        }
    }
}
