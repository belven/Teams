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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import belven.teams.TeamManager.TeamRank;

public class Team {
	public String teamName;
	public HashMap<Player, PlayerTeamData> pData = new HashMap<Player, PlayerTeamData>();
	public List<Location> ownedChunkslocations = new ArrayList<Location>();
	public HashMap<String, String> playersUUIDs = new HashMap<String, String>();
	public Location teamHome;
	private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private Calendar cal = Calendar.getInstance();

	public TeamManager plugin;
	public boolean friendlyFire = false;
	public boolean isOpen = true;
	public Date lastClaimDate = new Date();

	private static final int SECOND = 1000;
	private static final int MINUTE = 60 * SECOND;
	private static final int HOUR = 60 * MINUTE;

	public Team(TeamManager tm, String tn) {
		teamName = tn;
		plugin = tm;
		plugin.CurrentTeams.add(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Team ? teamName == ((Team) other).teamName : false;
	}

	public List<Chunk> ownedChunks() {
		List<Chunk> tempChunks = new ArrayList<Chunk>();
		for (Location l : ownedChunkslocations) {
			tempChunks.add(l.getChunk());
		}
		return tempChunks;
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
		return getMembers().contains(p);
	}

	public void RemoveMember(Player p) {
		pData.remove(p);
		plugin.reloadConfig();
		plugin.getConfig().set(PlayerPath(p), null);
		if (getMembers().size() == 0) {
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
		for (Player pl : getMembers()) {
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

	public void SaveTeam() {
		FileConfiguration conf = plugin.getConfig();
		conf.set(teamName, null);
		conf.set(teamName + ".Open", isOpen);
		conf.set(teamName + ".FriendlyFire", friendlyFire);

		if (teamHome != null) {
			conf.set(teamName + ".Team Home", plugin.LocationToString(teamHome));
		}

		if (lastClaimDate != null) {
			String date = dateFormat.format(lastClaimDate);
			conf.set(teamName + ".Last Claimed", date);
		}

		SavePlayersToConfig();
		saveTeamChunks();
		plugin.saveConfig();
	}

	public void SavePlayersToConfig() {
		FileConfiguration conf = plugin.getConfig();
		for (String UUID : playersUUIDs.keySet()) {
			conf.set(teamName + ".Players." + UUID, playersUUIDs.get(UUID));
		}
	}

	public void RemoveTeam() {
		plugin.getConfig().set(teamName, null);
		plugin.CurrentTeams.remove(this);
		plugin.saveConfig();
	}

	public PlayerTeamData getPlayerData(Player p) {
		return pData.get(p);
	}

	public void Add(Player p, TeamRank tr) {
		plugin.reloadConfig();

		if (!Contains(p)) {
			PlayerTeamData data = new PlayerTeamData(tr);
			pData.put(p, data);
			plugin.getConfig().set(PlayerPath(p), pData.get(p).toString());
			playersUUIDs.put(p.getUniqueId().toString(), tr.toString());
			plugin.saveConfig();
		}
	}

	public String PlayerPath(Player p) {
		return teamName + ".Players." + p.getUniqueId().toString();
	}

	// public boolean OwnsChunk(Chunk c) {
	// return ownedChunksBlocks.contains(c.getBlock(0, 0, 0));
	//
	// // return ownedChunks().contains(c);
	// }

	public boolean OwnsChunk(Chunk c) {
		Block cb = c.getBlock(0, 0, 0);
		for (Location l : ownedChunkslocations) {
			if (l.equals(cb.getLocation())) {
				return true;
			}
		}
		return false;

	}

	public boolean CanClaim() {
		return ClaimsLeft() > 0;
	}

	public int ClaimsLeft() {
		return getMaxChunks() - ownedChunks().size();
	}

	public int getMaxChunks() {
		return playersUUIDs.size() * 7;
	}

	public void AddLocationToTeam(Location l) {
		ownedChunkslocations.add(l);
	}

	public void ClaimChunk(Player p, Location l) {
		Chunk c = l.getChunk();
		String path = teamName + ".Last Claimed";

		if (!OwnsChunk(c) && CanClaim()) {
			// if (durationFromLastClaim() <= 0) {
			AddLocationToTeam(c.getBlock(0, 0, 0).getLocation());

			plugin.SendTeamChat(this,
					p.getName() + " claim land for " + teamName + ", you can claim " + String.valueOf(ClaimsLeft())
							+ " chunks of land.");

			plugin.getConfig().set(path, dateFormat.format(cal.getTime()));

			saveTeamChunks();
			// } else {
			// p.sendMessage("You need to wait " +
			// String.valueOf(durationFromLastClaim())
			// + " hour before you can claim land");
			// }
		} else {
			p.sendMessage("Your team cannot claim more land ");
		}
	}

	public String GetChunkWorld() {
		return ownedChunks().get(0).getWorld().getName();
	}

	public void saveTeamChunks() {
		if (ownedChunks().size() == 1) {
			plugin.getConfig().set(teamName + ".World", GetChunkWorld());
		} else if (teamHome != null) {
			plugin.getConfig().set(teamName + ".World", teamHome.getWorld().getName());
		}

		StringBuilder sb = new StringBuilder(50);

		for (Location l : ownedChunkslocations) {
			String ls = plugin.LocationToString(l);
			sb.append(ls + "@C");
		}

		plugin.getConfig().set(teamName + ".Chunks", sb.toString());
		plugin.saveConfig();
	}

	public long durationFromLastClaim() {
		FileConfiguration con = plugin.getConfig();
		String path = teamName + ".Last Claimed";
		Date lastDate = new Date();

		if (con.contains(path)) {
			try {
				if (lastClaimDate != null) {
					lastDate = lastClaimDate;
				} else {
					lastDate = dateFormat.parse(con.getString(path));
					lastClaimDate = lastDate;
				}

				long diff = Calendar.getInstance().getTime().getTime() - lastDate.getTime();

				return timeBetweenClaims() - diff / HOUR;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	public long timeBetweenClaims() {
		return 1L;
	}

	public void RemoveLocationFromTeam(Location l) {
		ownedChunkslocations.remove(l);
	}

	public void removeClaim(Player p, Location l) {
		Chunk c = l.getChunk();
		if (OwnsChunk(c)) {
			if (plugin.playersWithBlockChanges.containsKey(p)) {
				plugin.showClaims(p);
			}

			RemoveLocationFromTeam(c.getBlock(0, 0, 0).getLocation());

			plugin.SendTeamChat(this, p.getName() + " removed claimed land for " + teamName);

			plugin.getConfig().set(teamName + ".Last Claimed", null);
			saveTeamChunks();
		} else {
			p.sendMessage("Your team doesn't own this land");
		}
	}
}
