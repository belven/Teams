package belven.teams;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import belven.teams.TeamManager.TeamRank;

public class Team {
	public String teamName;
	public HashMap<Player, PlayerTeamData> pData = new HashMap<Player, PlayerTeamData>();
	public List<Chunk> ownedChunks = new ArrayList<Chunk>();
	public List<String> playersUUIDs = new ArrayList<String>();
	DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	Calendar cal = Calendar.getInstance();

	public TeamManager plugin;
	public boolean friendlyFire = false;
	public boolean isOpen = true;

	private static final int SECOND = 1000;
	private static final int MINUTE = 60 * SECOND;
	private static final int HOUR = 60 * MINUTE;

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

		plugin.saveConfig();
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
		plugin.saveConfig();
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
		playersUUIDs.add(p.getUniqueId().toString());
		plugin.saveConfig();
	}

	public void ClaimChunk(Player p, Location l) {
		Chunk c = l.getChunk();
		String path = teamName + ".Last Claimed";

		if (!ownedChunks.contains(c)) {
			if (durationFromLastClaim() <= 0) {
				if (ownedChunks.size() < getMaxChunks()) {
					ownedChunks.add(c);

					plugin.TeamChunks.put(c, this);
					plugin.SendTeamChat(
							this,
							p.getName() + " claim land for " + teamName
									+ ", you can claim "
									+ String.valueOf(getMaxChunks())
									+ " chunks of land.");

					String date = dateFormat.format(cal.getTime());
					plugin.getConfig().set(path, date);
					saveTeamChunks();
					plugin.saveConfig();
				} else {
					p.sendMessage("Your team cannot claim more land ");
				}
			} else {
				p.sendMessage("You need to wait "
						+ String.valueOf(durationFromLastClaim())
						+ " hour before you can claim land");
			}
		}
	}

	public int getMaxChunks() {
		return playersUUIDs.size() * 3;
	}

	public void saveTeamChunks() {
		if (ownedChunks.size() == 1) {
			plugin.getConfig().set(teamName + ".World",
					ownedChunks.get(0).getWorld().getName());
		}

		StringBuilder sb = new StringBuilder(50);

		for (Chunk oc : ownedChunks) {
			Block b = oc.getBlock(0, 0, 0);
			String l = plugin.LocationToString(b.getLocation());
			sb.append(l + "@C");
		}

		plugin.getConfig().set(teamName + ".Chunks", sb.toString());
		plugin.saveConfig();
	}

	public long durationFromLastClaim() {
		Calendar cal = Calendar.getInstance();
		String path = teamName + ".Last Claimed";
		Date lastDate = new Date();
		long hoursDiff = 0;

		if (plugin.getConfig().contains(path)) {
			try {
				lastDate = dateFormat.parse(plugin.getConfig().getString(path));
				long diff = cal.getTime().getTime() - lastDate.getTime();
				hoursDiff = diff / HOUR;
				return timeBetweenClaims() - hoursDiff;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return hoursDiff;
	}

	public long timeBetweenClaims() {
		return 1L;
	}

	public void removeClaim(Player p, Location l) {
		Chunk c = l.getChunk();
		if (ownedChunks.contains(c)) {

			ownedChunks.remove(c);
			plugin.TeamChunks.remove(c);

			String msg = p.getName() + " removed claimed land for " + teamName;
			plugin.SendTeamChat(this, msg);

			String path = teamName + ".Last Claimed";
			plugin.getConfig().set(path, null);
			saveTeamChunks();
		} else {
			p.sendMessage("Your team doesn't own this land");
		}
	}
}
