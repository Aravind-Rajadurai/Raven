/**
 * Desc: This trigger 'gives' the triggering bot a weapon of the specified type
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.triggers;

import static Raven.Constants.FrameRate;
import Raven.Raven_Bot;
import static Raven.Raven_ObjectEnumerations.*;
import static Raven.lua.Raven_Scriptor.script;
import static common.D2.Transformation.WorldTransform;
import common.D2.Vector2D;
import common.Triggers.Trigger_Respawning;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.GetValueFromStream_Int;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Trigger_WeaponGiver extends Trigger_Respawning<Raven_Bot> {
    //vrtex buffers for rocket shape

    private List<Vector2D> m_vecRLVB = new ArrayList<>();
    private List<Vector2D> m_vecRLVBTrans = new ArrayList<>();

    /**
     * this type of trigger is created when reading a map file
     */
    public Trigger_WeaponGiver(Scanner datafile) {

        super(GetValueFromStream_Int(datafile));
        Read(datafile);

        //create the vertex buffer for the rocket shape
        final Vector2D rip[] = {new Vector2D(0, 3),
            new Vector2D(1, 2),
            new Vector2D(1, 0),
            new Vector2D(2, -2),
            new Vector2D(-2, -2),
            new Vector2D(-1, 0),
            new Vector2D(-1, 2),
            new Vector2D(0, 3)};
        final int NumRocketVerts = rip.length;

        for (int i = 0; i < NumRocketVerts; ++i) {
            m_vecRLVB.add(rip[i]);
        }
    }

    /**
     * if triggered, this trigger will call the PickupWeapon method of the bot.
     * PickupWeapon will instantiate a weapon of the appropriate type.
     */
    @Override
    public void Try(Raven_Bot pBot) {
        if (this.isActive() && this.isTouchingTrigger(pBot.Pos(), pBot.BRadius())) {
            pBot.GetWeaponSys().AddWeapon(EntityType());

            Deactivate();
        }
    }

    /**
     * draws a symbol representing the weapon type at the trigger's location
     */
    @Override
    public void Render() {
        if (isActive()) {
            switch (EntityType()) {
                case type_rail_gun: {
                    gdi.BluePen();
                    gdi.BlueBrush();
                    gdi.Circle(Pos(), 3);
                    gdi.ThickBluePen();
                    gdi.Line(Pos(), new Vector2D(Pos().x, Pos().y - 9));
                }

                break;

                case type_shotgun: {

                    gdi.BlackBrush();
                    gdi.BrownPen();
                    final double sz = 3.0;
                    gdi.Circle(Pos().x - sz, Pos().y, sz);
                    gdi.Circle(Pos().x + sz, Pos().y, sz);
                }

                break;

                case type_rocket_launcher: {

                    Vector2D facing = new Vector2D(-1, 0);

                    m_vecRLVBTrans = WorldTransform(m_vecRLVB,
                            Pos(),
                            facing,
                            facing.Perp(),
                            new Vector2D(2.5, 2.5));

                    gdi.RedPen();
                    gdi.ClosedShape(m_vecRLVBTrans);
                }

                break;

            }//end switch
        }
    }

    @Override
    public void Read(InputStream in) {
        Read(new Scanner(in));
    }
    
    @Override
    public void Read(Scanner in) {
        try {
            double x, y, r;
            int GraphNodeIndex;

            x = in.nextDouble();
            y = in.nextDouble();
            r = in.nextDouble();
            GraphNodeIndex = in.nextInt();

            SetPos(new Vector2D(x, y));
            SetBRadius(r);
            SetGraphNodeIndex(GraphNodeIndex);

            //create this trigger's region of fluence
            AddCircularTriggerRegion(Pos(), script.GetDouble("DefaultGiverTriggerRange"));


            SetRespawnDelay((int) (script.GetDouble("Weapon_RespawnDelay") * FrameRate));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}