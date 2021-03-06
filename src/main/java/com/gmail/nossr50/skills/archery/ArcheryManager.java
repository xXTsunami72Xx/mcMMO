package com.gmail.nossr50.skills.archery;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SecondaryAbility;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.CombatUtils;
import com.gmail.nossr50.util.skills.SkillUtils;

public class ArcheryManager extends SkillManager {
    public ArcheryManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, SkillType.ARCHERY);
    }

    public boolean canDaze(LivingEntity target) {
        return target instanceof Player && Permissions.secondaryAbilityEnabled(getPlayer(), SecondaryAbility.DAZE);
    }

    public boolean canSkillShot() {
        return getSkillLevel() >= Archery.skillShotIncreaseLevel && Permissions.secondaryAbilityEnabled(getPlayer(), SecondaryAbility.SKILL_SHOT);
    }

    public boolean canRetrieveArrows() {
        return Permissions.secondaryAbilityEnabled(getPlayer(), SecondaryAbility.RETRIEVE);
    }

    /**
     * Calculate bonus XP awarded for Archery when hitting a far-away target.
     *
     * @param target The {@link LivingEntity} damaged by the arrow
     * @param damager The {@link Entity} who shot the arrow
     */
    public void distanceXpBonus(LivingEntity target, Entity damager) {
        Location firedLocation = (Location) damager.getMetadata(mcMMO.arrowDistanceKey).get(0).value();
        Location targetLocation = target.getLocation();

        if (firedLocation.getWorld() != targetLocation.getWorld()) {
            return;
        }

        applyXpGain((int) (firedLocation.distanceSquared(targetLocation) * Archery.DISTANCE_XP_MULTIPLIER), getXPGainReason(target, damager));
    }

    /**
     * Track arrows fired for later retrieval.
     *
     * @param target The {@link LivingEntity} damaged by the arrow
     */
    public void retrieveArrows(LivingEntity target) {
        if (SkillUtils.activationSuccessful(SecondaryAbility.RETRIEVE, getPlayer(), getSkillLevel(), activationChance)) {
            Archery.incrementTrackerValue(target);
        }
    }

    /**
     * Handle the effects of the Daze ability
     *
     * @param defender The {@link Player} being affected by the ability
     * @param arrow The {@link Arrow} that was fired
     */
    public double daze(Player defender, Arrow arrow) {
        if (!SkillUtils.activationSuccessful(SecondaryAbility.DAZE, getPlayer(), getSkillLevel(), activationChance)) {
            return 0;
        }

        Location dazedLocation = defender.getLocation();
        dazedLocation.setPitch(90 - Misc.getRandom().nextInt(181));

        defender.teleport(dazedLocation);
        defender.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 10));

        if (UserManager.getPlayer(defender).useChatNotifications()) {
            defender.sendMessage(LocaleLoader.getString("Combat.TouchedFuzzy"));
        }

        if (mcMMOPlayer.useChatNotifications()) {
            getPlayer().sendMessage(LocaleLoader.getString("Combat.TargetDazed"));
        }

        return CombatUtils.callFakeDamageEvent(arrow, defender, DamageCause.PROJECTILE, Archery.dazeModifier);
    }

    /**
     * Handle the effects of the Skill Shot ability
     *
     * @param target The {@link LivingEntity} being affected by the ability
     * @param damage The amount of damage initially dealt by the event
     * @param arrow The {@link Arrow} that was fired
     */
    public double skillShot(LivingEntity target, double damage, Arrow arrow) {
        double damageBonusPercent = Math.min(((getSkillLevel() / Archery.skillShotIncreaseLevel) * Archery.skillShotIncreasePercentage), Archery.skillShotMaxBonusPercentage);
        double archeryBonus = Math.min(damage * damageBonusPercent, Archery.skillShotMaxBonusDamage);

        return CombatUtils.callFakeDamageEvent(arrow, target, DamageCause.PROJECTILE, archeryBonus);
    }
}
