package me.lokka30.levelledmobs.rules.strategies;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class YDistanceStrategy implements LevellingStrategy, Cloneable {
    public Integer startingYLevel;
    public Integer endingYLevel;
    public Integer yPeriod;

    public void mergeRule(final LevellingStrategy levellingStrategy){
        if (levellingStrategy instanceof YDistanceStrategy)
            mergeYDistanceStrategy((YDistanceStrategy) levellingStrategy);
    }

    public void mergeYDistanceStrategy(final YDistanceStrategy yds){
        if (yds == null) return;

        if (yds.startingYLevel != null) this.startingYLevel = yds.startingYLevel;
        if (yds.endingYLevel != null) this.endingYLevel = yds.endingYLevel;
        if (yds.yPeriod != null) this.yPeriod = yds.yPeriod;
    }

    public String toString(){
        return String.format("ycoord: start: %s, end: %s, yPeriod: %s",
                startingYLevel == null ? 0 : startingYLevel,
                endingYLevel == null ? 0 : endingYLevel,
                yPeriod == null ? 0 : yPeriod
        );
    }

    public int generateLevel(@NotNull final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel) {

        final int mobYLocation = lmEntity.getLivingEntity().getLocation().getBlockY();
        int yStart = this.startingYLevel == null ? 0 : this.startingYLevel;
        int yEnd = this.endingYLevel == null ? 0 : this.endingYLevel;
        final double yPeriod = this.yPeriod == null ? 0.0 : this.yPeriod;
        final boolean isAscending = (yEnd > yStart);
        if (!isAscending) {
            yStart = yEnd;
            yEnd = this.startingYLevel == null ? 0 : this.startingYLevel;
        }

        int useLevel = minLevel;
        boolean skipYPeriod = false;

        if (mobYLocation >= yEnd){
            useLevel = maxLevel;
            skipYPeriod = true;
        } else if (mobYLocation <= yStart)
            skipYPeriod = true;

        if (!skipYPeriod) {
            final double diff = yEnd - yStart;
            double useMobYLocation =  mobYLocation - yStart;

            if (yPeriod > 0)
                useLevel = (int) (useMobYLocation / yPeriod);
            else {
                double percent = useMobYLocation / diff;
                useLevel = (int) Math.ceil((maxLevel - 1) * percent);
            }
        }

        if (!isAscending)
            useLevel = maxLevel - useLevel + 1;

        useLevel += getVariance(lmEntity, useLevel >= maxLevel);

        if (useLevel < minLevel)
            useLevel = minLevel;
        else if (useLevel > maxLevel)
            useLevel = maxLevel;

        return useLevel;
    }

    private int getVariance(@NotNull final LivingEntityWrapper lmEntity, final boolean isAtMaxLevel){
        final int variance = lmEntity.getMainInstance().rulesManager.getRule_MaxRandomVariance(lmEntity);
        if (variance == 0) return 0;

        final int change = ThreadLocalRandom.current().nextInt(0, variance + 1);

        // Start variation. First check if variation is positive or negative towards the original level amount.
        if (!isAtMaxLevel || ThreadLocalRandom.current().nextBoolean()) {
            // Positive. Add the variation to the final level
            return change;
        } else {
            // Negative. Subtract the variation from the final level
            return -change;
        }
    }

    public YDistanceStrategy cloneItem() {
        YDistanceStrategy copy = null;
        try {
            copy = (YDistanceStrategy) super.clone();
        } catch (Exception ignored) {}

        return copy;
    }
}
