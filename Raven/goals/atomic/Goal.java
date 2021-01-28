package Raven.goals.atomic;

import Raven.Raven_Bot;
import common.D2.Vector2D;
import common.Messaging.Telegram;
import static common.misc.Cgdi.gdi;
import common.misc.TypeToString;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 *
 * Desc: Base goal class.
 */
abstract public class Goal<entity_type extends Raven_Bot> {

    public static final int active = 0;
    public static final int inactive = 1;
    public static final int completed = 2;
    public static final int failed = 3;
    /**
     * an enumerated type specifying the type of goal
     */
    protected int m_iType;
    /**
     * a pointer to the entity that owns this goal
     */
    protected entity_type m_pOwner;
    /**
     * an enumerated value indicating the goal's status (active, inactive,
     * completed, failed)
     */
    protected int m_iStatus;


    /* the following methods were created to factor out some of the commonality
     in the implementations of the Process method() */
    /**
     * if m_iStatus = inactive this method sets it to active and calls
     * Activate()
     */
    protected void ActivateIfInactive() {
        if (isInactive()) {
            Activate();
        }
    }

    //if m_iStatus is failed this method sets it to inactive so that the goal
    //will be reactivated (and therefore re-planned) on the next update-step.
    protected void ReactivateIfFailed() {
        if (hasFailed()) {
            m_iStatus = inactive;
        }
    }

    /**
     * note how goals start off in the inactive state
     */
    public Goal(entity_type pE, int type) {
        m_iType = type;
        m_pOwner = pE;
        m_iStatus = inactive;
    }

    //virtual ~Goal(){}
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * logic to run when the goal is activated.
     */
    public abstract void Activate();

    /**
     * logic to run each update-step
     */
    public abstract int Process();

    /**
     * logic to run when the goal is satisfied. (typically used to switch off
     * any active steering behaviors)
     */
    public abstract void Terminate();

    /**
     * goals can handle messages. Many don't though, so this defines a default
     * behavior
     */
    public boolean HandleMessage(final Telegram msg) {
        return false;
    }

    /**
     * a Goal is atomic and cannot aggregate subgoals yet we must implement this
     * method to provide the uniform interface required for the goal hierarchy.
     */
    public void AddSubgoal(Goal<entity_type> g) {
        throw new RuntimeException("Cannot add goals to atomic goals");
    }

    public boolean isComplete() {
        return m_iStatus == completed;
    }

    public boolean isActive() {
        return m_iStatus == active;
    }

    public boolean isInactive() {
        return m_iStatus == inactive;
    }

    public boolean hasFailed() {
        return m_iStatus == failed;
    }

    public int GetType() {
        return m_iType;
    }

    /**
     * this is used to draw the name of the goal at the specific position used
     * for debugging
     */
    public void RenderAtPos(Vector2D pos, TypeToString tts) {
        pos.y += 15;
        gdi.TransparentText();
        if (isComplete()) {
            gdi.TextColor(0, 255, 0);
        }
        if (isInactive()) {
            gdi.TextColor(0, 0, 0);
        }
        if (hasFailed()) {
            gdi.TextColor(255, 0, 0);
        }
        if (isActive()) {
            gdi.TextColor(0, 0, 255);
        }

        gdi.TextAtPos(pos.x, pos.y, tts.Convert(GetType()));
    }

    /**
     * used to render any goal specific information
     */
    public void Render() {
    }
}