/*
 *  Desc:   class to implement a bolt type projectile
 * 
 *  @author Petr (http://www.sallyx.org/)
 */
package Raven.armory.projectiles;

import Raven.Raven_Bot;
import static Raven.Raven_Messages.message_type.Msg_TakeThatMF;
import static Raven.lua.Raven_Scriptor.script;
import common.D2.Vector2D;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.sub;
import static common.D2.WallIntersectionTests.FindClosestPointOfIntersectionWithWalls;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import static common.misc.Cgdi.gdi;
import common.misc.CppToJava.DoubleRef;

public class Bolt extends Raven_Projectile {

    /**
     * tests the trajectory of the shell for an impact
     */
    //private void TestForImpact();

//-------------------------- ctor ---------------------------------------------
//-----------------------------------------------------------------------------
    public Bolt(Raven_Bot shooter, Vector2D target) {

        super(target,
                shooter.GetWorld(),
                shooter.ID(),
                shooter.Pos(),
                shooter.Facing(),
                script.GetInt("Bolt_Damage"),
                script.GetDouble("Bolt_Scale"),
                script.GetDouble("Bolt_MaxSpeed"),
                script.GetDouble("Bolt_Mass"),
                script.GetDouble("Bolt_MaxForce"));

        assert !target.equals(new Vector2D());
    }

//------------------------------ Update ---------------------------------------
//-----------------------------------------------------------------------------
    @Override
    public void Update() {
        if (!m_bImpacted) {
            m_vVelocity = mul(MaxSpeed(), Heading());

            //make sure vehicle does not exceed maximum velocity
            m_vVelocity.Truncate(m_dMaxSpeed);

            //update the position
            m_vPosition.add(m_vVelocity);


            //if the projectile has reached the target position or it hits an entity
            //or wall it should explode/inflict damage/whatever and then mark itself
            //as dead


            //test to see if the line segment connecting the bolt's current position
            //and previous position intersects with any bots.
            Raven_Bot hit = GetClosestIntersectingBot(sub(m_vPosition, m_vVelocity),
                    m_vPosition);

            //if hit
            if (hit != null) {
                m_bImpacted = true;
                m_bDead = true;

                //send a message to the bot to let it know it's been hit, and who the
                //shot came from
                Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                        m_iShooterID,
                        hit.ID(),
                        Msg_TakeThatMF,
                        new Integer(m_iDamageInflicted));
            }

            //test for impact with a wall
            DoubleRef dist = new DoubleRef(0.0);
            if (FindClosestPointOfIntersectionWithWalls(sub(m_vPosition, m_vVelocity),
                    m_vPosition,
                    dist,
                    m_vImpactPoint,
                    m_pWorld.GetMap().GetWalls())) {
                m_bDead = true;
                m_bImpacted = true;

                m_vPosition = m_vImpactPoint;
                
                return;
            }
        }
    }

    //-------------------------- Render -------------------------------------------
    //-----------------------------------------------------------------------------
    @Override
    public void Render() {
        gdi.ThickGreenPen();
        gdi.Line(Pos(), sub(Pos(), Velocity()));
    }
}