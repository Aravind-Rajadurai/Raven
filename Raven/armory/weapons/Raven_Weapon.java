/**
 * Desc: Base Weapon class for the raven project
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.weapons;

import Raven.Raven_Bot;
import common.D2.Vector2D;
import static common.Time.CrudeTimer.Clock;
import common.fuzzy.FuzzyModule;
import static common.misc.utils.clamp;
import java.util.ArrayList;
import java.util.List;

abstract public class Raven_Weapon {

	/**
	 * a weapon is always (in this game) carried by a bot
	 */
	protected Raven_Bot m_pOwner;
	/**
	 * an enumeration indicating the type of weapon
	 */
	protected int m_iType;
	/**
	 * fuzzy logic is used to determine the desirability of a weapon. Each
	 * weapon owns its own instance of a fuzzy module because each has a
	 * different rule set for inferring desirability.
	 */
	protected FuzzyModule m_FuzzyModule = new FuzzyModule();
	/**
	 * amount of ammo carried for this weapon
	 */
	protected int m_iNumRoundsLeft;
	/**
	 * maximum number of rounds a bot can carry for this weapon
	 */
	protected int m_iMaxRoundsCarried;
	/**
	 * the number of times this weapon can be fired per second
	 */
	protected double m_dRateOfFire;
	/**
	 * the earliest time the next shot can be taken
	 */
	protected double m_dTimeNextAvailable;
	/**
	 * this is used to keep a local copy of the previous desirability score so
	 * that we can give some feedback for debugging
	 */
	protected double m_dLastDesirabilityScore;
	/**
	 * this is the prefered distance from the enemy when using this weapon
	 */
	protected double m_dIdealRange;
	/**
	 * the max speed of the projectile this weapon fires
	 */
	protected double m_dMaxProjectileSpeed;

	/**
	 * The number of times a weapon can be discharges depends on its rate of
	 * fire. This method returns true if the weapon is able to be discharged at
	 * the current time. (called from ShootAt() )
	 *
	 * @return true if the weapon is ready to be discharged
	 */
	protected boolean isReadyForNextShot() {
		if (Clock.GetCurrentTime() > m_dTimeNextAvailable) {
			return true;
		}

		return false;
	}

	/**
	 * this is called when a shot is fired to update m_dTimeNextAvailable
	 */
	protected void UpdateTimeWeaponIsNextAvailable() {
		m_dTimeNextAvailable = Clock.GetCurrentTime() + 1.0 / m_dRateOfFire;
	}

	/**
	 * this method initializes the fuzzy module with the appropriate fuzzy
	 * variables and rule base.
	 */
	abstract protected void InitializeFuzzyModule();
	/**
	 * vertex buffers containing the weapon's geometry
	 */
	protected List<Vector2D> m_vecWeaponVB = new ArrayList<Vector2D>();
	protected List<Vector2D> m_vecWeaponVBTrans = new ArrayList<Vector2D>();

	public Raven_Weapon(int TypeOfGun,
			int DefaultNumRounds,
			int MaxRoundsCarried,
			double RateOfFire,
			double IdealRange,
			double ProjectileSpeed,
			Raven_Bot OwnerOfGun) {
		m_iType = TypeOfGun;
		m_iNumRoundsLeft = DefaultNumRounds;
		m_pOwner = OwnerOfGun;
		m_dRateOfFire = RateOfFire;
		m_iMaxRoundsCarried = MaxRoundsCarried;
		m_dLastDesirabilityScore = 0;
		m_dIdealRange = IdealRange;
		m_dMaxProjectileSpeed = ProjectileSpeed;
		m_dTimeNextAvailable = Clock.GetCurrentTime();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	/**
	 * this method aims the weapon at the given target by rotating the weapon's
	 * owner's facing direction (constrained by the bot's turning rate). It
	 * returns true if the weapon is directly facing the target.
	 */
	public boolean AimAt(Vector2D target) {
		return m_pOwner.RotateFacingTowardPosition(target);
	}

	/**
	 * this discharges a projectile from the weapon at the given target position
	 * (provided the weapon is ready to be discharged... every weapon has its
	 * own rate of fire)
	 */
	abstract public void ShootAt(Vector2D pos);

	/**
	 * each weapon has its own shape and color
	 */
	public abstract void Render();

	/**
	 * this method returns a value representing the desirability of using the
	 * weapon. This is used by the AI to select the most suitable weapon for a
	 * bot's current situation. This value is calculated using fuzzy logic
	 */
	public abstract double GetDesirability(double DistToTarget);

	/**
	 * returns the desirability score calculated in the last call to
	 * GetDesirability (just used for debugging)
	 */
	public double GetLastDesirabilityScore() {
		return m_dLastDesirabilityScore;
	}

	/**
	 * @return the maximum speed of the projectile this weapon fires
	 */
	public double GetMaxProjectileSpeed() {
		return m_dMaxProjectileSpeed;
	}

	/**
	 * returns the number of rounds remaining for the weapon
	 */
	public int NumRoundsRemaining() {
		return m_iNumRoundsLeft;
	}

	public void DecrementNumRounds() {
		if (m_iNumRoundsLeft > 0) {
			--m_iNumRoundsLeft;
		}
	}

	public void IncrementRounds(int num) {
		m_iNumRoundsLeft += num;
		m_iNumRoundsLeft = clamp(m_iNumRoundsLeft, 0, m_iMaxRoundsCarried);
	}

	public int GetType() {
		return m_iType;
	}

	public double GetIdealRange() {
		return m_dIdealRange;
	}
}