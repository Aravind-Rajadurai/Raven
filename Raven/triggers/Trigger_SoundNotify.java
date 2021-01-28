/**
 * Desc: whenever an agent makes a sound -- such as when a weapon fires -- this
 * trigger can be used to notify other bots of the event.
 *
 * This type of trigger has a circular trigger region and a lifetime of 1
 * update-step
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.triggers;

import static Raven.Constants.FrameRate;
import static Raven.Raven_Messages.message_type.Msg_GunshotSound;
import Raven.Raven_Bot;
import static Raven.lua.Raven_Scriptor.script;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import static common.Messaging.MessageDispatcher.SENDER_ID_IRRELEVANT;
import common.Triggers.Trigger_LimitedLifetime;

public class Trigger_SoundNotify extends Trigger_LimitedLifetime<Raven_Bot> {

    /**
     * a pointer to the bot that has made the sound
     */
    private Raven_Bot m_pSoundSource;

    //------------------------------ ctor -----------------------------------------
    //-----------------------------------------------------------------------------
    public Trigger_SoundNotify(Raven_Bot source, double range) {
        super(FrameRate / script.GetInt("Bot_TriggerUpdateFreq"));
        m_pSoundSource = source;

        //set position and range
        SetPos(m_pSoundSource.Pos());

        SetBRadius(range);

        //create and set this trigger's region of fluence
        AddCircularTriggerRegion(Pos(), BRadius());
    }

    /**
     * when triggered this trigger adds the bot that made the source of the
     * sound to the triggering bot's perception.
     */
    @Override
    public void Try(Raven_Bot pBot) {
        //is this bot within range of this sound
        if (isTouchingTrigger(pBot.Pos(), pBot.BRadius())) {
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    SENDER_ID_IRRELEVANT,
                    pBot.ID(),
                    Msg_GunshotSound,
                    m_pSoundSource);
        }
    }

    @Override
    public void Render() {
    }
}