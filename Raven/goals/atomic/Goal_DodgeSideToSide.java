package Raven.goals.atomic;

/**
 * Author: Mat Buckland (ai-junkie.com)
 *
 * Desc: this goal makes the bot dodge from side to side
 *
 */
import static Raven.DEFINE.*;
import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_strafe;
import common.D2.Vector2D;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.RandBool;

public class Goal_DodgeSideToSide extends Goal<Raven_Bot> {

    private Vector2D m_vStrafeTarget = new Vector2D();
    private boolean m_bClockwise;

    //private Vector2D GetStrafeTarget()    const;
    public Goal_DodgeSideToSide(Raven_Bot pBot) {
        super(pBot, goal_strafe);
        m_bClockwise = RandBool();
    }

    @Override
    public void Activate() {
        m_iStatus = active;

        m_pOwner.GetSteering().SeekOn();


        if (m_bClockwise) {
            if (m_pOwner.canStepRight(m_vStrafeTarget)) {
                m_pOwner.GetSteering().SetTarget(m_vStrafeTarget);
            } else {
                //debug_con << "changing" << "";
                m_bClockwise = !m_bClockwise;
                m_iStatus = inactive;
            }
        } else {
            if (m_pOwner.canStepLeft(m_vStrafeTarget)) {
                m_pOwner.GetSteering().SetTarget(m_vStrafeTarget);
            } else {
                // debug_con << "changing" << "";
                m_bClockwise = !m_bClockwise;
                m_iStatus = inactive;
            }
        }


    }

    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        //if target goes out of view terminate
        if (!m_pOwner.GetTargetSys().isTargetWithinFOV()) {
            m_iStatus = completed;
        } //else if bot reaches the target position set status to inactive so the goal 
        //is reactivated on the next update-step
        else if (m_pOwner.isAtPosition(m_vStrafeTarget)) {
            m_iStatus = inactive;
        }

        return m_iStatus;
    }

    @Override
    public void Render() {
        //def(SHOW_TARGET);
        if (def(SHOW_TARGET)) {
            gdi.OrangePen();
            gdi.HollowBrush();

            gdi.Line(m_pOwner.Pos(), m_vStrafeTarget);
            gdi.Circle(m_vStrafeTarget, 3);
        }
    }

    @Override
    public void Terminate() {
        m_pOwner.GetSteering().SeekOff();
    }
}