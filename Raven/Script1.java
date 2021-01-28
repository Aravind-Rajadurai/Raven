/**
 * @author Petr (http://www.sallyx.org/)
 */
package Raven;

import java.util.HashMap;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.MenuElement;
import static Raven.resource.*;
import static common.windows.*;

public class Script1 {

    public static class MyMenuBar extends JMenuBar {

        final private ActionListener al;
        private Map<Integer, MyCheckBoxMenuItem> items = new HashMap<Integer, MyCheckBoxMenuItem>();

        public MyMenuBar() {
            super();
            al = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    MyMenuItem source = (MyMenuItem) e.getSource();
                    Main.HandleMenuItems(source.getID(), MyMenuBar.this);
                }
            };
        }

        @Override
        public JMenu add(JMenu c) {
            for (MenuElement elm : c.getSubElements()) {
                for (MenuElement comp : elm.getSubElements()) {
                    if (comp instanceof MyCheckBoxMenuItem) {
                        MyCheckBoxMenuItem myelm = (MyCheckBoxMenuItem) comp;
                        this.items.put(myelm.getID(), myelm);
                    }
                }
            }
            return super.add(c);
        }

        private ActionListener getActionListener() {
            return al;
        }

        /**
         * Swap menu state and do call actionEvent
         *
         * @param MenuItem ID of MyCheckBoxMenuItem
         */
        public void changeMenuState(int MenuItem) {
            MyCheckBoxMenuItem item = this.items.get(MenuItem);
            if (item != null) {
                item.doClick();
            }
        }

        /**
         * Set menu state and do not call actionEvent
         *
         * @param MenuItem ID of MyCheckBoxMenuItem
         * @param state New state (MFS_CHECKED or MFS_UNCHECKED)
         */
        public void setMenuState(int MenuItem, final long state) {
            MyCheckBoxMenuItem item = this.items.get(MenuItem);
            if (item == null) {
                return;
            }
            if (state == MFS_CHECKED) {
                item.setSelected(true);
            } else if (state == MFS_UNCHECKED) {
                item.setSelected(false);
            } else {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        }
    }

    public static interface MyMenuItem {

        public int getID();
    }

    public static class MyButtonMenuItem extends JMenuItem implements MyMenuItem {

        private final int id;

        public MyButtonMenuItem(String title, int id, ActionListener al) {
            super(title);
            this.id = id;
            this.addActionListener(al);
        }

        @Override
        public int getID() {
            return id;
        }
    }

    public static class MyCheckBoxMenuItem extends JCheckBoxMenuItem implements MyMenuItem {

        private final int id;

        public MyCheckBoxMenuItem(String title, int id, ActionListener al) {
            this(title, id, al, false);
        }

        public MyCheckBoxMenuItem(String title, int id, ActionListener al, boolean checked) {
            super(title, checked);
            this.id = id;
            this.addActionListener(al);
        }

        @Override
        public int getID() {
            return id;
        }
    }

    public static MyMenuBar createMenu(final int id_menu) {
        MyMenuBar menu = new MyMenuBar();
        ActionListener al = menu.getActionListener();
        JMenu game = new JMenu("Game");
        {
            JMenuItem i1 = new MyButtonMenuItem("Load Map", IDM_GAME_LOAD, al);
            JMenuItem i2 = new MyButtonMenuItem("Add Bot [ csr up ]", IDM_GAME_ADDBOT, al);
            JMenuItem i3 = new MyButtonMenuItem("Remove Bot [ csr down ]", IDM_GAME_REMOVEBOT, al);
            JMenuItem i4 = new MyButtonMenuItem("Toggle Pause [ 'P' ]", IDM_GAME_PAUSE, al);
            game.add(i1);
            game.add(i2);
            game.add(i3);
            game.add(i4);
        }
        menu.add(game);

        JMenu navigation = new JMenu("Navigation");
        {
            JMenuItem i1 = new MyCheckBoxMenuItem("Show NavGraph", IDM_NAVIGATION_SHOW_NAVGRAPH, al);
            JMenuItem i2 = new MyCheckBoxMenuItem("Show Node Indices", IDM_NAVIGATION_SHOW_INDICES, al);
            JMenuItem i3 = new MyCheckBoxMenuItem("Smooth Paths (quick)", IDM_NAVIGATION_SMOOTH_PATHS_QUICK, al);

            JMenuItem i4 = new MyCheckBoxMenuItem("Smooth Paths (Precise)", IDM_NAVIGATION_SMOOTH_PATHS_PRECISE, al);
            navigation.add(i1);
            navigation.add(i2);
            navigation.add(i3);
            navigation.add(i4);

        }
        menu.add(navigation);
        JMenu general = new JMenu("General Bot Info");
        {
            JMenuItem i1 = new MyCheckBoxMenuItem("Show IDs", IDM_BOTS_SHOW_IDS, al);
            JMenuItem i2 = new MyCheckBoxMenuItem("Show Health", IDM_BOTS_SHOW_HEALTH, al);
            JMenuItem i3 = new MyCheckBoxMenuItem("Show Scores", IDM_BOTS_SHOW_SCORES, al);
            general.add(i1);
            general.add(i2);
            general.add(i3);
        }
        menu.add(general);
        JMenu info = new JMenu("Selected Bot Info");
        {
            JMenuItem i1 = new MyCheckBoxMenuItem("Show Target (boxed in red)", IDM_BOTS_SHOW_TARGET, al);
            JMenuItem i2 = new MyCheckBoxMenuItem("Show Sensed Opponents (boxed in orange)", IDM_BOTS_SHOW_SENSED, al);
            JMenuItem i3 = new MyCheckBoxMenuItem("Only Show Opponents in FOV", IDM_BOTS_SHOW_FOV, al);
            JMenuItem i4 = new MyCheckBoxMenuItem("Show Goal Queue", IDM_BOTS_SHOW_GOAL_Q, al);
            JMenuItem i5 = new MyCheckBoxMenuItem("Show Path", IDM_NAVIGATION_SHOW_PATH, al);
            info.add(i1);
            info.add(i2);
            info.add(i3);
            info.add(i4);
            info.add(i5);
        }
        menu.add(info);

        return menu;
    }
}
