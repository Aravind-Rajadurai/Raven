/*
 *  Desc:   class to implement a rocket
 * 
 *  @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.projectiles;

import Raven.Raven_Bot;
import static Raven.Raven_Messages.message_type.Msg_TakeThatMF;
import static Raven.lua.Raven_Scriptor.script;
import common.D2.Vector2D;
import static common.D2.Vector2D.Vec2DDistance;
import static common.D2.Vector2D.Vec2DDistanceSq;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.sub;
import static common.D2.WallIntersectionTests.FindClosestPointOfIntersectionWithWalls;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import static common.misc.Cgdi.gdi;
import common.misc.CppToJava.DoubleRef;

public class Rocket extends Raven_Projectile {

    /**
     * the radius of damage, once the rocket has impacted
     */
    private double m_dBlastRadius;
    /**
     * this is used to render the splash when the rocket impacts
     */
    double m_dCurrentBlastRadius;

    /**
     * If the rocket has impacted we test all bots to see if they are within the
     * blast radius and reduce their health accordingly
     */
    private void InflictDamageOnBotsWithinBlastRadius() {
        for (Raven_Bot curBot : m_pWorld.GetAllBots()) {
            if (Vec2DDistance(Pos(), curBot.Pos()) < m_dBlastRadius + curBot.BRadius()) {
                //send a message to the bot to let it know it's been hit, and who the
                //shot came from
                Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                        m_iShooterID,
                        curBot.ID(),
                        Msg_TakeThatMF,
                        new Integer(m_iDamageInflicted));

            }
        }
    }

    /**
     * tests the trajectory of the shell for an impact
     */
    private void TestForImpact() {

        //if the projectile has reached the target position or it hits an entity
        //or wall it should explode/inflict damage/whatever and then mark itself
        //as dead


        //test to see if the line segment connecting the rocket's current position
        //and previous position intersects with any bots.
        Raven_Bot hit = GetClosestIntersectingBot(sub(m_vPosition, m_vVelocity), m_vPosition);

        //if hit
        if (hit != null) {
            m_bImpacted = true;

            //send a message to the bot to let it know it's been hit, and who the
            //shot came from
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    m_iShooterID,
                    hit.ID(),
                    Msg_TakeThatMF,
                    new Integer(m_iDamageInflicted));

            //test for bots within the blast radius and inflict damage
            InflictDamageOnBotsWithinBlastRadius();
        }

        //test for impact with a wall
        DoubleRef dist = new DoubleRef(0.0);
        if (FindClosestPointOfIntersectionWithWalls(sub(m_vPosition, m_vVelocity),
                m_vPosition,
                dist,
                m_vImpactPoint,
                m_pWorld.GetMap().GetWalls())) {
            m_bImpacted = true;

            //test for bots within the blast radius and inflict damage
            InflictDamageOnBotsWithinBlastRadius();

            m_vPosition = m_vImpactPoint;

            return;
        }

        //test to see if rocket has reached target position. If so, test for
        //all bots in vicinity
        final double tolerance = 5.0;
        if (Vec2DDistanceSq(Pos(), m_vTarget) < tolerance * tolerance) {
            m_bImpacted = true;

            InflictDamageOnBotsWithinBlastRadius();
        }
    }

    //-------------------------- ctor ---------------------------------------------
    //-----------------------------------------------------------------------------
    public Rocket(Raven_Bot shooter, Vector2D target) {

        super(target,
                shooter.GetWorld(),
                shooter.ID(),
                shooter.Pos(),
                shooter.Facing(),
                script.GetInt("Rocket_Damage"),
                script.GetDouble("Rocket_Scale"),
                script.GetDouble("Rocket_MaxSpeed"),
                script.GetDouble("Rocket_Mass"),
                script.GetDouble("Rocket_MaxForce"));

        m_dCurrentBlastRadius = 0.0;
        m_dBlastRadius = script.GetDouble("Rocket_BlastRadius");
        assert (!target.equals(new Vector2D()));
    }

    //------------------------------ Update ---------------------------------------
    @Override
    public void Update() {
        if (!m_bImpacted) {
            m_vVelocity = mul(MaxSpeed(), Heading());

            //make sure vehicle does not exceed maximum velocity
            m_vVelocity.Truncate(m_dMaxSpeed);

            //update the position
            m_vPosition.add(m_vVelocity);

            TestForImpact();
        } else {
            m_dCurrentBlastRadius += script.GetDouble("Rocket_ExplosionDecayRate");

            //when the rendered blast circle becomes equal in size to the blast radius
            //the rocket can be removed from the game
            if (m_dCurrentBlastRadius > m_dBlastRadius) {
                m_bDead = true;
            }
        }
    }

    //-------------------------- Render -------------------------------------------
    //-----------------------------------------------------------------------------
    @Override
    public void Render() {

        gdi.RedPen();
        gdi.OrangeBrush();
        gdi.Circle(Pos(), 2);

        if (m_bImpacted) {
            gdi.HollowBrush();
            gdi.Circle(Pos(), m_dCurrentBlastRadius);
        }
    }
}
