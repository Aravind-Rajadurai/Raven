package Raven;

import static common.D2.Transformation.WorldTransform;
import common.D2.Vector2D;
import static common.Time.CrudeTimer.Clock;
import static common.misc.Cgdi.gdi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Mat Buckland (ai-junkie.com)
 * 
* Desc: Class to record and render graves at the site of a bot's death
 * 
*/
class GraveMarkers {

	private class GraveRecord {

		Vector2D Position;
		double TimeCreated;

		GraveRecord(Vector2D pos) {
			Position = new Vector2D(pos);
			TimeCreated = Clock.GetCurrentTime();
		}
	}

	private class GraveList extends LinkedList<GraveRecord> {
	};
	/**
	 * how long a grave remains on screen
	 */
	private double m_dLifeTime;
	//when a bot dies, a grave is rendered to mark the spot.
	private List<Vector2D> m_vecRIPVB = new ArrayList<Vector2D>();
	private List<Vector2D> m_vecRIPVBTrans = new ArrayList<Vector2D>();
	private GraveList m_GraveList = new GraveList();

	/**
	 * ctor
	 */
	public GraveMarkers(double lifetime) {
		m_dLifeTime = lifetime;
		//create the vertex buffer for the graves
		final Vector2D[] rip = {
			new Vector2D(-4, -5),
			new Vector2D(-4, 3),
			new Vector2D(-3, 5),
			new Vector2D(-1, 6),
			new Vector2D(1, 6),
			new Vector2D(3, 5),
			new Vector2D(4, 3),
			new Vector2D(4, -5),
			new Vector2D(-4, -5)};
		final int NumripVerts = rip.length;
		for (int i = 0; i < NumripVerts; ++i) {
			m_vecRIPVB.add(rip[i]);
		}
	}

	public void Update() {
		Iterator<GraveRecord> it = m_GraveList.iterator();
		while (it.hasNext()) {
			GraveRecord gr = it.next();
			if (Clock.GetCurrentTime() - gr.TimeCreated > m_dLifeTime) {
				it.remove();
			}
		}
	}

	public void Render() {
		Iterator<GraveRecord> it = m_GraveList.iterator();
		Vector2D facing = new Vector2D(-1, 0);
		while (it.hasNext()) {
			GraveRecord gr = it.next();
			m_vecRIPVBTrans = WorldTransform(m_vecRIPVB,
					gr.Position,
					facing,
					facing.Perp(),
					new Vector2D(1, 1));

			gdi.BrownPen();
			gdi.ClosedShape(m_vecRIPVBTrans);
			gdi.TextColor(133, 90, 0);
			gdi.TextAtPos(gr.Position.x - 9, gr.Position.y - 5, "RIP");
		}
	}

	public void AddGrave(Vector2D pos) {
		m_GraveList.add(new GraveRecord(pos));
	}
}