package belven.teams;

import belven.teams.TeamManager.TeamRank;

public class PlayerTeamData {
	public static final String SEPERATOR = "@";

	public enum CHATLVL {
		Global, Team;
	}

	public TeamRank teamRank;
	public CHATLVL chatLvl = CHATLVL.Global;

	public PlayerTeamData() {
	}

	public PlayerTeamData(TeamRank t) {
		teamRank = t;
	}

	public PlayerTeamData(String s) {
		fromString(s);
	}

	@Override
	public String toString() {
		return teamRank.toString();
	}

	public void fromString(String s) {
		teamRank = TeamRank.valueOf(s);
	}
}
