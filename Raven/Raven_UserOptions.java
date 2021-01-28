package Raven;
/**
* Name:   Raven_UserOptions.h
*
*  Author: Mat Buckland (www.ai-junkie.com)
*
*  Desc:   singleton class to control a number of menu options
*/

public class Raven_UserOptions {
	
	public static Raven_UserOptions UserOptions = new Raven_UserOptions();

	private Raven_UserOptions() {
		m_bShowGraph = false;

		m_bShowPathOfSelectedBot = true;
		m_bSmoothPathsQuick = false;
		m_bSmoothPathsPrecise = false;
		m_bShowBotIDs = false;
		m_bShowBotHealth = true;
		m_bShowTargetOfSelectedBot = false;
		m_bOnlyShowBotsInTargetsFOV = false;
		m_bShowScore = false;
		m_bShowGoalsOfSelectedBot = true;
		m_bShowGoalAppraisals = true;
		m_bShowNodeIndices = false;
		m_bShowOpponentsSensedBySelectedBot = true;
		m_bShowWeaponAppraisals = false;
	}

	//copy ctor and assignment should be private
	//Raven_UserOptions(const Raven_UserOptions&);
	//Raven_UserOptions& operator=(const Raven_UserOptions&);
	private Raven_UserOptions(Raven_UserOptions o) {
	}

	public static Raven_UserOptions Instance() {
		return UserOptions;
	}
	
	public boolean m_bShowGraph;
	public boolean m_bShowNodeIndices;
	public boolean m_bShowPathOfSelectedBot;
	public boolean m_bShowTargetOfSelectedBot;
	public boolean m_bShowOpponentsSensedBySelectedBot;
	public boolean m_bOnlyShowBotsInTargetsFOV;
	public boolean m_bShowGoalsOfSelectedBot;
	public boolean m_bShowGoalAppraisals;
	public boolean m_bShowWeaponAppraisals;
	public boolean m_bSmoothPathsQuick;
	public boolean m_bSmoothPathsPrecise;
	public boolean m_bShowBotIDs;
	public boolean m_bShowBotHealth;
	public boolean m_bShowScore;
}
