/*
 * 
 *  @author Petr (http://www.sallyx.org/)
 */
package Raven.goals;

import common.misc.TypeToString;

public class Raven_Goal_Types {

    public static final int goal_think = 0;
    public static final int goal_explore = 1;
    public static final int goal_arrive_at_position = 2;
    public static final int goal_seek_to_position = 3;
    public static final int goal_follow_path = 4;
    public static final int goal_traverse_edge = 5;
    public static final int goal_move_to_position = 6;
    public static final int goal_get_health = 7;
    public static final int goal_get_shotgun = 8;
    public static final int goal_get_rocket_launcher = 9;
    public static final int goal_get_railgun = 10;
    public static final int goal_wander = 11;
    public static final int goal_negotiate_door = 12;
    public static final int goal_attack_target = 13;
    public static final int goal_hunt_target = 14;
    public static final int goal_strafe = 15;
    public static final int goal_adjust_range = 16;
    public static final int goal_say_phrase = 17;

    public static class GoalTypeToString extends TypeToString {

        private static GoalTypeToString instance = new GoalTypeToString();

        private GoalTypeToString() {
        }

        public static GoalTypeToString Instance() {
            return instance;
        }

        @Override
        public String Convert(int gt) {
            switch (gt) {
                case goal_explore:
                    return "explore";
                case goal_think:
                    return "think";
                case goal_arrive_at_position:
                    return "arrive_at_position";
                case goal_seek_to_position:
                    return "seek_to_position";
                case goal_follow_path:
                    return "follow_path";
                case goal_traverse_edge:
                    return "traverse_edge";
                case goal_move_to_position:
                    return "move_to_position";
                case goal_get_health:
                    return "get_health";
                case goal_get_shotgun:
                    return "get_shotgun";
                case goal_get_railgun:
                    return "get_railgun";
                case goal_get_rocket_launcher:
                    return "get_rocket_launcher";
                case goal_wander:
                    return "wander";
                case goal_negotiate_door:
                    return "negotiate_door";
                case goal_attack_target:
                    return "attack_target";
                case goal_hunt_target:
                    return "hunt_target";
                case goal_strafe:
                    return "strafe";
                case goal_adjust_range:
                    return "adjust_range";
                case goal_say_phrase:
                    return "say_phrase";
                default:
                    return "UNKNOWN GOAL TYPE!";
            }//end switch
        }
    }
}