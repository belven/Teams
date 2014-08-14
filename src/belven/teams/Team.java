package belven.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import belven.teams.TeamManager.TeamRank;

public class Team {
	public String teamName;
	public HashMap<Player, PlayerTeamData> pData = new HashMap<Player, PlayerTeamData>();

	public TeamManager plugin;
	public boolean friendlyFire = false;
	public boolean isOpen = true;
	public List<Chunk> ownedChunks = new ArrayList<Chunk>();

	public Team(TeamManager tm, String tn) {
		teamName = tn;
		plugin = tm;
		plugin.CurrentTeams.add(this);
	}

	public Team(TeamManager tm, String tn, List<Player> players) {
		this(tm, tn);

		for (Player p : players) {
			Add(p, TeamRank.MEMBER);
		}

		if (players.size() > 0) {
			Add(players.get(0), TeamRank.LEADER);
		}
	}

	public Team(TeamManager tm, String tn, Player p) {
		this(tm, tn);
		Add(p, TeamRank.LEADER);
	}

	public boolean Contains(Player p) {
		return pData.keySet().contains(p);
	}

	public void RemoveMember(Player p) {
		pData.remove(p);

		plugin.reloadConfig();
		plugin.getConfig().set(
				teamName + ".Players." + p.getUniqueId().toString(), null);

		if (pData.keySet().size() == 0) {
			RemoveTeam();
		}
	}

	public Set<Player> getMembers() {
		return pData.keySet();
	}

	public TeamRank getRank(Player p) {
		return pData.get(p).teamRank;
	}

	public void SetLeader(Player p) {
		for (Player pl : pData.keySet()) {
			if (pData.get(pl).teamRank == TeamRank.LEADER) {
				SetRank(pl, TeamRank.MEMBER);
			}
		}
		SetRank(p, TeamRank.LEADER);
	}

	public void SetRank(Player p, TeamRank tr) {
		Add(p, tr);
	}

	public void Leave(Player p) {
		p.sendMessage("You have left " + teamName);
		RemoveMember(p);
	}

	public void RemoveTeam() {
		plugin.getConfig().set(teamName, null);
		plugin.CurrentTeams.remove(this);
	}

	public void Add(Player p, TeamRank tr) {
		PlayerTeamData data;

		if (pData.containsKey(p)) {
			data = pData.get(p);
		} else {
			data = new PlayerTeamData(tr);
		}

		data.teamRank = tr;
		pData.put(p, data);
		plugin.reloadConfig();
		plugin.getConfig().set(
				teamName + ".Players." + p.getUniqueId().toString(),
				pData.get(p).toString());
	}

	public void ClaimChunk(Player p, Location location) {
		Chunk c = location.getChunk();

		if (!ownedChunks.contains(c)) {
			ownedChunks.add(c);
			plugin.AddTeamChunk(c, this);
			plugin.SendTeamChat(this, p.getName()
					+ " has just claim lad for the team.");
		}
	}
}
