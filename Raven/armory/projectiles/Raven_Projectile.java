/**
 * Desc: Base class to define a projectile type. A projectile of the correct
 * type is created whnever a weapon is fired. In Raven there are four types of
 * projectile: Slugs (railgun), Pellets (shotgun), Rockets (rocket launcher )
 * and Bolts (Blaster)
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.projectiles;

import Raven.MovingEntity;
import Raven.Raven_Bot;
import Raven.Raven_Game;
import common.D2.Vector2D;
import static common.D2.Vector2D.Vec2DDistanceSq;
import static common.D2.geometry.DistToLineSegment;
import static common.Time.CrudeTimer.Clock;
import static common.misc.utils.MaxDouble;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Raven_Projectile extends MovingEntity {

	/**
	 * the ID of the entity that fired this
	 */
	protected int m_iShooterID;
	/**
	 * the place the projectile is aimed at
	 */
	protected Vector2D m_vTarget = new Vector2D();
	/**
	 * a pointer to the world data
	 */
	protected Raven_Game m_pWorld;
	/**
	 * where the projectile was fired from
	 */
	protected Vector2D m_vOrigin = new Vector2D();
	/**
	 * how much damage the projectile inflicts
	 */
	protected int m_iDamageInflicted;
	/**
	 * is it dead? A dead projectile is one that has come to the end of its
	 * trajectory and cycled through any explosion sequence. A dead projectile
	 * can be removed from the world environment and deleted.
	 */
	protected boolean m_bDead;
	/**
	 * this is set to true as soon as a projectile hits something
	 */
	protected boolean m_bImpacted;
	/**
	 * the position where this projectile impacts an object
	 */
	protected Vector2D m_vImpactPoint = new Vector2D();
	/**
	 * this is stamped with the time this projectile was instantiated. This is
	 * to enable the shot to be rendered for a specific length of time
	 */
	protected double m_dTimeOfCreation;

	protected Raven_Bot GetClosestIntersectingBot(Vector2D From, Vector2D To) {
		Raven_Bot ClosestIntersectingBot = null;
		double ClosestSoFar = MaxDouble;

		//iterate through all entities checking against the line segment FromTo

		for (Raven_Bot curBot : m_pWorld.GetAllBots()) {
			//make sure we don't check against the shooter of the projectile
			if (curBot.ID() != m_iShooterID) {
				//if the distance to FromTo is less than the entity's bounding radius then
				//there is an intersection
				if (DistToLineSegment(From, To, curBot.Pos()) < curBot.BRadius()) {
					//test to see if this is the closest so far
					double Dist = Vec2DDistanceSq(curBot.Pos(), m_vOrigin);

					if (Dist < ClosestSoFar) {
						Dist = ClosestSoFar;
						ClosestIntersectingBot = curBot;
					}
				}
			}

		}

		return ClosestIntersectingBot;
	}

//---------------------- GetListOfIntersectingBots ----------------------------
	protected List<Raven_Bot> GetListOfIntersectingBots(Vector2D From, Vector2D To) {
		//this will hold any bots that are intersecting with the line segment
		List<Raven_Bot> hits = new ArrayList<Raven_Bot>();

		//iterate through all entities checking against the line segment FromTo
		for (Raven_Bot curBot : m_pWorld.GetAllBots()) {
			//make sure we don't check against the shooter of the projectile
			if ((curBot.ID() != m_iShooterID)) {
				//if the distance to FromTo is less than the entities bounding radius then
				//there is an intersection so add it to hits
				if (DistToLineSegment(From, To, curBot.Pos()) < curBot.BRadius()) {
					hits.add(curBot);
				}
			}

		}

		return hits;
	}

	public Raven_Projectile(Vector2D target, //the target's position
			Raven_Game world, //a pointer to the world data
			int ShooterID, //the ID of the bot that fired this shot
			Vector2D origin, //the start position of the projectile
			Vector2D heading, //the heading of the projectile
			int damage, //how much damage it inflicts
			double scale,
			double MaxSpeed,
			double mass,
			double MaxForce) {
		super(origin,
				scale,
				new Vector2D(0, 0),
				MaxSpeed,
				heading,
				mass,
				new Vector2D(scale, scale),
				0, //max turn rate irrelevant here, all shots go straight
				MaxForce);

		m_vTarget = new Vector2D(target);
		m_bDead = false;
		m_bImpacted = false;
		m_pWorld = world;
		m_iDamageInflicted = damage;
		m_vOrigin = new Vector2D(origin);
		m_iShooterID = ShooterID;


		m_dTimeOfCreation = Clock.GetCurrentTime();
	}

	//unimportant for this class unless you want to implement a full state 
	//save/restore (which can be useful for debugging purposes)
	@Override
	public void Write(PrintStream os) {
	}

	@Override
	public void Read(InputStream is) {
	}

	//must be implemented
	@Override
	abstract public void Update();

	@Override
	abstract public void Render();

	/**
	 * set to true if the projectile has impacted and has finished any explosion
	 * sequence. When true the projectile will be removed from the game
	 */
	public boolean isDead() {
		return m_bDead;
	}

	/**
	 * true if the projectile has impacted but is not yet dead (because it may
	 * be exploding outwards from the point of impact for example)
	 */
	public boolean HasImpacted() {
		return m_bImpacted;
	}
}
