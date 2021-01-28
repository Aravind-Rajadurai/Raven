package Raven.goals.atomic;

import Raven.Raven_Bot;
import static Raven.goals.Raven_Goal_Types.goal_traverse_edge;
import static Raven.lua.Raven_Scriptor.script;
import Raven.navigation.PathEdge;
import static common.Debug.DbgConsole.debug_con;
import common.Graph.GraphEdgeTypes.NavGraphEdge;
import static common.misc.Cgdi.gdi;
import static common.Time.CrudeTimer.Clock;

public class Goal_TraverseEdge extends Goal<Raven_Bot> {

    /**
     * the edge the bot will follow
     */
    private PathEdge m_Edge;
    /**
     * true if m_Edge is the last in the path.
     */
    private boolean m_bLastEdgeInPath;
    /**
     * the estimated time the bot should take to traverse the edge
     */
    private double m_dTimeExpected;
    /**
     * this records the time this goal was activated
     */
    private double m_dStartTime;

    /**
     * returns true if the bot has taken longer than expected to reach the
     * currently active waypoint
     */
    public boolean isStuck() {
        double TimeTaken = Clock.GetCurrentTime() - m_dStartTime;

        if (TimeTaken > m_dTimeExpected) {
            debug_con.print("BOT ").print(m_pOwner.ID()).print(" IS STUCK!!").print("");

            return true;
        }

        return false;
    }

    //---------------------------- ctor -------------------------------------------
    //-----------------------------------------------------------------------------
    public Goal_TraverseEdge(Raven_Bot pBot,
            PathEdge edge,
            boolean LastEdge) {

        super(pBot, goal_traverse_edge);
        m_Edge = new PathEdge(edge);
        m_dTimeExpected = 0.0;
        m_bLastEdgeInPath = LastEdge;

    }

    //the usual suspects
    @Override
    public void Activate() {
        m_iStatus = active;

        //the edge behavior flag may specify a type of movement that necessitates a 
        //change in the bot's max possible speed as it follows this edge
        switch (m_Edge.Behavior()) {
            case NavGraphEdge.swim: {
                m_pOwner.SetMaxSpeed(script.GetDouble("Bot_MaxSwimmingSpeed"));
            }

            break;

            case NavGraphEdge.crawl: {
                m_pOwner.SetMaxSpeed(script.GetDouble("Bot_MaxCrawlingSpeed"));
            }

            break;
        }


        //record the time the bot starts this goal
        m_dStartTime = Clock.GetCurrentTime();

        //calculate the expected time required to reach the this waypoint. This value
        //is used to determine if the bot becomes stuck 
        m_dTimeExpected = m_pOwner.CalculateTimeToReachPosition(m_Edge.Destination());

        //factor in a margin of error for any reactive behavior
        final double MarginOfError = 2.0;

        m_dTimeExpected += MarginOfError;


        //set the steering target
        m_pOwner.GetSteering().SetTarget(m_Edge.Destination());

        //Set the appropriate steering behavior. If this is the last edge in the path
        //the bot should arrive at the position it points to, else it should seek
        if (m_bLastEdgeInPath) {
            m_pOwner.GetSteering().ArriveOn();
        } else {
            m_pOwner.GetSteering().SeekOn();
        }
    }

    @Override
    public int Process() {
        //if status is inactive, call Activate()
        ActivateIfInactive();

        //if the bot has become stuck return failure
        if (isStuck()) {
            m_iStatus = failed;
        } //if the bot has reached the end of the edge return completed
        else {
            if (m_pOwner.isAtPosition(m_Edge.Destination())) {
                m_iStatus = completed;
            }
        }

        return m_iStatus;
    }

    @Override
    public void Terminate() {
        //turn off steering behaviors.
        m_pOwner.GetSteering().SeekOff();
        m_pOwner.GetSteering().ArriveOff();

        //return max speed back to normal
        m_pOwner.SetMaxSpeed(script.GetDouble("Bot_MaxSpeed"));
    }

    @Override
    public void Render() {
        if (m_iStatus == active) {
            gdi.BluePen();
            gdi.Line(m_pOwner.Pos(), m_Edge.Destination());
            gdi.GreenBrush();
            gdi.BlackPen();
            gdi.Circle(m_Edge.Destination(), 3);
        }
    }
}