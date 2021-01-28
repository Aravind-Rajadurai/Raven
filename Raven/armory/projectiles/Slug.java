/*
 *  Desc:   class to implement a railgun slug
 * 
 *  @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.projectiles;

import Raven.Raven_Bot;
import static Raven.Raven_Messages.message_type.Msg_TakeThatMF;
import static Raven.lua.Raven_Scriptor.script;
import common.D2.Vector2D;
import static common.D2.Vector2D.Vec2DNormalize;
import static common.D2.Vector2D.div;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.sub;
import static common.D2.WallIntersectionTests.FindClosestPointOfIntersectionWithWalls;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import static common.Time.CrudeTimer.Clock;
import static common.misc.Cgdi.gdi;
import common.misc.CppToJava.DoubleRef;
import java.util.List;

public class Slug extends Raven_Projectile {

    /**
     * when this projectile hits something it's trajectory is rendered for this
     * amount of time
     */
    private double m_dTimeShotIsVisible;

    /**
     * tests the trajectory of the shell for an impact
     */
    private void TestForImpact() {
        // a rail gun slug travels VERY fast. It only gets the chance to update once 
        m_bImpacted = true;

        //first find the closest wall that this ray intersects with. Then we
        //can test against all entities within this range.
        DoubleRef DistToClosestImpact = new DoubleRef(0.0);
        FindClosestPointOfIntersectionWithWalls(m_vOrigin,
                m_vPosition,
                DistToClosestImpact,
                m_vImpactPoint,
                m_pWorld.GetMap().GetWalls());

        //test to see if the ray between the current position of the slug and 
        //the start position intersects with any bots.
        List<Raven_Bot> hits = GetListOfIntersectingBots(m_vOrigin, m_vPosition);

        //if no bots hit just return;
        if (hits.isEmpty()) {
            return;
        }

        //give some damage to the hit bots
        for (Raven_Bot it : hits) {
            {
                //send a message to the bot to let it know it's been hit, and who the
                //shot came from
                Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                        m_iShooterID,
                        it.ID(),
                        Msg_TakeThatMF,
                        new Integer(m_iDamageInflicted));

            }
        }
    }

    /**
     * returns true if the shot is still to be rendered
     */
    private boolean isVisibleToPlayer() {
        return Clock.GetCurrentTime() < m_dTimeOfCreation + m_dTimeShotIsVisible;
    }

    public Slug(Raven_Bot shooter, Vector2D target) {

        super(target,
                shooter.GetWorld(),
                shooter.ID(),
                shooter.Pos(),
                shooter.Facing(),
                script.GetInt("Slug_Damage"),
                script.GetDouble("Slug_Scale"),
                script.GetDouble("Slug_MaxSpeed"),
                script.GetDouble("Slug_Mass"),
                script.GetDouble("Slug_MaxForce"));

        m_dTimeShotIsVisible = script.GetDouble("Slug_Persistance");
    }

    //-------------------------- Render -------------------------------------------
//-----------------------------------------------------------------------------
    @Override
    public void Render() {
        if (isVisibleToPlayer() && m_bImpacted) {
            gdi.GreenPen();
            gdi.Line(m_vOrigin, m_vImpactPoint);
        }
    }

    @Override
    public void Update() {
        if (!HasImpacted()) {
            //calculate the steering force
            Vector2D DesiredVelocity = mul(Vec2DNormalize(sub(m_vTarget, Pos())), MaxSpeed());

            Vector2D sf = sub(DesiredVelocity, Velocity());

            //update the position
            Vector2D accel = div(sf, m_dMass);

            m_vVelocity.add(accel);

            //make sure the slug does not exceed maximum velocity
            m_vVelocity.Truncate(m_dMaxSpeed);

            //update the position
            m_vPosition.add(m_vVelocity);

            TestForImpact();
        } else if (!isVisibleToPlayer()) {
            m_bDead = true;
        }

    }
}