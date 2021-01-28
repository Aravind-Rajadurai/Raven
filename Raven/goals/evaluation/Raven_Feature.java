package Raven.goals.evaluation;

import Raven.Raven_Bot;
import Raven.armory.weapons.Raven_Weapon;
import static Raven.lua.Raven_Scriptor.script;
import static Raven.Raven_ObjectEnumerations.*;
import static common.misc.utils.clamp;

/**
 *
 * Author: Mat Buckland (ai-junkie.com)
 *
 * Desc: class that implements methods to extract feature specific information
 * from the Raven game world and present it as a value in the range 0 to 1
 *
 */
public class Raven_Feature {

    /**
     * returns a value between 0 and 1 based on the bot's health. The better the
     * health, the higher the rating
     */
    public static double Health(Raven_Bot pBot) {
        return (double) pBot.Health() / (double) pBot.MaxHealth();
    }

    /**
     * returns a value between 0 and 1 based on the bot's closeness to the given
     * item. the further the item, the higher the rating. If there is no item of
     * the given type present in the game world at the time this method is
     * called the value returned is 1
     */
    public static double DistanceToItem(Raven_Bot pBot, int ItemType) {
        //determine the distance to the closest instance of the item type
        double DistanceToItem = pBot.GetPathPlanner().GetCostToClosestItem(ItemType);

        //if the previous method returns a negative value then there is no item of
        //the specified type present in the game world at this time.
        if (DistanceToItem < 0) {
            return 1;
        }

        //these values represent cutoffs. Any distance over MaxDistance results in
        //a value of 0, and value below MinDistance results in a value of 1
        final double MaxDistance = 500.0;
        final double MinDistance = 50.0;

        DistanceToItem = clamp(DistanceToItem, MinDistance, MaxDistance);

        return DistanceToItem / MaxDistance;
    }

    /**
     * returns a value between 0 and 1 based on how much ammo the bot has for
     * the given weapon, and the maximum amount of ammo the bot can carry. The
     * closer the amount carried is to the max amount, the higher the score
     */
    public static double IndividualWeaponStrength(Raven_Bot pBot,
            int WeaponType) {
        //grab a pointer to the gun (if the bot owns an instance)
        Raven_Weapon wp = pBot.GetWeaponSys().GetWeaponFromInventory(WeaponType);

        if (wp != null) {
            return wp.NumRoundsRemaining() / GetMaxRoundsBotCanCarryForWeapon(WeaponType);
        } else {
            return 0.0;
        }
    }

    /**
     * returns a value between 0 and 1 based on the total amount of ammo the bot
     * is carrying each of the weapons. Each of the three weapons a bot can pick
     * up can contribute a third to the score. In other words, if a bot is
     * carrying a RL and a RG and has max ammo for the RG but only half max for
     * the RL the rating will be 1/3 + 1/6 + 0 = 0.5
     */
    public static double TotalWeaponStrength(Raven_Bot pBot) {
        final double MaxRoundsForShotgun = GetMaxRoundsBotCanCarryForWeapon(type_shotgun);
        final double MaxRoundsForRailgun = GetMaxRoundsBotCanCarryForWeapon(type_rail_gun);
        final double MaxRoundsForRocketLauncher = GetMaxRoundsBotCanCarryForWeapon(type_rocket_launcher);
        final double TotalRoundsCarryable = MaxRoundsForShotgun + MaxRoundsForRailgun + MaxRoundsForRocketLauncher;

        double NumSlugs = (double) pBot.GetWeaponSys().GetAmmoRemainingForWeapon(type_rail_gun);
        double NumCartridges = (double) pBot.GetWeaponSys().GetAmmoRemainingForWeapon(type_shotgun);
        double NumRockets = (double) pBot.GetWeaponSys().GetAmmoRemainingForWeapon(type_rocket_launcher);

        //the value of the tweaker (must be in the range 0-1) indicates how much
        //desirability value is returned even if a bot has not picked up any weapons.
        //(it basically adds in an amount for a bot's persistent weapon -- the blaster)
        final double Tweaker = 0.1;

        return Tweaker + (1 - Tweaker) * (NumSlugs + NumCartridges + NumRockets) / (MaxRoundsForShotgun + MaxRoundsForRailgun + MaxRoundsForRocketLauncher);
    }

    /**
     * helper function to tidy up IndividualWeapon method returns the maximum
     * rounds of ammo a bot can carry for the given weapon
     */
    private static double GetMaxRoundsBotCanCarryForWeapon(int WeaponType) {
        switch (WeaponType) {
            case type_rail_gun:

                return script.GetDouble("RailGun_MaxRoundsCarried");

            case type_rocket_launcher:

                return script.GetDouble("RocketLauncher_MaxRoundsCarried");

            case type_shotgun:

                return script.GetDouble("ShotGun_MaxRoundsCarried");

            default:

                throw new RuntimeException("trying to calculate  of unknown weapon");

        }//end switch
    }
}