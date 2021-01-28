/**
 * Desc: trigger class to define a button that sends a msg to a specific entity
 * when activated.
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.triggers;

import Raven.BaseGameEntity;
import Raven.Raven_Messages.message_type;
import common.D2.Vector2D;
import static common.D2.Vector2D.add;
import static common.D2.Vector2D.sub;
import static common.Messaging.MessageDispatcher.Dispatcher;
import static common.Messaging.MessageDispatcher.NO_ADDITIONAL_INFO;
import static common.Messaging.MessageDispatcher.SEND_MSG_IMMEDIATELY;
import common.Messaging.Telegram;
import common.Triggers.Trigger;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.GetValueFromStream_Int;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class Trigger_OnButtonSendMsg<entity_type extends BaseGameEntity> extends Trigger<entity_type> {

    /**
     * when triggered a message is sent to the entity with the following ID
     */
    private int m_iReceiver;
    /**
     * the message that is sent
     */
    private int m_iMessageToSend;

    public Trigger_OnButtonSendMsg(Scanner datafile) {
        super(GetValueFromStream_Int(datafile));
        Read(datafile);
    }

    @Override
    public void Try(entity_type pEnt) {

        if (isTouchingTrigger(pEnt.Pos(), pEnt.BRadius())) {
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    this.ID(),
                    m_iReceiver,
                    message_type.valueOf(m_iMessageToSend),
                    NO_ADDITIONAL_INFO);

        }
    }

    @Override
    public void Update() {
    }

    @Override
    public void Render() {
        gdi.OrangePen();

        double sz = BRadius();

        gdi.Line(Pos().x - sz, Pos().y - sz, Pos().x + sz, Pos().y - sz);
        gdi.Line(Pos().x + sz, Pos().y - sz, Pos().x + sz, Pos().y + sz);
        gdi.Line(Pos().x + sz, Pos().y + sz, Pos().x - sz, Pos().y + sz);
        gdi.Line(Pos().x - sz, Pos().y + sz, Pos().x - sz, Pos().y - sz);
    }

    @Override
    public void Write(PrintStream os) {
    }

    @Override
    public void Read(InputStream is) {
        Read(new Scanner(is));
    }
    
    @Override
    public void Read(Scanner is) {
        try {
            //grab the id of the entity it messages
            m_iReceiver = is.nextInt();

            //grab the message type
            m_iMessageToSend = is.nextInt();

            //grab the position and radius
            double x, y, r;
            x = is.nextDouble();
            y = is.nextDouble();
            r = is.nextDouble();

            SetPos(new Vector2D(x, y));
            SetBRadius(r);

            //create and set this trigger's region of fluence
            AddRectangularTriggerRegion(sub(Pos(), new Vector2D(BRadius(), BRadius())), //top left corner
                    add(Pos(), new Vector2D(BRadius(), BRadius())));  //bottom right corner
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean HandleMessage(final Telegram msg) {
        return false;
    }
}