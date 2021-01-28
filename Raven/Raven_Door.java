package Raven;

import static Raven.Raven_Messages.message_type.Msg_OpenSesame;
import common.D2.Vector2D;
import common.D2.Wall2D;
import static common.D2.Vector2D.Vec2DDistance;
import static common.D2.Vector2D.add;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.sub;
import static common.D2.Vector2D.Vec2DNormalize;
import common.Messaging.Telegram;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.GetValueFromStream_Int;
import static common.misc.utils.clamp;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Author: Mat Buckland (www.ai-junkie.com)
 * 
* Desc: class to emulate a sliding door that can be opened by sending it a
 * msg_OpenSesame telegram. The door stays open for a user specified amount of
 * time before closing.
 */
class Raven_Door extends BaseGameEntity {

    protected static enum door_status {

        open, opening, closed, closing
    };
    protected door_status m_Status;
    /**
     * a sliding door is created from two walls, back to back.These walls must
     * be added to a map's geometry in order for an agent to detect them
     */
    protected Wall2D m_pWall1;
    protected Wall2D m_pWall2;
    /**
     * a container of the id's of the triggers able to open this door
     */
    protected List<Integer> m_Switches = new ArrayList<Integer>();
    /**
     * how long the door remains open before it starts to shut again
     */
    protected int m_iNumTicksStayOpen;
    /**
     * how long the door has been open (0 if status is not open)
     */
    protected int m_iNumTicksCurrentlyOpen;
    // the door's position and size when in the open position
    protected Vector2D m_vP1 = new Vector2D();
    protected Vector2D m_vP2 = new Vector2D();
    protected double m_dSize;
    /**
     * a normalized vector facing along the door. This is used frequently by the
     * other methods so we might as well just calculate it once in the ctor
     */
    protected Vector2D m_vtoP2Norm = new Vector2D();
    /**
     * the door's current size
     */
    protected double m_dCurrentSize;

    //---------------------------- Open -------------------------------------------
    protected void Open() {
        if (m_Status == door_status.opening) {
            if (m_dCurrentSize < 2) {
                m_Status = door_status.open;

                m_iNumTicksCurrentlyOpen = m_iNumTicksStayOpen;

                return;

            }

            //reduce the current size
            m_dCurrentSize -= 1;

            m_dCurrentSize = clamp(m_dCurrentSize, 0.0, m_dSize);

            ChangePosition(m_vP1, add(m_vP1, mul(m_vtoP2Norm, m_dCurrentSize)));

        }
    }

    protected void Close() {
        if (m_Status == door_status.closing) {
            if (m_dCurrentSize == m_dSize) {
                m_Status = door_status.closed;
                return;

            }

            //reduce the current size
            m_dCurrentSize += 1;

            m_dCurrentSize = clamp(m_dCurrentSize, 0.0, m_dSize);

            ChangePosition(m_vP1, add(m_vP1, mul(m_vtoP2Norm, m_dCurrentSize)));

        }
    }

    protected void ChangePosition(Vector2D newP1, Vector2D newP2) {
        m_vP1 = new Vector2D(newP1);
        m_vP2 = new Vector2D(newP2);

        m_pWall1.SetFrom(add(m_vP1, m_vtoP2Norm.Perp()));
        m_pWall1.SetTo(add(m_vP2, m_vtoP2Norm.Perp()));

        m_pWall2.SetFrom(sub(m_vP2, m_vtoP2Norm.Perp()));
        m_pWall2.SetTo(sub(m_vP1, m_vtoP2Norm.Perp()));
    }
//---------------------------- ctor -------------------------------------------
//-----------------------------------------------------------------------------
    public Raven_Door(Raven_Map pMap, InputStream is) {
        this(pMap, new Scanner(is));
    }
    
    public Raven_Door(Raven_Map pMap, Scanner is) {
        super(GetValueFromStream_Int(is));
        m_Status = door_status.closed;
        m_iNumTicksStayOpen = 60;                   //MGC!
        Read(is);

        m_vtoP2Norm = Vec2DNormalize(sub(m_vP2, m_vP1));
        m_dCurrentSize = m_dSize = Vec2DDistance(m_vP2, m_vP1);

        Vector2D perp = m_vtoP2Norm.Perp();

        //create the walls that make up the door's geometry
        m_pWall1 = pMap.AddWall(add(m_vP1, perp), add(m_vP2, perp));
        m_pWall2 = pMap.AddWall(sub(m_vP2, perp), sub(m_vP1, perp));
    }

//---------------------------- dtor -------------------------------------------
//-----------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    //the usual suspects
    @Override
    public void Render() {
        gdi.ThickBluePen();
        gdi.Line(m_vP1, m_vP2);
    }

//--------------------------- Update ------------------------------------------
    @Override
    public void Update() {
        switch (m_Status) {
            case opening:

                Open();
                break;

            case closing:

                Close();
                break;

            case open: {
                if (m_iNumTicksCurrentlyOpen-- < 0) {
                    m_Status = door_status.closing;
                }
            }
        }


    }

    @Override
    public boolean HandleMessage(final Telegram msg) {
        if (msg.Msg == Msg_OpenSesame) {
            if (m_Status != door_status.open) {
                m_Status = door_status.opening;
            }

            return true;
        }

        return false;
    }

    @Override
    public void Read(InputStream in) {
        Read(new Scanner(in));
    }
    
    @Override
    public void Read(Scanner in) {
        double x, y;
        
        //grab the hinge points
        x = in.nextDouble();
        y = in.nextDouble();
        m_vP1 = new Vector2D(x, y);
        x = in.nextDouble();
        y = in.nextDouble();
        m_vP2 = new Vector2D(x, y);

        //grab the number of triggers
        int num, trig;

        num = in.nextInt();

        //save the trigger IDs
        for (int i = 0; i < num; ++i) {
            trig = in.nextInt();

            m_Switches.add(trig);
        }
    }

    /**
     * adds the id of a trigger for the door to notify when operating
     */
    public void AddSwitch(int id) {
        //only add the trigger if it isn't already present

        Iterator<Integer> it = m_Switches.iterator();
        while (it.hasNext()) {
            if (it.next() == id) {
                return;
            }
        }
        m_Switches.add(id);
    }

    public List<Integer> GetSwitchIDs() {
        return new ArrayList<Integer>(m_Switches);
    }
}