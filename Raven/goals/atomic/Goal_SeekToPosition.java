package Raven.goals.atomic;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_seek_to_position;
import static common.Debug.DbgConsole.debug_con;
import common.D2.Vector2D;
import static common.Time.CrudeTimer.Clock;
import static common.misc.Cgdi.gdi;

public class Goal_SeekToPosition extends Goal<Raven_Bot> {

    /**
     * the position the bot is moving to
     */
    private Vector2D m_vPosition;
    /**
     * the approximate time the bot should take to travel the target location
     */
    private double m_dTimeToReachPos;
    /**
     * this records the time this goal was activated
     */
    private double m_dStartTime;

    /**
     * returns true if the bot has taken longer than expected to reach the
     * currently active waypoint
     */
    private boolean isStuck() {
        double TimeTaken = Clock.GetCurrentTime() - m_dStartTime;

        if (TimeTaken > m_dTimeToReachPos) {
            debug_con.print("BOT ").print(m_pOwner.ID()).print(" IS STUCK!!").print("");
            return true;
        }

        return false;
    }

    //---------------------------- ctor -------------------------------------------
    //-----------------------------------------------------------------------------
    public Goal_SeekToPosition(Raven_Bot pBot, Vector2D target) {
        super(pBot, goal_seek_to_position);
        m_vPosition = new Vector2D(target);
        m_dTimeToReachPos = 0.0;
    }

    //the usual suspects
    @Override
    public void Activate() {
        m_iStatus = active;

        //record the time the bot starts this goal
        m_dStartTime = Clock.GetCurrentTime();

        //This value is used to determine if the bot becomes stuck 
        m_dTimeToReachPos = m_pOwner.CalculateTimeToReachPosition(m_vPosition);

        //factor in a margin of error for any reactive behavior
        final double MarginOfError = 1.0;

        m_dTimeToReachPos += MarginOfError;


        m_pOwner.GetSteering().SetTarget(m_vPosition);

        m_pOwner.GetSteering().SeekOn();
    }


    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        //test to see if the bot has become stuck
        if (isStuck()) {
            m_iStatus = failed;
        } //test to see if the bot has reached the waypoint. If so terminate the goal
        else {
            if (m_pOwner.isAtPosition(m_vPosition)) {
                m_iStatus = completed;
            }
        }

        return m_iStatus;
    }

    @Override
    public void Terminate() {
        m_pOwner.GetSteering().SeekOff();
        m_pOwner.GetSteering().ArriveOff();

        m_iStatus = completed;
    }

    @Override
    public void Render() {
        if (m_iStatus == active) {
            gdi.GreenBrush();
            gdi.BlackPen();
            gdi.Circle(m_vPosition, 3);
        } else if (m_iStatus == inactive) {

            gdi.RedBrush();
            gdi.BlackPen();
            gdi.Circle(m_vPosition, 3);
        }
    }
}