/*
 * 
 *  @author Petr (http://www.sallyx.org/)
 */
package Raven;

import common.D2.Vector2D;
import static common.D2.Vector2D.isSecondInFOVOfFirst;
import static common.Time.CrudeTimer.Clock;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.MaxDouble;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

class MemoryRecord {

    /**
     * records the time the opponent was last sensed (seen or heard). This is
     * used to determine if a bot can 'remember' this record or not. (if
     * CurrentTime() - m_dTimeLastSensed is greater than the bot's memory span,
     * the data in this record is made unavailable to clients)
     */
    public double fTimeLastSensed;
    /**
     * it can be useful to know how long an opponent has been visible. This
     * variable is tagged with the current time whenever an opponent first
     * becomes visible. It's then a simple matter to calculate how long the
     * opponent has been in view (CurrentTime - fTimeBecameVisible)
     */
    public double fTimeBecameVisible;
    /**
     * it can also be useful to know the last time an opponent was seen
     */
    public double fTimeLastVisible;
    /**
     * a vector marking the position where the opponent was last sensed. This
     * can be used to help hunt down an opponent if it goes out of view
     */
    public Vector2D vLastSensedPosition = new Vector2D();
    /**
     * set to true if opponent is within the field of view of the owner
     */
    public boolean bWithinFOV;
    /**
     * set to true if there is no obstruction between the opponent and the
     * owner, permitting a shot.
     */
    public boolean bShootable;

    public MemoryRecord() {
        fTimeLastSensed = -999;
        fTimeBecameVisible = -999;
        fTimeLastVisible = 0;
        bWithinFOV = false;
        bShootable = false;
    }
}

public class Raven_SensoryMemory {

    private class MemoryMap extends HashMap<Raven_Bot, MemoryRecord> {
    }
    /**
     * the owner of this instance
     */
    private Raven_Bot m_pOwner;
    /**
     * this container is used to simulate memory of sensory events. A
     * MemoryRecord is created for each opponent in the environment. Each record
     * is updated whenever the opponent is encountered. (when it is seen or
     * heard)
     */
    private MemoryMap m_MemoryMap = new MemoryMap();
    /**
     * a bot has a memory span equivalent to this value. When a bot requests a
     * list of all recently sensed opponents this value is used to determine if
     * the bot is able to remember an opponent or not.
     */
    private double m_dMemorySpan;

    /**
     * this methods checks to see if there is an existing record for pBot. If
     * not a new MemoryRecord record is made and added to the memory map.(called
     * by UpdateWithSoundSource & UpdateVision)
     */
    private void MakeNewRecordIfNotAlreadyPresent(Raven_Bot pOpponent) {
        //else check to see if this Opponent already exists in the memory. If it doesn't,
        //create a new record
        if (m_MemoryMap.get(pOpponent) == null) {
            m_MemoryMap.put(pOpponent, new MemoryRecord());
        }
    }

    public Raven_SensoryMemory(Raven_Bot owner, double MemorySpan) {
        m_pOwner = owner;
        m_dMemorySpan = MemorySpan;
    }

    /**
     * this method is used to update the memory map whenever an opponent makes a
     * noise
     *
     * this updates the record for an individual opponent. Note, there is no
     * need to test if the opponent is within the FOV because that test will be
     * done when the UpdateVision method is called
     */
    public void UpdateWithSoundSource(Raven_Bot pNoiseMaker) {
        //make sure the bot being examined is not this bot
        if (m_pOwner != pNoiseMaker) {
            //if the bot is already part of the memory then update its data, else
            //create a new memory record and add it to the memory
            MakeNewRecordIfNotAlreadyPresent(pNoiseMaker);

            MemoryRecord info = m_MemoryMap.get(pNoiseMaker);

            //test if there is LOS between bots 
            if (m_pOwner.GetWorld().isLOSOkay(m_pOwner.Pos(), pNoiseMaker.Pos())) {
                info.bShootable = true;

                //record the position of the bot
                info.vLastSensedPosition = pNoiseMaker.Pos();
            } else {
                info.bShootable = false;
            }

            //record the time it was sensed
            info.fTimeLastSensed = (double) Clock.GetCurrentTime();
        }
    }

    /**
     * this removes a bot's record from memory
     */
    public void RemoveBotFromMemory(Raven_Bot pBot) {
        m_MemoryMap.remove(pBot);
    }

    /**
     * this method iterates through all the opponents in the game world and
     * updates the records of those that are in the owner's FOV this method
     * iterates through all the bots in the game world to test if they are in
     * the field of view. Each bot's memory record is updated accordingly
     */
    public void UpdateVision() {
        //for each bot in the world test to see if it is visible to the owner of
        //this class
        final List<Raven_Bot> bots = m_pOwner.GetWorld().GetAllBots();
        for (Raven_Bot curBot : bots) {
            //make sure the bot being examined is not this bot
            if (m_pOwner != curBot) {
                //make sure it is part of the memory map
                MakeNewRecordIfNotAlreadyPresent(curBot);

                //get a reference to this bot's data
                MemoryRecord info = m_MemoryMap.get(curBot);

                //test if there is LOS between bots 
                if (m_pOwner.GetWorld().isLOSOkay(m_pOwner.Pos(), curBot.Pos())) {
                    info.bShootable = true;

                    //test if the bot is within FOV
                    if (isSecondInFOVOfFirst(m_pOwner.Pos(),
                            m_pOwner.Facing(),
                            curBot.Pos(),
                            m_pOwner.FieldOfView())) {
                        info.fTimeLastSensed = Clock.GetCurrentTime();
                        info.vLastSensedPosition = curBot.Pos();
                        info.fTimeLastVisible = Clock.GetCurrentTime();

                        if (info.bWithinFOV == false) {
                            info.bWithinFOV = true;
                            info.fTimeBecameVisible = info.fTimeLastSensed;
                        }
                    } else {
                        info.bWithinFOV = false;
                    }
                } else {
                    info.bShootable = false;
                    info.bWithinFOV = false;
                }
            }
        }//next bot
    }

    /**
     * returns true if the bot given as a parameter can be shot (ie. its not
     * obscured by walls)
     */
    public boolean isOpponentShootable(Raven_Bot pOpponent) {
        MemoryRecord mr = m_MemoryMap.get(pOpponent);
        if (mr != null) {
            return mr.bShootable;
        }

        return false;
    }

    /**
     * returns true if the bot given as a parameter is within FOV
     */
    public boolean isOpponentWithinFOV(Raven_Bot pOpponent) {
        MemoryRecord mr = m_MemoryMap.get(pOpponent);
        if (mr != null) {
            return mr.bWithinFOV;
        }

        return false;
    }

    /**
     * returns the last recorded position of the bot
     */
    public Vector2D GetLastRecordedPositionOfOpponent(Raven_Bot pOpponent) {
        MemoryRecord mr = m_MemoryMap.get(pOpponent);
        if (mr != null) {
            return new Vector2D(mr.vLastSensedPosition);
        }
        throw new RuntimeException("< Raven_SensoryMemory::GetLastRecordedPositionOfOpponent>: Attempting to get position of unrecorded bot");
    }

    /**
     * returns the amount of time the given bot has been visible
     */
    public double GetTimeOpponentHasBeenVisible(Raven_Bot pOpponent) {
        MemoryRecord mr = m_MemoryMap.get(pOpponent);
        if (mr != null && mr.bWithinFOV) {
            return Clock.GetCurrentTime() - mr.fTimeBecameVisible;
        }

        return 0;
    }

    /**
     * returns the amount of time the given bot has been visible
     */
    public double GetTimeSinceLastSensed(Raven_Bot pOpponent) {
        MemoryRecord mr = m_MemoryMap.get(pOpponent);
        if (mr != null && mr.bWithinFOV) {
            return Clock.GetCurrentTime() - mr.fTimeLastSensed;
        }
        return 0;
    }

    /**
     * returns the amount of time the given opponent has remained out of view
     * returns a high value if opponent has never been seen or not present
     */
    public double GetTimeOpponentHasBeenOutOfView(Raven_Bot pOpponent) {
        MemoryRecord mr = m_MemoryMap.get(pOpponent);
        if (mr != null) {

            return Clock.GetCurrentTime() - mr.fTimeLastVisible;
        }

        return MaxDouble;
    }

    /**
     * this method returns a list of all the opponents that have had their
     * records updated within the last m_dMemorySpan seconds.
     *
     * @return a list of the bots that have been sensed recently
     */
    public List<Raven_Bot> GetListOfRecentlySensedOpponents() {
        //this will store all the opponents the bot can remember
        List<Raven_Bot> opponents = new ArrayList<Raven_Bot>();

        double CurrentTime = Clock.GetCurrentTime();

        for (Entry<Raven_Bot, MemoryRecord> curRecord : m_MemoryMap.entrySet()) {
            //if this bot has been updated in the memory recently, add to list
            if ((CurrentTime - curRecord.getValue().fTimeLastSensed) <= m_dMemorySpan) {
                opponents.add(curRecord.getKey());
            }
        }

        return opponents;
    }

    /**
     * renders boxes around the opponents it has sensed recently.
     */
    public void RenderBoxesAroundRecentlySensed() {
        List<Raven_Bot> opponents = GetListOfRecentlySensedOpponents();
        for (Raven_Bot it : opponents) {
            gdi.OrangePen();
            Vector2D p = it.Pos();
            double b = it.BRadius();

            gdi.Line(p.x - b, p.y - b, p.x + b, p.y - b);
            gdi.Line(p.x + b, p.y - b, p.x + b, p.y + b);
            gdi.Line(p.x + b, p.y + b, p.x - b, p.y + b);
            gdi.Line(p.x - b, p.y + b, p.x - b, p.y - b);
        }

    }
}