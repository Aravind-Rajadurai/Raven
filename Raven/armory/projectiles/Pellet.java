/**
 * Desc: class to implement a pellet type projectile
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.projectiles;

import Raven.Raven_Bot;
import static Raven.Raven_Messages.message_type.Msg_TakeThatMF;
import static Raven.lua.Raven_Scriptor.script;
import common.D2.Vector2D;
import static common.D2.geometry.GetLineSegmentCircleClosestIntersectionPoint;
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

public class Pellet extends Raven_Projectile {

    /**
     * when this projectile hits something it's trajectory is rendered for this
     * amount of time
     */
    private double m_dTimeShotIsVisible;

    /**
     * tests the trajectory of the pellet for an impact
     */
    private void TestForImpact() {
        //a shot gun shell is an instantaneous projectile so it only gets the chance
        //to update once 
        m_bImpacted = true;

        //first find the closest wall that this ray intersects with. Then we
        //can test against all entities within this range.
        DoubleRef DistToClosestImpact = new DoubleRef(0.0);
        FindClosestPointOfIntersectionWithWalls(m_vOrigin,
                m_vPosition,
                DistToClosestImpact,
                m_vImpactPoint,
                m_pWorld.GetMap().GetWalls());

        //test to see if the ray between the current position of the shell and 
        //the start position intersects with any bots.
        Raven_Bot hit = GetClosestIntersectingBot(m_vOrigin, m_vImpactPoint);

        //if no bots hit just return;
        if (hit == null) {
            return;
        }

        //determine the impact point with the bot's bounding circle so that the
        //shell can be rendered properly
        GetLineSegmentCircleClosestIntersectionPoint(m_vOrigin,
                m_vImpactPoint,
                hit.Pos(),
                hit.BRadius(),
                m_vImpactPoint);

        //send a message to the bot to let it know it's been hit, and who the
        //shot came from
        Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                m_iShooterID,
                hit.ID(),
                Msg_TakeThatMF,
                new Integer(m_iDamageInflicted));
    }

    /**
     * returns true if the shot is still to be rendered
     */
    private boolean isVisibleToPlayer() {
        return Clock.GetCurrentTime() < m_dTimeOfCreation + m_dTimeShotIsVisible;
    }

    //-------------------------- ctor ---------------------------------------------
    //-----------------------------------------------------------------------------
    public Pellet(Raven_Bot shooter, Vector2D target) {

        super(target,
                shooter.GetWorld(),
                shooter.ID(),
                shooter.Pos(),
                shooter.Facing(),
                script.GetInt("Pellet_Damage"),
                script.GetDouble("Pellet_Scale"),
                script.GetDouble("Pellet_MaxSpeed"),
                script.GetDouble("Pellet_Mass"),
                script.GetDouble("Pellet_MaxForce"));

        m_dTimeShotIsVisible = script.GetDouble("Pellet_Persistance");
    }

    //------------------------------ Update ---------------------------------------
    @Override
    public void Update() {
        if (!HasImpacted()) {
            //calculate the steering force
            Vector2D DesiredVelocity = mul(Vec2DNormalize(sub(m_vTarget, Pos())), MaxSpeed());

            Vector2D sf = sub(DesiredVelocity, Velocity());

            //update the position
            Vector2D accel = div(sf, m_dMass);

            m_vVelocity.add(accel);

            //make sure vehicle does not exceed maximum velocity
            m_vVelocity.Truncate(m_dMaxSpeed);

            //update the position
            m_vPosition.add(m_vVelocity);

            TestForImpact();
        } else if (!isVisibleToPlayer()) {
            m_bDead = true;
        }
    }

    //-------------------------- Render -------------------------------------------
    @Override
    public void Render() {
        if (isVisibleToPlayer() && m_bImpacted) {
            gdi.YellowPen();
            gdi.Line(m_vOrigin, m_vImpactPoint);

            gdi.BrownBrush();
            gdi.Circle(m_vImpactPoint, 3);
        }
    }
}