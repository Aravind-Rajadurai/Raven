/**
 * Desc: If a bot runs over an instance of this class its health is increased.
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.triggers;

import static Raven.Constants.FrameRate;
import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.type_health;
import static Raven.lua.Raven_Scriptor.script;
import common.D2.Vector2D;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.GetValueFromStream_Int;
import common.Triggers.Trigger_Respawning;
import java.io.InputStream;
import java.util.Scanner;

public class Trigger_HealthGiver extends Trigger_Respawning<Raven_Bot> {

    /**
     * the amount of health an entity receives when it runs over this trigger
     */
    private int m_iHealthGiven;

    public Trigger_HealthGiver(Scanner datafile) {

        super(GetValueFromStream_Int(datafile));
        Read(datafile);
    }

    //if triggered, the bot's health will be incremented
    @Override
    public void Try(Raven_Bot pBot) {
        if (isActive() && isTouchingTrigger(pBot.Pos(), pBot.BRadius())) {
            pBot.IncreaseHealth(m_iHealthGiven);

            Deactivate();
        }
    }

    //draws a box with a red cross at the trigger's location
    @Override
    public void Render() {
        if (isActive()) {
            gdi.BlackPen();
            gdi.WhiteBrush();
            final int sz = 5;
            gdi.Rect(Pos().x - sz, Pos().y - sz, Pos().x + sz + 1, Pos().y + sz + 1);
            gdi.RedPen();
            gdi.Line(Pos().x, Pos().y - sz, Pos().x, Pos().y + sz + 1);
            gdi.Line(Pos().x - sz, Pos().y, Pos().x + sz + 1, Pos().y);
        }
    }

    @Override
    public void Read(InputStream in) {
        Read(new Scanner(in));
    }
    
    @Override
    public void Read(Scanner sc) {
        try {
            double x, y, r;
            int GraphNodeIndex;

            x = sc.nextDouble();
            y = sc.nextDouble();
            r = sc.nextDouble();
            m_iHealthGiven = sc.nextInt();
            GraphNodeIndex = sc.nextInt();

            SetPos(new Vector2D(x, y));
            SetBRadius(r);
            SetGraphNodeIndex(GraphNodeIndex);

            //create this trigger's region of fluence
            AddCircularTriggerRegion(Pos(), script.GetDouble("DefaultGiverTriggerRange"));

            SetRespawnDelay((int) (script.GetDouble("Health_RespawnDelay") * FrameRate));
            SetEntityType(type_health);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}