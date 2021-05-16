package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RulesManager {
    public RulesManager(final LevelledMobs main) {
        this.main = main;
        this.rulesInEffect = new TreeMap<>();
        this.levelNumbersWithBiasMapCache = new TreeMap<>();
    }

    private final LevelledMobs main;
    private final Map<String, LevelNumbersWithBias> levelNumbersWithBiasMapCache;
    @NotNull
    public final SortedMap<Integer, List<RuleInfo>> rulesInEffect;

    @Nullable
    public LevelNumbersWithBias getRule_LowerMobLevelBiasFactor(@NotNull final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel){
        Integer lowerMobLevelBiasFactor = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.lowerMobLevelBiasFactor != null) lowerMobLevelBiasFactor = ruleInfo.lowerMobLevelBiasFactor;
        }

        if (lowerMobLevelBiasFactor == null) return null;

        final String checkName = String.format("%s-%s-%s", minLevel, maxLevel, lowerMobLevelBiasFactor);
        if (this.levelNumbersWithBiasMapCache.containsKey(checkName))
            return this.levelNumbersWithBiasMapCache.get(checkName);

        LevelNumbersWithBias levelNumbersWithBias = new LevelNumbersWithBias(minLevel, maxLevel, lowerMobLevelBiasFactor);
        this.levelNumbersWithBiasMapCache.put(checkName, levelNumbersWithBias);
        return levelNumbersWithBias;
    }

    public int getRule_MaxRandomVariance(@NotNull final LivingEntityWrapper lmEntity){
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.maxRandomVariance != null) result = ruleInfo.maxRandomVariance;
        }

        return result;
    }

    public boolean getRule_CheckIfNoDropMultiplierEntitiy(@NotNull final LivingEntityWrapper lmEntity){
        CachedModalList<String> entitiesList = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.conditions_NoDropEntities != null) entitiesList = ruleInfo.conditions_NoDropEntities;
        }

        return entitiesList != null && entitiesList.isEnabledInList(lmEntity.getNameIfBaby());
    }

    @NotNull
    public CustomDropsRuleSet getRule_UseCustomDropsForMob(@NotNull final LivingEntityWrapper lmEntity){
        final CustomDropsRuleSet dropRules = new CustomDropsRuleSet();
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.customDrops_UseForMobs != null) dropRules.useDrops = ruleInfo.customDrops_UseForMobs;
            if (ruleInfo.customDrops_UseOverride != null) dropRules.override = ruleInfo.customDrops_UseOverride;
            if (ruleInfo.customDrop_DropTableId != null) dropRules.useDropTableId = ruleInfo.customDrop_DropTableId;
        }

        return dropRules;
    }

    public boolean getRule_IsMobAllowedInEntityOverride(@NotNull final LivingEntityInterface lmInterface){
        // check if it should be denied thru the entity override list
        boolean babyMobsInheritAdultSetting = true; // default
        CachedModalList<String> allowedEntitiesList = null;
        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()){
            if (ruleInfo.allowedEntities != null)
                allowedEntitiesList = ruleInfo.allowedEntities;
            if (ruleInfo.babyMobsInheritAdultSetting != null)
                babyMobsInheritAdultSetting = ruleInfo.babyMobsInheritAdultSetting;
        }

        if (lmInterface instanceof LivingEntityWrapper) {
            LivingEntityWrapper lmEntity = (LivingEntityWrapper) lmInterface;
            return (
                    allowedEntitiesList == null ||
                            !babyMobsInheritAdultSetting && lmEntity.isBabyMob() && Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity, true)) ||
                    Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity, babyMobsInheritAdultSetting);
        }
        else {
            return (
                    allowedEntitiesList == null || allowedEntitiesList.isEnabledInList(lmInterface.getTypeName())
            );
        }
    }

    public FineTuningAttributes getFineTuningAttributes(@NotNull final LivingEntityWrapper lmEntity){
        FineTuningAttributes defaultAttribs = null;
        FineTuningAttributes mobAttribs = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.defaultFineTuning == null) continue;

            if (defaultAttribs == null)
                defaultAttribs = ruleInfo.defaultFineTuning;
            else
                defaultAttribs.mergeAttributes(ruleInfo.defaultFineTuning);

            if (ruleInfo.fineTuning != null && ruleInfo.fineTuning.containsKey(lmEntity.getNameIfBaby())){
                final FineTuningAttributes tempAttribs = ruleInfo.fineTuning.get(lmEntity.getNameIfBaby());
                if (mobAttribs == null)
                    mobAttribs = tempAttribs;
                else
                    mobAttribs.mergeAttributes(tempAttribs);
            }
        }

        if (defaultAttribs != null) defaultAttribs.mergeAttributes(mobAttribs);
        return defaultAttribs;
    }

    public int getRule_CreeperMaxBlastRadius(@NotNull final LivingEntityWrapper lmEntity){
        int maxBlast = 5;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.creeperMaxDamageRadius != null) maxBlast = ruleInfo.creeperMaxDamageRadius;
        }

        return maxBlast;
    }

    @Nullable
    public LevellingStrategy getRule_LevellingStrategy(@NotNull final LivingEntityWrapper lmEntity){
        LevellingStrategy levellingStrategy = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.useRandomLevelling != null && ruleInfo.useRandomLevelling) {
                // specifying random in strategies will nullify any levelling systems to that point
                levellingStrategy = null;
            }
            else if (!ruleInfo.levellingStrategies.isEmpty())
                levellingStrategy = ruleInfo.levellingStrategies.get(0);
        }

        return levellingStrategy;
    }

    public boolean getRule_MobLevelInheritance(@NotNull final LivingEntityWrapper lmEntity){
        boolean result = true;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.mobLevelInheritance != null) result = ruleInfo.mobLevelInheritance;
        }

        return result;
    }

    public MobCustomNameStatusEnum getRule_MobCustomNameStatus(@NotNull final LivingEntityWrapper lmEntity){
        MobCustomNameStatusEnum result = MobCustomNameStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobCustomnameStatus;

        return result;
    }

    public MobTamedStatusEnum getRule_MobTamedStatus(@NotNull final LivingEntityWrapper lmEntity){
        MobTamedStatusEnum result = MobTamedStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobTamedStatus != MobTamedStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobTamedStatus;

        return result;
    }

    public int getRule_MobMinLevel(@NotNull final LivingEntityInterface lmInterface){
        int minLevel = 1;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MinLevel != null) minLevel = ruleInfo.restrictions_MinLevel;
        }

        return minLevel;
    }

    public int getRule_MobMaxLevel(@NotNull final LivingEntityInterface lmInterface){
        int maxLevel = 10;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MaxLevel != null) maxLevel = ruleInfo.restrictions_MaxLevel;
        }

        return maxLevel;
    }

    @NotNull
    public String getRule_Nametag(@NotNull final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag)) {
                nametag = "disabled".equalsIgnoreCase(ruleInfo.nametag) ?
                    "" : ruleInfo.nametag;
            }
        }

        return nametag;
    }

    @NotNull
    public String getRule_Nametag_CreatureDeath(@NotNull final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag_CreatureDeath)) nametag = ruleInfo.nametag_CreatureDeath;
        }

        return nametag;
    }

    public boolean getRule_CreatureNametagAlwaysVisible(@NotNull final LivingEntityWrapper lmEntity){
        boolean creatureNametagAlwaysVisible = false;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.CreatureNametagAlwaysVisible != null)
                creatureNametagAlwaysVisible = ruleInfo.CreatureNametagAlwaysVisible;
        }

        return creatureNametagAlwaysVisible;
    }

    @Nullable
    public String getRule_TieredPlaceholder(@NotNull final LivingEntityWrapper lmEntity){
        List<TieredColoringInfo> coloringInfo = null;
        String tieredText = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.tieredColoringInfos != null) coloringInfo = ruleInfo.tieredColoringInfos;
        }

        if (coloringInfo == null) return  null;

        final int mobLevel = lmEntity.getMobLevel();
        for (TieredColoringInfo info : coloringInfo){
            if (info.isDefault) tieredText = info.text;
            if (mobLevel >= info.minLevel && mobLevel <= info.maxLevel){
                tieredText = info.text;
                break;
            }
        }

        return tieredText;
    }

    @Nullable
    public String getRule_EntityOverriddenName(@NotNull final LivingEntityWrapper lmEntity){
        List<String> overridenNames = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.entityNameOverrides.containsKey(lmEntity.getNameIfBaby()))
                overridenNames = ruleInfo.entityNameOverrides.get(lmEntity.getNameIfBaby());
        }

        if (overridenNames == null || overridenNames.isEmpty())
            return null;
        else if (overridenNames.size() > 1) {
            Collections.shuffle(overridenNames);
            // set a PDC key with the name otherwise the name will constantly change
            lmEntity.setOverridenEntityName(overridenNames.get(0));
        }

        return overridenNames.get(0);
    }

    @NotNull
    public List<RuleInfo> getApplicableRules(final LivingEntityInterface lmInterface){
        final List<RuleInfo> applicableRules = new LinkedList<>();

        for (final int rulePriority : rulesInEffect.keySet()) {
            final List<RuleInfo> rules = rulesInEffect.get(rulePriority);
            for (final RuleInfo ruleInfo : rules) {

                if (!ruleInfo.ruleIsEnabled) continue;
                if (lmInterface instanceof LivingEntityWrapper && !isRuleApplicable_Entity((LivingEntityWrapper) lmInterface, ruleInfo))
                    continue;
                if (!isRuleApplicable_Interface(lmInterface, ruleInfo)) continue;

                if (ruleInfo.stopProcessingRules != null && ruleInfo.stopProcessingRules) {
                    // TODO: // should we add a debug entry here?
                    break;
                }

                applicableRules.add(ruleInfo);
            }
        }

        return applicableRules;
    }

    private boolean isRuleApplicable_Entity(final LivingEntityWrapper lmEntity, @NotNull final RuleInfo ri){
        if (ri.conditions_MinLevel != null && (!lmEntity.isLevelled() || ri.conditions_MinLevel < lmEntity.getMobLevel())) {
            Utils.debugLog(main, DebugType.DENIED_RULE_MAXLEVEL, String.format("%s, mob: %s, mob lvl: %s, rule minlvl: %s",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(), ri.conditions_MinLevel));
            return false;
        }
        if (ri.conditions_MaxLevel != null && (!lmEntity.isLevelled() || ri.conditions_MaxLevel > lmEntity.getMobLevel())) {
            Utils.debugLog(main, DebugType.DENIED_RULE_MAXLEVEL, String.format("%s, mob: %s, mob lvl: %s, rule maxlvl: %s",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(), ri.conditions_MaxLevel));
            return false;
        }

        if (ri.conditions_CustomNames != null && lmEntity.getLivingEntity().getCustomName() != null &&
                !ri.conditions_CustomNames.isEnabledInList(lmEntity.getLivingEntity().getCustomName())) {
            Utils.debugLog(main, DebugType.DENIED_RULE_CUSTOM_NAME, String.format("%s, mob: %s, name: %s",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getLivingEntity().getCustomName()));
            return false;
        }

        if (ri.conditions_SpawnReasons != null && !ri.conditions_SpawnReasons.isEnabledInList(lmEntity.getSpawnReason())){
            Utils.debugLog(main, DebugType.DENIED_RULE_SPAWN_REASON, String.format("%s, mob: %s, spawn reason: %s",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getSpawnReason()));
            return false;
        }

        if (lmEntity.isMobOfExternalType() && ri.conditions_ApplyPlugins != null && !ri.conditions_ApplyPlugins.isEnabledInList(lmEntity.getMobExternalType().toString())){
            Utils.debugLog(main, DebugType.DENIED_RULE_PLUGIN_COMPAT, String.format("%s, mob: %s, mob plugin: %s",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobExternalType()));
            return false;
        }

        return true;
    }

    private boolean isRuleApplicable_Interface(final LivingEntityInterface lmInterface, final RuleInfo ri){

        if (lmInterface instanceof LivingEntityWrapper) {
            if (ri.conditions_Entities != null && !Utils.isLivingEntityInModalList(ri.conditions_Entities, (LivingEntityWrapper) lmInterface)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_ENTITIES_LIST, String.format("%s, mob: %s", ri.getRuleName(), lmInterface.getEntityType().name()));
                return false;
            }
        }
        else {
            // can't check groups if not a living entity wrapper
            if (ri.conditions_Entities != null && !ri.conditions_Entities.isEnabledInList(lmInterface.getEntityType().name())) {
                Utils.debugLog(main, DebugType.DENIED_RULE_ENTITIES_LIST, String.format("%s, mob: %s", ri.getRuleName(), lmInterface.getEntityType().name()));
                return false;
            }
        }

        if (ri.worlds != null && !ri.worlds.isEnabledInList(lmInterface.getWorld().getName())) {
            Utils.debugLog(main, DebugType.DENIED_RULE_WORLD_LIST, String.format("%s, mob: %s, mob world: %s",
                    ri.getRuleName(), lmInterface.getEntityType().name(), lmInterface.getWorld().getName()));
            return false;
        }

        if (ri.conditions_Biomes != null && !ri.conditions_Biomes.isEnabledInList(lmInterface.getLocation().getBlock().getBiome().toString())) {
            Utils.debugLog(main, DebugType.DENIED_RULE_BIOME_LIST, String.format("%s, mob: %s, mob biome: %s",
                    ri.getRuleName(), lmInterface.getEntityType().name(), lmInterface.getLocation().getBlock().getBiome().name()));
            return false;
        }

        if (ri.conditions_WGRegions != null){
            boolean isInList = false;
            final List<String> wgRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(lmInterface);
            if (wgRegions != null) {
                for (final String regionName : wgRegions) {
                    if (ri.conditions_WGRegions.isEnabledInList(regionName)) {
                        isInList = true;
                        break;
                    }
                }
            }
            if (!isInList){
                // TODO: add debug info
                return false;
            }
        }

        if (ri.conditions_ApplyAboveY != null && lmInterface.getLocation().getBlockY() < ri.conditions_ApplyAboveY){
            // TODO: add debug info
            return false;
        }

        if (ri.conditions_ApplyBelowY != null && lmInterface.getLocation().getBlockY() > ri.conditions_ApplyBelowY){
            // TODO: add debug info
            return false;
        }

        if (ri.conditions_Chance != null && ri.conditions_Chance < 1.0){
            //new Throwable().printStackTrace();

            final double chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
            if (chanceRole < ri.conditions_Chance){
                Utils.debugLog(main, DebugType.DENIED_RULE_CHANCE, String.format("%s, mob: %s, chance: %s, chance role: %s",
                        ri.getRuleName(), lmInterface.getTypeName(), ri.conditions_Chance, chanceRole));
                return false;
            }
        }

        return true;
    }
}