/**
 * Desc: class to represent a path edge. This path can be used by a path planner
 * in the creation of paths.
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.navigation;

import common.D2.Vector2D;

public class PathEdge {
    //positions of the source and destination nodes this edge connects

    private Vector2D m_vSource;
    private Vector2D m_vDestination;
    /**
     * the behavior associated with traversing this edge
     */
    private int m_iBehavior;
    private int m_iDoorID;

    public PathEdge(Vector2D Source, Vector2D Destination, int Behavior) {
        this(Source, Destination, Behavior, 0);
    }

    public PathEdge(Vector2D Source, Vector2D Destination, int Behavior, int DoorID) {
        m_vSource = new Vector2D(Source);
        m_vDestination = new Vector2D(Destination);
        m_iBehavior = Behavior;
        m_iDoorID = DoorID;
    }
    // copy ctor
    public PathEdge(PathEdge edge) {
        this(new Vector2D(edge.m_vSource), new Vector2D(edge.m_vDestination), edge.m_iBehavior, edge.m_iDoorID);
    }

    public Vector2D Destination() {
        return new Vector2D(m_vDestination);
    }

    public void SetDestination(Vector2D NewDest) {
        m_vDestination = new Vector2D(NewDest);
    }

    public Vector2D Source() {
        return new Vector2D(m_vSource);
    }

    public void SetSource(Vector2D NewSource) {
        m_vSource = new Vector2D(NewSource);
    }

    public int DoorID() {
        return m_iDoorID;
    }

    public int Behavior() {
        return m_iBehavior;
    }
}