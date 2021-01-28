/**
 * Desc: a template class to manage a number of graph searches, and to
 * distribute the calculation of each search over several update-steps
 *
 * @author Petr (http://www.sallyx.org/)
 */
package Raven.navigation;

import static Raven.navigation.TimeSlicedGraphAlgorithms.target_found;
import static Raven.navigation.TimeSlicedGraphAlgorithms.target_not_found;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PathManager<path_planner extends Raven_PathPlanner> {

	/**
	 * a container of all the active search requests
	 */
	private List<path_planner> m_SearchRequests = new LinkedList<path_planner>();
	/**
	 * this is the total number of search cycles allocated to the manager. Each
	 * update-step these are divided equally amongst all registered path
	 * requests
	 */
	private int m_iNumSearchCyclesPerUpdate;

	public PathManager(int NumCyclesPerUpdate) {
		m_iNumSearchCyclesPerUpdate = NumCyclesPerUpdate;
	}

	/**
	 * every time this is called the total amount of search cycles available
	 * will be shared out equally between all the active path requests. If a
	 * search completes successfully or fails the method will notify the
	 * relevant bot
	 * ///////////////////////////////////////////////////////////////////////////////
	 *
	 *
	 * This method iterates through all the active path planning requests
	 * updating their searches until the user specified total number of search
	 * cycles has been satisfied.
	 *
	 * If a path is found or the search is unsuccessful the relevant agent is
	 * notified accordingly by Telegram
	 */
	public void UpdateSearches() {
		int NumCyclesRemaining = m_iNumSearchCyclesPerUpdate;

		//iterate through the search requests until either all requests have been
		//fulfilled or there are no search cycles remaining for this update-step.
		Iterator<path_planner> iterator = m_SearchRequests.iterator();
		while (NumCyclesRemaining-- > 0 && !m_SearchRequests.isEmpty()) {
			path_planner curPath = iterator.next();
			//make one search cycle of this path request
			int result = curPath.CycleOnce();

			//if the search has terminated remove from the list
			if ((result == target_found) || (result == target_not_found)) {
				//remove this path from the path list
				iterator.remove();
			}

			//the iterator may now be pointing to the end of the list. If this is so,
			// it must be reset to the beginning.
			if (!iterator.hasNext()) {
				iterator = m_SearchRequests.iterator();
			}

		}//end while
	}

	/**
	 * a path planner should call this method to register a search with the
	 * manager. (The method checks to ensure the path planner is only registered
	 * once)
	 */
	public void Register(path_planner pPathPlanner) {
		//make sure the bot does not already have a current search in the queue
		if (!m_SearchRequests.contains(pPathPlanner)) {
			//add to the list
			m_SearchRequests.add(pPathPlanner);
		}
	}

	public void UnRegister(path_planner pPathPlanner) {
		m_SearchRequests.remove(pPathPlanner);
	}

	/**
	 * returns the amount of path requests currently active.
	 */
	public int GetNumActiveSearches() {
		return m_SearchRequests.size();
	}
}
