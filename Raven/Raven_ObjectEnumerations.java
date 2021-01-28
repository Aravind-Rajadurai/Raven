/**
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven;

public class Raven_ObjectEnumerations {

    public static final int type_wall = 0;
    public static final int type_bot = 1;
    public static final int type_unused = 2;
    public static final int type_waypoint = 3;
    public static final int type_health = 4;
    public static final int type_spawn_point = 5;
    public static final int type_rail_gun = 6;
    public static final int type_rocket_launcher = 7;
    public static final int type_shotgun = 8;
    public static final int type_blaster = 9;
    public static final int type_obstacle = 10;
    public static final int type_sliding_door = 11;
    public static final int type_door_trigger = 12;

    public static String GetNameOfType(int w) {
        String s;

        switch (w) {
            case type_wall:
                s = "Wall";
                break;

            case type_waypoint:
                s = "Waypoint";
                break;

            case type_obstacle:
                s = "Obstacle";
                break;

            case type_health:
                s = "Health";
                break;

            case type_spawn_point:
                s = "Spawn Point";
                break;

            case type_rail_gun:
                s = "Railgun";
                break;

            case type_blaster:
                s = "Blaster";
                break;

            case type_rocket_launcher:
                s = "rocket_launcher";
                break;

            case type_shotgun:
                s = "shotgun";
                break;

            case type_unused:
                s = "knife";
                break;

            case type_bot:
                s = "bot";
                break;

            case type_sliding_door:
                s = "sliding_door";
                break;

            case type_door_trigger:
                s = "door_trigger";
                break;

            default:
                s = "UNKNOWN OBJECT TYPE";
                break;
        }

        return s;
    }
}