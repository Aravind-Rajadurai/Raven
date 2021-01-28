/**
 * Desc: class to manage all operations specific to weapons and their deployment
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven;

import Raven.armory.weapons.Raven_Weapon;
import static Raven.Raven_ObjectEnumerations.*;
import Raven.armory.weapons.Blaster;
import Raven.armory.weapons.RailGun;
import Raven.armory.weapons.RocketLauncher;
import Raven.armory.weapons.ShotGun;
import common.D2.Vector2D;
import static common.D2.Vector2D.add;
import static common.D2.Vector2D.sub;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.Vec2DDistance;
import static common.D2.Transformation.Vec2DRotateAroundOrigin;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.MinDouble;
import static common.misc.utils.RandInRange;
import static common.misc.Stream_Utility_function.ttos;
import java.util.HashMap;
import java.util.Map.Entry;

public class Raven_WeaponSystem {

    /**
     * a map of weapon instances indexed into by type
     */
    private class WeaponMap extends HashMap<Integer, Raven_Weapon> {
    };
    private Raven_Bot m_pOwner;
    /**
     * pointers to the weapons the bot is carrying (a bot may only carry one
     * instance of each weapon)
     */
    private WeaponMap m_WeaponMap = new WeaponMap();
    /**
     * a pointer to the weapon the bot is currently holding
     */
    private Raven_Weapon m_pCurrentWeapon;
    /**
     * this is the minimum amount of time a bot needs to see an opponent before
     * it can react to it. This variable is used to prevent a bot shooting at an
     * opponent the instant it becomes visible.
     */
    private double m_dReactionTime;
    /**
     * each time the current weapon is fired a certain amount of random noise is
     * added to the the angle of the shot. This prevents the bots from hitting
     * their opponents 100% of the time. The lower this value the more accurate
     * a bot's aim will be. Recommended values are between 0 and 0.2 (the value
     * represents the max deviation in radians that can be added to each shot).
     */
    private double m_dAimAccuracy;
    /**
     * the amount of time a bot will continue aiming at the position of the
     * target even if the target disappears from view.
     */
    private double m_dAimPersistance;

    /**
     * predicts where the target will be located in the time it takes for a
     * projectile to reach it. This uses a similar logic to the Pursuit steering
     * behavior. Used by TakeAimAndShoot.
     */
    private Vector2D PredictFuturePositionOfTarget() {
        double MaxSpeed = GetCurrentWeapon().GetMaxProjectileSpeed();

        //if the target is ahead and facing the agent shoot at its current pos
        Vector2D ToEnemy = sub(m_pOwner.GetTargetBot().Pos(), m_pOwner.Pos());

        //the lookahead time is proportional to the distance between the enemy
        //and the pursuer; and is inversely proportional to the sum of the
        //agent's velocities
        double LookAheadTime = ToEnemy.Length()
                / (MaxSpeed + m_pOwner.GetTargetBot().MaxSpeed());

        //return the predicted future position of the enemy
        return add(m_pOwner.GetTargetBot().Pos(),
                mul(m_pOwner.GetTargetBot().Velocity(), LookAheadTime));
    }

    /**
     * adds a random deviation to the firing angle not greater than
     * m_dAimAccuracy rads
     */
    private void AddNoiseToAim(Vector2D AimingPos) {
        Vector2D toPos = sub(AimingPos, m_pOwner.Pos());

        Vec2DRotateAroundOrigin(toPos, RandInRange(-m_dAimAccuracy, m_dAimAccuracy));

        AimingPos = add(toPos, m_pOwner.Pos());
    }

//------------------------- ctor ----------------------------------------------
//-----------------------------------------------------------------------------
    public Raven_WeaponSystem(Raven_Bot owner,
            double ReactionTime,
            double AimAccuracy,
            double AimPersistance) {
        m_pOwner = owner;
        m_dReactionTime = ReactionTime;
        m_dAimAccuracy = AimAccuracy;
        m_dAimPersistance = AimPersistance;
        Initialize();
    }

    //------------------------- dtor ----------------------------------------------
    //-----------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_WeaponMap.clear();
    }

    /**
     * sets up the weapon map with just one weapon: the blaster initializes the
     * weapons
     */
    public void Initialize() {
        //delete any existing weapons
        m_WeaponMap.clear();

        //set up the container
        m_pCurrentWeapon = new Blaster(m_pOwner);

        m_WeaponMap.put(type_blaster, m_pCurrentWeapon);
        m_WeaponMap.put(type_shotgun, null);
        m_WeaponMap.put(type_rail_gun, null);
        m_WeaponMap.put(type_rocket_launcher, null);
    }

    /**
     * this method aims the bot's current weapon at the target (if there is a
     * target) and, if aimed correctly, fires a round. (Called each update-step
     * from Raven_Bot::Update)
     */
    public void TakeAimAndShoot() {
        //aim the weapon only if the current target is shootable or if it has only
        //very recently gone out of view (this latter condition is to ensure the 
        //weapon is aimed at the target even if it temporarily dodges behind a wall
        //or other cover)
        if (m_pOwner.GetTargetSys().isTargetShootable()
                || (m_pOwner.GetTargetSys().GetTimeTargetHasBeenOutOfView()
                < m_dAimPersistance)) {
            //the position the weapon will be aimed at
            Vector2D AimingPos = m_pOwner.GetTargetBot().Pos();

            //if the current weapon is not an instant hit type gun the target position
            //must be adjusted to take into account the predicted movement of the 
            //target
            if (GetCurrentWeapon().GetType() == type_rocket_launcher
                    || GetCurrentWeapon().GetType() == type_blaster) {
                AimingPos = PredictFuturePositionOfTarget();

                //if the weapon is aimed correctly, there is line of sight between the
                //bot and the aiming position and it has been in view for a period longer
                //than the bot's reaction time, shoot the weapon
                if (m_pOwner.RotateFacingTowardPosition(AimingPos)
                        && (m_pOwner.GetTargetSys().GetTimeTargetHasBeenVisible()
                        > m_dReactionTime)
                        && m_pOwner.hasLOSto(AimingPos)) {
                    AddNoiseToAim(AimingPos);

                    GetCurrentWeapon().ShootAt(AimingPos);
                }
            } //no need to predict movement, aim directly at target
            else {
                //if the weapon is aimed correctly and it has been in view for a period
                //longer than the bot's reaction time, shoot the weapon
                if (m_pOwner.RotateFacingTowardPosition(AimingPos)
                        && (m_pOwner.GetTargetSys().GetTimeTargetHasBeenVisible()
                        > m_dReactionTime)) {
                    AddNoiseToAim(AimingPos);

                    GetCurrentWeapon().ShootAt(AimingPos);
                }
            }

        } //no target to shoot at so rotate facing to be parallel with the bot's
        //heading direction
        else {
            m_pOwner.RotateFacingTowardPosition(add(m_pOwner.Pos(), m_pOwner.Heading()));
        }
    }

    /**
     * this method determines the most appropriate weapon to use given the
     * current game state. (Called every n update-steps from Raven_Bot::Update)
     */
    public void SelectWeapon() {
        //if a target is present use fuzzy logic to determine the most desirable 
        //weapon.
        if (m_pOwner.GetTargetSys().isTargetPresent()) {
            //calculate the distance to the target
            double DistToTarget = Vec2DDistance(m_pOwner.Pos(), m_pOwner.GetTargetSys().GetTarget().Pos());

            //for each weapon in the inventory calculate its desirability given the 
            //current situation. The most desirable weapon is selected
            double BestSoFar = MinDouble;

            for (Entry<Integer, Raven_Weapon> curWeap : m_WeaponMap.entrySet()) {
                //grab the desirability of this weapon (desirability is based upon
                //distance to target and ammo remaining)
                if (curWeap.getValue() != null) {
                    double score = curWeap.getValue().GetDesirability(DistToTarget);

                    //if it is the most desirable so far select it
                    if (score > BestSoFar) {
                        BestSoFar = score;

                        //place the weapon in the bot's hand.
                        m_pCurrentWeapon = curWeap.getValue();
                    }
                }
            }
        } else {
            m_pCurrentWeapon = m_WeaponMap.get(type_blaster);
        }
    }

    /**
     * this will add a weapon of the specified type to the bot's inventory. If
     * the bot already has a weapon of this type only the ammo is added. (called
     * by the weapon giver-triggers to give a bot a weapon)
     */
    public void AddWeapon(int weapon_type) {
        //create an instance of this weapon
        Raven_Weapon w = null;

        switch (weapon_type) {
            case type_rail_gun:

                w = new RailGun(m_pOwner);
                break;

            case type_shotgun:

                w = new ShotGun(m_pOwner);
                break;

            case type_rocket_launcher:

                w = new RocketLauncher(m_pOwner);
                break;

        }//end switch


        //if the bot already holds a weapon of this type, just add its ammo
        Raven_Weapon present = GetWeaponFromInventory(weapon_type);

        if (present != null) {
            present.IncrementRounds(w.NumRoundsRemaining());

            w = null;
        } //if not already holding, add to inventory
        else {
            m_WeaponMap.put(weapon_type, w);
        }
    }

    /**
     * changes the current weapon to one of the specified type (provided that
     * type is in the bot's possession)
     */
    public void ChangeWeapon(int type) {
        Raven_Weapon w = GetWeaponFromInventory(type);

        if (w != null) {
            m_pCurrentWeapon = w;
        }
    }

    /**
     * shoots the current weapon at the given position
     */
    public void ShootAt(Vector2D pos) {
        GetCurrentWeapon().ShootAt(pos);
    }

    /**
     * returns a pointer to the current weapon
     */
    public Raven_Weapon GetCurrentWeapon() {
        return m_pCurrentWeapon;
    }

    /**
     * returns a pointer to any matching weapon.
     *
     * returns a null pointer if the weapon is not present
     */
    public Raven_Weapon GetWeaponFromInventory(int weapon_type) {
        return m_WeaponMap.get(weapon_type);
    }

    /**
     * returns the amount of ammo remaining for the specified weapon. Return
     * zero if the weapon is not present
     */
    public int GetAmmoRemainingForWeapon(int weapon_type) {
        if (m_WeaponMap.get(weapon_type) != null) {
            return m_WeaponMap.get(weapon_type).NumRoundsRemaining();
        }

        return 0;
    }

    public double ReactionTime() {
        return m_dReactionTime;
    }

    public void RenderCurrentWeapon() {
        GetCurrentWeapon().Render();
    }

    public void RenderDesirabilities() {
        Vector2D p = m_pOwner.Pos();

        int num = 0;

        for (Entry<Integer, Raven_Weapon> curWeap : m_WeaponMap.entrySet()) {
            if (curWeap.getValue() != null) {
                num++;
            }
        }

        int offset = 15 * num;

        for (Entry<Integer, Raven_Weapon> curWeap : m_WeaponMap.entrySet()) {
            if (curWeap.getValue() != null) {
                double score = curWeap.getValue().GetLastDesirabilityScore();
                String type = GetNameOfType(curWeap.getValue().GetType());

                gdi.TextAtPos(p.x + 10.0, p.y - offset, ttos(score) + " " + type);

                offset += 15;
            }
        }
    }
}