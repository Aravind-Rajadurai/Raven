/**
 * Desc: file to enumerate the messages a Raven_Bot must be able to handle
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven;

public class Raven_Messages {

    public static enum message_type {

        Msg_Blank(0),
        Msg_PathReady(1),
        Msg_NoPathAvailable(2),
        Msg_TakeThatMF(3),
        Msg_YouGotMeYouSOB(4),
        Msg_GoalQueueEmpty(5),
        Msg_OpenSesame(6),
        Msg_GunshotSound(7),
        Msg_UserHasRemovedBot(8);
        final private int id;

        message_type(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return MessageToString(this);
        }
		
		public static message_type valueOf(int id) {
			for(message_type message: message_type.values()) {
				if(message.id == id) {
					return message;
				}
			}
			throw new IllegalArgumentException("Unknown id " + id);
		}
    };

//used for outputting debug info
    public static String MessageToString(message_type msg) {
        switch (msg) {
            case Msg_PathReady:
                return "Msg_PathReady";

            case Msg_NoPathAvailable:
                return "Msg_NoPathAvailable";

            case Msg_TakeThatMF:
                return "Msg_TakeThatMF";

            case Msg_YouGotMeYouSOB:
                return "Msg_YouGotMeYouSOB";

            case Msg_GoalQueueEmpty:
                return "Msg_GoalQueueEmpty";

            case Msg_OpenSesame:
                return "Msg_OpenSesame";

            case Msg_GunshotSound:
                return "Msg_GunshotSound";

            case Msg_UserHasRemovedBot:
                return "Msg_UserHasRemovedBot";

            default:
                return "Undefined message!";
        }
    }
}
