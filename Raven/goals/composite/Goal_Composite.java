package Raven.goals.composite;

import Raven.Raven_Bot;
import Raven.goals.atomic.Goal;
import common.D2.Vector2D;
import common.Messaging.Telegram;
import static common.misc.Cgdi.gdi;
import common.misc.TypeToString;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: Base composite goal class
 */
public abstract class Goal_Composite<entity_type extends Raven_Bot> extends Goal<entity_type> {

    //private    typedef std::list<Goal<entity_type>* > SubgoalList;
    protected class SubgoalList extends LinkedList<Goal<entity_type>> {
    }
    /**
     * composite goals may have any number of subgoals
     */
    protected SubgoalList m_SubGoals = new SubgoalList();

    /**
     * processes any subgoals that may be present
     *
     * this method first removes any completed goals from the front of the
     * subgoal list. It then processes the next goal in the list (if there is
     * one)
     */
    protected int ProcessSubgoals() {
        //remove all completed and failed goals from the front of the subgoal list
        while (!m_SubGoals.isEmpty()
                && (m_SubGoals.getFirst().isComplete() || m_SubGoals.getFirst().hasFailed())) {
            m_SubGoals.getFirst().Terminate();
            m_SubGoals.removeFirst();
        }

        //if any subgoals remain, process the one at the front of the list
        if (!m_SubGoals.isEmpty()) {
            //grab the status of the front-most subgoal
            int StatusOfSubGoals = m_SubGoals.getFirst().Process();

            //we have to test for the special case where the front-most subgoal
            //reports 'completed' *and* the subgoal list contains additional goals.When
            //this is the case, to ensure the parent keeps processing its subgoal list
            //we must return the 'active' status.
            if (StatusOfSubGoals == completed && m_SubGoals.size() > 1) {
                return active;
            }

            return StatusOfSubGoals;
        } //no more subgoals to process - return 'completed'
        else {
            return completed;
        }
    }

    /**
     * passes the message to the goal at the front of the queue
     */
    protected boolean ForwardMessageToFrontMostSubgoal(final Telegram msg) {
        if (!m_SubGoals.isEmpty()) {
            return m_SubGoals.getFirst().HandleMessage(msg);
        }

        //return false if the message has not been handled
        return false;
    }

    public Goal_Composite(entity_type pE, int type) {
        super(pE, type);
    }

    /**
     * when this object is destroyed make sure any subgoals are terminated and
     * destroyed.
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        RemoveAllSubgoals();
    }

    /**
     * logic to run when the goal is activated.
     */
    @Override
    public abstract void Activate();

    /**
     * logic to run each update-step.
     */
    @Override
    public abstract int Process();

    /**
     * logic to run prior to the goal's destruction
     */
    @Override
    public abstract void Terminate();

    /**
     * if a child class of Goal_Composite does not define a message handler the
     * default behavior is to forward the message to the front-most subgoal
     */
    @Override
    public boolean HandleMessage(final Telegram msg) {
        return ForwardMessageToFrontMostSubgoal(msg);
    }

    /**
     * adds a subgoal to the front of the subgoal list
     */
    @Override
    public void AddSubgoal(Goal<entity_type> g) {
        //add the new goal to the front of the list
        m_SubGoals.addFirst(g);
    }

    /**
     * this method iterates through the subgoals and calls each one's Terminate
     * method before deleting the subgoal and removing it from the subgoal list
     */
    public void RemoveAllSubgoals() {
        Iterator<Goal<entity_type>> it = m_SubGoals.iterator();
        while (it.hasNext()) {
            it.next().Terminate();
            //it.remove();
        }

        m_SubGoals.clear();
    }

    @Override
    public void RenderAtPos(Vector2D pos, TypeToString tts) {
        super.RenderAtPos(pos, tts);

        pos.x += 10;

        gdi.TransparentText();

        ListIterator<Goal<entity_type>> it = m_SubGoals.listIterator(m_SubGoals.size());
        while (it.hasPrevious()) {
            it.previous().RenderAtPos(pos, tts);
        }

        pos.x -= 10;
    }

    /**
     * this is only used to render information for debugging purposes
     */
    @Override
    public void Render() {
        if (!m_SubGoals.isEmpty()) {
            m_SubGoals.getFirst().Render();
        }
    }
}