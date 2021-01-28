/**
 * Desc: a few functions for testing line segments against containers of walls
 *
 * @author Petr (http://www.sallyx.org/)
 */
package common.D2;

import java.util.List;
import static common.D2.geometry.*;
import static common.D2.Vector2D.add;
import static common.D2.Vector2D.sub;
import static common.D2.Vector2D.mul;
import static common.D2.Vector2D.Vec2DNormalize;
import common.misc.CppToJava.DoubleRef;
import static common.misc.utils.MaxDouble;

/**
 *
 * @author Petr
 */
public class WallIntersectionTests {

    /**
     * given a line segment defined by the points from and to, iterate through
     * all the map objects and walls and test for any intersection. This method
     * returns true if an intersection occurs.
     */
    public static <ContWall extends List< ? extends Wall2D>> boolean 
            doWallsObstructLineSegment(Vector2D from,
            Vector2D to,
            final ContWall walls) {
        //test against the walls  
        for (Wall2D curWall : walls) {
            //do a line segment intersection test
            if (LineIntersection2D(from, to, curWall.From(), curWall.To())) {
                return true;
            }
        }

        return false;
    }

    /**
     * similar to above except this version checks to see if the sides described
     * by the cylinder of length |AB| with the given radius intersect any walls.
     * (this enables the trace to take into account any the bounding radii of
     * entity objects)
     */
    public static <ContWall extends List<? extends Wall2D>> boolean 
            doWallsObstructCylinderSides(Vector2D A,
            Vector2D B,
            double BoundingRadius,
            final ContWall walls) {
        //the line segments that make up the sides of the cylinder must be created
        Vector2D toB = Vec2DNormalize(sub(B, A));

        //A1B1 will be one side of the cylinder, A2B2 the other.
        Vector2D A1, B1, A2, B2;

        Vector2D radialEdge = mul(toB.Perp(), BoundingRadius);

        //create the two sides of the cylinder
        A1 = add(A, radialEdge);
        B1 = add(B, radialEdge);

        A2 = sub(A, radialEdge);
        B2 = sub(B, radialEdge);

        //now test against them
        if (!doWallsObstructLineSegment(A1, B1, walls)) {
            return doWallsObstructLineSegment(A2, B2, walls);
        }

        return true;
    }

    /**
     * tests a line segment against the container of walls to calculate the
     * closest intersection point, which is stored in the reference 'ip'. The
     * distance to the point is assigned to the reference 'distance'
     *
     * @return false if no intersection point found
     */
    public static <ContWall extends List<? extends Wall2D>> boolean 
            FindClosestPointOfIntersectionWithWalls(Vector2D A,
            Vector2D B,
            DoubleRef distance,
            Vector2D ip,
            final ContWall walls) {
        distance.set(MaxDouble);

        for (Wall2D curWall : walls) {
            DoubleRef dist = new DoubleRef(0.0);
            Vector2D point = new Vector2D();

            if (LineIntersection2D(A, B, curWall.From(), curWall.To(), dist, point)) {
                if (dist.get() < distance.toDouble()) {
                    distance.set(dist.toDouble());
                    ip.set(point);
                }
            }
        }

        if (distance.toDouble() < MaxDouble) {
            return true;
        }

        return false;
    }

    /**
     * @return true if any walls intersect the circle of radius at point p
     */
    public static <ContWall extends List<? extends Wall2D>> boolean 
            doWallsIntersectCircle(final ContWall walls, Vector2D p, double r) {
        //test against the walls
        for (Wall2D curWall : walls) {
            //do a line segment intersection test
            if (LineSegmentCircleIntersection(curWall.From(), curWall.To(), p, r)) {
                return true;
            }
        }

        return false;
    }
}