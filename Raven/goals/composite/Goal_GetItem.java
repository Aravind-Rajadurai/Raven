package Raven.goals.composite;

import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_health;
import static Raven.Raven_ObjectEnumerations.type_rail_gun;
import static Raven.Raven_ObjectEnumerations.type_rocket_launcher;
import static Raven.Raven_ObjectEnumerations.type_shotgun;
import static Raven.goals.Raven_Goal_Types.goal_get_health;
import static Raven.goals.Raven_Goal_Types.goal_get_railgun;
import static Raven.goals.Raven_Goal_Types.goal_get_rocket_launcher;
import static Raven.goals.Raven_Goal_Types.goal_get_shotgun;
import Raven.goals.atomic.Goal_Wander;
import common.Messaging.Telegram;
import common.Triggers.Trigger;

public class Goal_GetItem extends Goal_Composite<Raven_Bot> {

    private int m_iItemToGet;
    private Trigger<Raven_Bot> m_pGiverTrigger;
    /**
     * true if a path to the item has been formulated
     */
    private boolean m_bFollowingPath;

    /**
     * returns true if the bot sees that the item it is heading for has been
     * picked up by an opponent
     */
    public boolean hasItemBeenStolen() {
        if (m_pGiverTrigger != null
                && !m_pGiverTrigger.isActive()
                && m_pOwner.hasLOSto(m_pGiverTrigger.Pos())) {
            return true;
        }

        return false;
    }

    public Goal_GetItem(Raven_Bot pBot, int item) {
        super(pBot, ItemTypeToGoalType(item));
        m_iItemToGet = item;
        m_pGiverTrigger = null;
        m_bFollowingPath = false;
    }

    @Override
    public void Activate() {
        m_iStatus = active;

        m_pGiverTrigger = null;

        //request a path to the item
        m_pOwner.GetPathPlanner().RequestPathToItem(m_iItemToGet);

        //the bot may have to wait a few update cycles before a path is calculated
        //so for appearances sake it just wanders
        AddSubgoal(new Goal_Wander(m_pOwner));

    }

    @Override
    public int Process() {
        ActivateIfInactive();

        if (hasItemBeenStolen()) {
            Terminate();
        } else {
            //process the subgoals
            m_iStatus = ProcessSubgoals();
        }

        return m_iStatus;
    }

    @Override
    public boolean HandleMessage(final Telegram msg) {
        //first, pass the message down the goal hierarchy
        boolean bHandled = ForwardMessageToFrontMostSubgoal(msg);

        //if the msg was not handled, test to see if this goal can handle it
        if (bHandled == false) {
            switch (msg.Msg) {
                case Msg_PathReady:

                    //clear any existing goals
                    RemoveAllSubgoals();

                    AddSubgoal(new Goal_FollowPath(m_pOwner,
                            m_pOwner.GetPathPlanner().GetPath()));

                    //get the pointer to the item
                    //m_pGiverTrigger = static_cast < Raven_Map::TriggerType * > (msg.ExtraInfo);
                    m_pGiverTrigger = (Trigger<Raven_Bot>) (msg.ExtraInfo);

                    return true; //msg handled


                case Msg_NoPathAvailable:
                    m_iStatus = failed;

                    return true; //msg handled

                default:
                    return false;
            }
        }

        //handled by subgoals
        return true;
    }

    @Override
    public void Terminate() {
		RemoveAllSubgoals();
        m_iStatus = completed;
    }

    /**
     * helper function to change an item type enumeration into a goal type
     */
    protected static int ItemTypeToGoalType(int gt) {
        switch (gt) {
            case type_health:

                return goal_get_health;

            case type_shotgun:

                return goal_get_shotgun;

            case type_rail_gun:

                return goal_get_railgun;

            case type_rocket_launcher:

                return goal_get_rocket_launcher;

            default:
                throw new RuntimeException("Goal_GetItem cannot determine item type");

        }//end switch
    }
}
