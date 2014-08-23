package belven.teams;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import belven.teams.PlayerTeamData.CHATLVL;
import belven.teams.listeners.PlayerListener;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class TeamManager extends JavaPlugin {
	public List<Team> CurrentTeams = new ArrayList<Team>();
	//public HashMap<Chunk, Team> TeamChunks = new HashMap<Chunk, Team>();
	private final PlayerListener playerListener = new PlayerListener(this);
	public HashMap<Player, Team> playersInTeamLand = new HashMap<Player, Team>();

	public enum TeamRank {
		LEADER, MEMBER, OFFICER
	}

	private static HashMap<TeamRank, String> TeamRankfriendlyNames = new HashMap<TeamRank, String>();
	private static HashMap<TeamRank, List<String>> TeamRankCommandPerms = new HashMap<TeamRank, List<String>>();
	private static HashMap<String, List<String>> CommandAlisases = new HashMap<String, List<String>>();

	private static HashMap<String, Boolean> friendlyFire = new HashMap<String, Boolean>();
	private static List<String> MemberCommands = new ArrayList<String>();
	private static List<String> OfficerCommands = new ArrayList<String>();
	private static List<String> LeaderCommands = new ArrayList<String>();
	HashMap<Player, Integer> playersWithBlockChanges = new HashMap<Player, Integer>();
	DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	WorldGuardPlugin wg = (WorldGuardPlugin) getServer().getPluginManager()
			.getPlugin("WorldGuard");

	static {
		CommandAlisases
				.put("jointeam", Arrays.asList("jointeam", "join", "jt"));

		CommandAlisases.put("leaveteam",
				Arrays.asList("leaveteam", "leave", "lt"));

		CommandAlisases.put("list",
				Arrays.asList("list", "lst", "showteams", "teams"));

		CommandAlisases.put("listmembers",
				Arrays.asList("listmembers", "lm", "listmem", "members"));

		CommandAlisases.put("chat", Arrays.asList("t", "c", "w", "pm"));

		CommandAlisases.put("teamchat", Arrays.asList("teamchat", "tc"));
		CommandAlisases.put("globalchat", Arrays.asList("globalchat", "gc"));

		CommandAlisases.put("claimchunk",
				Arrays.asList("claimchunk", "cc", "claim"));

		CommandAlisases.put("removeclaim",
				Arrays.asList("removeclaim", "rc", "unclaim", "uc"));

		CommandAlisases.put("setopen", Arrays.asList("setopen", "so"));

		CommandAlisases.put("setfriendlyfire",
				Arrays.asList("setfriendlyfire", "setff", "sff"));

		CommandAlisases.put("removemember",
				Arrays.asList("removemember", "rm", "kick"));

		CommandAlisases.put("setleader", Arrays.asList("setleader", "sl"));

		CommandAlisases.put("removeteam",
				Arrays.asList("removeteam", "rt", "disband"));

		// Arrays.asList("jointeam", "join", "jt",
		// "lt", "leaveteam", "leave", "list", "lst", "showteams",
		// "teams", "lm", "listmem", "listmembers", "members", "teamchat",
		// "tc", "globalchat", "gc");

		MemberCommands.addAll(CommandAlisases.get("jointeam"));
		MemberCommands.addAll(CommandAlisases.get("leaveteam"));
		MemberCommands.addAll(CommandAlisases.get("listmembers"));
		MemberCommands.addAll(CommandAlisases.get("list"));
		MemberCommands.addAll(CommandAlisases.get("chat"));
		MemberCommands.addAll(CommandAlisases.get("teamchat"));
		MemberCommands.addAll(CommandAlisases.get("globalchat"));

		// Arrays.asList("cc", "claim",
		// "claimchunk", "rc", "uc", "unclaim", "removeclaim", "so",
		// "setopen", "sff", "setff", "setfriendlyfire", "rm",
		// "removemember", "kick");
		OfficerCommands.addAll(CommandAlisases.get("claimchunk"));
		OfficerCommands.addAll(CommandAlisases.get("removeclaim"));
		OfficerCommands.addAll(CommandAlisases.get("setopen"));
		OfficerCommands.addAll(CommandAlisases.get("setfriendlyfire"));
		OfficerCommands.addAll(CommandAlisases.get("removemember"));

		OfficerCommands.addAll(MemberCommands);

		LeaderCommands.addAll(CommandAlisases.get("setleader"));
		LeaderCommands.addAll(CommandAlisases.get("removeteam"));
		// Arrays.asList("sl", "setleader", "rt",
		// "removeteam", "disband");

		LeaderCommands.addAll(OfficerCommands);

		friendlyFire.put("on", true);
		friendlyFire.put("true", true);
		friendlyFire.put("off", false);
		friendlyFire.put("false", false);
		TeamRankfriendlyNames.put(TeamRank.LEADER, "Leader");
		TeamRankfriendlyNames.put(TeamRank.MEMBER, "Member");
		TeamRankfriendlyNames.put(TeamRank.OFFICER, "Officer");

		TeamRankCommandPerms.put(TeamRank.MEMBER, MemberCommands);
		TeamRankCommandPerms.put(TeamRank.OFFICER, OfficerCommands);
		TeamRankCommandPerms.put(TeamRank.LEADER, LeaderCommands);
	}

	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(playerListener, this);
		RecreateTeams();
		wg = (WorldGuardPlugin) getServer().getPluginManager().getPlugin(
				"WorldGuard");
	}

	public void RecreateTeams() {
		reloadConfig();
		for (String s : getConfig().getKeys(false)) {
			getLogger().info(s);
			Team t = new Team(this, s);

			t.friendlyFire = getConfig().getBoolean(
					t.teamName + ".FriendlyFire");
			t.isOpen = getConfig().getBoolean(t.teamName + ".Open");

			ConfigurationSection config = getConfig().getConfigurationSection(
					t.teamName + ".Players");
			if (config != null) {
				Set<String> teamPlayers = config.getKeys(false);
				for (String tp : teamPlayers) {
					if (!t.playersUUIDs.containsKey(tp)) {
						String rank = getConfig().getString(
								t.teamName + ".Players." + tp);
						t.playersUUIDs.put(tp, rank);
					}
				}
			}

			if (getConfig().contains(s + ".Last Claimed")) {
				Date lastDate;
				try {
					lastDate = dateFormat.parse(getConfig().getString(
							s + ".Last Claimed"));
					t.lastClaimDate = lastDate;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}

			if (getConfig().contains(t.teamName + ".Chunks")
					&& getConfig().contains(t.teamName + ".World")) {
				String chunksList = getConfig().getString(
						t.teamName + ".Chunks");
				String worldName = getConfig().getString(t.teamName + ".World");

				World world = this.getServer().getWorld(worldName);

				if (world == null) {
					WorldCreator wc = new WorldCreator(worldName);
					world = this.getServer().createWorld(wc);

					if (world == null) {
						return;
					}
				}
				getLogger().info(chunksList);
				for (String chunk : chunksList.split("@C")) {
					Location l = StringToLocation(chunk, world);
					t.AddChunkToTeam(l.getBlock().getChunk());
				}
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		Player p = (Player) sender;
		String commandSent = cmd.getName();

		if (!commandSent.toLowerCase().equals("bt")) {
			return false;
		}

		switch (args[0].toLowerCase()) {
		case "jt":
		case "jointeam":
		case "join":
			joinTeam(p, args[1]);
			return true;

		case "lc":
		case "listcommands":
			listCommands(p);
			return true;

		case "lt":
		case "leaveteam":
		case "leave":
			leaveTeam(p);
			return true;

		case "cc":
		case "claim":
		case "claimchunk":
			claimChunk(p);
			return true;

		case "sc":
		case "showclaims":
			showClaims(p);
			return true;

		case "rc":
		case "uc":
		case "unclaim":
		case "removeclaim":
			removeClaim(p);
			return true;

		case "ttc":
		case "teleporttoclaim":
			teleportToClaim(p);
			return true;

		case "ct":
		case "createteam":
		case "create":
			p.sendMessage(createTeam(p, args[1]));
			return true;

		case "sl":
		case "setleader":
			p.sendMessage(setTeamLeader(p, args[1]));
			return true;

		case "so":
		case "setopen":
			p.sendMessage(setOpen(p, args[1]));
			return true;

		case "sff":
		case "setff":
		case "setfriendlyfire":
			p.sendMessage(setFriendlyFire(p, args[1]));
			return true;

		case "rm":
		case "removemember":
		case "kick":
			p.sendMessage(removeMember(p, args[1]));
			return true;

		case "rt":
		case "removeteam":
		case "disband":
			removeTeam(p);
			return true;

		case "list":
		case "lst":
		case "showteams":
		case "teams":
			listTeams(p);
			return true;

		case "lm":
		case "listmem":
		case "listmembers":
		case "members":
			listMembers(p);
			return true;

		case "t":
		case "c":
		case "w":
		case "pm":
			args[0] = "";
			SendTeamChat(p, appendMessage(args));
			return true;

		case "teamchat":
		case "tc":
			if (isInATeam(p)) {
				getTeam(p).pData.get(p).chatLvl = CHATLVL.Team;
				p.sendMessage("Chat channel changed to Team");
			}
			return true;

		case "globalchat":
		case "gc":
			getTeam(p).pData.get(p).chatLvl = CHATLVL.Global;
			p.sendMessage("Chat channel changed to Global");
			return true;
		}
		return false;
	}

	private void teleportToClaim(Player p) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			double lastDist = 0;
			Location lastLocation = null;
			for (Chunk c : t.ownedChunks) {
				Block b = c.getBlock(0, 70, 0);

				if (lastLocation != null) {
					if (p.getLocation().distance(b.getLocation()) < lastDist) {
						lastLocation = b.getLocation();
						lastDist = p.getLocation().distance(b.getLocation());
					}
				} else {
					lastLocation = b.getLocation();
					lastDist = p.getLocation().distance(b.getLocation());
				}
			}

			if (lastLocation != null) {
				p.sendMessage("Teleporting to nearest claim");
				p.teleport(lastLocation);
			} else {
				p.sendMessage("No claims found");
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void showClaims(Player p) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			int y = 0;

			if (playersWithBlockChanges.containsKey(p)) {
				y = playersWithBlockChanges.get(p);
			} else {
				y = (int) p.getLocation().getY() + 2;
			}

			Material m = Material.REDSTONE_BLOCK;

			for (Chunk c : t.ownedChunks) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						if (z == 15 || z == 0 || x == 15 || x == 0) {
							Block b = c.getBlock(x, y, z);
							if (!playersWithBlockChanges.containsKey(p)) {
								p.sendBlockChange(b.getLocation(), m,
										b.getData());
							} else {
								p.sendBlockChange(b.getLocation(), b.getType(),
										b.getData());
							}
						}
					}
				}
			}

			if (!playersWithBlockChanges.containsKey(p)) {
				playersWithBlockChanges.put(p, y);
			} else {
				playersWithBlockChanges.remove(p);
			}
		}
	}

	private void listCommands(Player p) {
		if (isInATeam(p)) {

			List<String> commands = TeamRankCommandPerms.get(getTeam(p)
					.getRank(p));

			StringBuilder sb = new StringBuilder(commands.size()
					+ (commands.size() * 3));

			for (String s : commands) {
				sb.append(s + ", ");
			}

			p.sendMessage(sb.toString());
		}
	}

	private void removeClaim(Player p) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			t.removeClaim(p, p.getLocation());
		} else {
			p.sendMessage("You must be in a team to do this");
		}
	}

	private void claimChunk(Player p) {
		Location l = p.getLocation();

		if (isInATeam(p)) {
			if (canClaim(p, l.getChunk())) {
				if (!teamOwnsChunk(l.getChunk())) {
					Team t = getTeam(p);
					t.ClaimChunk(p, l);
				} else {
					Team t = getChunkOwner(l.getChunk());
					p.sendMessage(t.teamName + " owns this land");
				}
			} else {
				p.sendMessage("You can't claim world guard protected land");
			}

		} else {
			p.sendMessage("You must be in a team to do this");
		}
	}

	private boolean canClaim(Player p, Chunk c) {
		if (wg != null) {
			for (int x = 0; x <= 15; x++) {
				for (int z = 0; z <= 15; z++) {
					for (int y = 0; y <= 127; y++) {
						Block b = c.getBlock(x, y, z);
						if (!wg.canBuild(p, b)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private String appendMessage(String[] args) {
		StringBuilder sb = new StringBuilder(50);
		sb.append(ChatColor.GREEN);
		for (String s : args) {
			sb.append(s).append(' ');
		}
		return sb.toString();
	}

	public String TeamChatFormat(Player p, Team t) {
		return ChatColor.GREEN + "["
				+ TeamRankfriendlyNames.get(t.pData.get(p).teamRank) + "] "
				+ p.getName() + ": ";
	}

	public void SendTeamChat(Team t, String msg) {
		if (msg != "") {
			for (Player pl : t.getMembers()) {
				pl.sendMessage(msg);
			}
		}
	}

	public void SendTeamChat(Player p, String msg) {
		if (isInATeam(p) && msg != "") {
			Team t = getTeam(p);
			SendTeamChat(t, TeamChatFormat(p, t) + msg);
		}
	}

	private String setFriendlyFire(Player p, String bool) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			if (t.getRank(p) == TeamRank.LEADER) {
				if (friendlyFire.containsKey(bool)
						|| bool.equalsIgnoreCase("toggle")) {

					t.friendlyFire = bool.equals("toggle") ? !t.friendlyFire
							: friendlyFire.get(bool);

					getConfig().set(t.teamName + ".FriendlyFire", friendlyFire);
					return "Team "
							+ t.teamName
							+ " is now "
							+ (t.friendlyFire ? "Friendly Fire is now on"
									: "Friendly Fire is now off");
				} else {
					return "Accepts the values on, off, true, false and toggle.";
				}

			} else {
				return "Only leaders can do this.";
			}
		}
		return "You must be in a team to do this.";
	}

	private boolean joinTeam(Player p, String tn) {
		Team t = getTeam(tn);

		if (t != null) {
			if (t.isOpen) {
				p.sendMessage("You have joined Team: " + tn);
				return AddPlayerToTeam(p, t);
			} else {
				p.sendMessage("You cannot join closed teams.");
				return true;
			}
		} else {
			p.sendMessage("Team: " + tn + " does not exist");
			return false;
		}
	}

	public boolean isInSameTeam(Player p1, Player p2) {
		return isInATeam(p1) && isInATeam(p2) && getTeam(p1) == getTeam(p2);
	}

	public String setOpen(Player p, String bool) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			if (t.getRank(p) == TeamRank.LEADER) {
				boolean open = Boolean.valueOf(bool);
				t.isOpen = open;

				getConfig().set(t.teamName + ".Open", open);

				return "Team " + t.teamName + " is now "
						+ (open ? "Open" : "Closed");
			} else {
				return "Only leaders can do this.";
			}
		}
		return "You must be in a team to do this.";
	}

	public String removeTeam(Player p) {
		if (isInATeam(p)) {
			Team t = getTeam(p);

			if (t.getRank(p) == TeamRank.LEADER) {
				for (Player pl : t.getMembers()) {
					pl.sendMessage("Team " + t.teamName + " was removed");
				}

				t.RemoveTeam();

				return "Team " + t.teamName + " was removed";
			} else {
				return "Only leaders can do this.";
			}
		}
		return "You must be in a team to do this.";
	}

	public String removeMember(Player p, String playerName) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			if (t.getRank(p) == TeamRank.LEADER) {
				Player otherPlayer = getPlayerFromString(playerName);

				// is the other player online/exist
				if (otherPlayer != null) {
					// is the other player in the team
					if (t.Contains(otherPlayer)) {
						t.RemoveMember(otherPlayer);
					}
				}
				return "The player " + playerName + " is not online";
			}
			return "Only leaders can do this";

		}
		return "You must be in a team to do this";
	}

	public void AddPlayerToTeamFromConfig(Player p) {
		for (Team t : CurrentTeams) {
			if (t.playersUUIDs.containsKey(p.getUniqueId().toString())) {
				String rank = getConfig().getString(
						t.teamName + ".Players." + p.getUniqueId().toString());
				t.Add(p, TeamRank.valueOf(rank));
			}
		}
	}

	public void RemovePlayerFromTeam(Player p) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			t.pData.remove(p);

			if (playersInTeamLand.containsKey(p)) {
				playersInTeamLand.remove(p);
			}
		}
	}

	public boolean AddPlayerToTeam(Player p, Team t) {
		if (t != null && p != null) {
			t.Add(p, TeamRank.MEMBER);

			for (Player pl : t.getMembers()) {
				pl.sendMessage(p.getName() + " has joined the team");
			}
			return true;
		}
		return false;
	}

	public void leaveTeam(Player p) {
		if (isInATeam(p)) {
			getTeam(p).Leave(p);
		}
	}

	public String setTeamLeader(Player p, String playerName) {
		// is the player in a team?
		if (isInATeam(p)) {
			Team t = getTeam(p);
			Player otherPlayer = getPlayerFromString(playerName);

			// is the other player online/exist
			if (otherPlayer != null) {
				// is the other player in the team
				if (t.Contains(otherPlayer)) {
					t.SetLeader(otherPlayer);
					return p.getName() + " is now the leader of team "
							+ t.teamName;
				} else {
					return p.getName() + " is not in the team " + t.teamName;
				}
			} else {
				return "The player " + playerName + " is not online";
			}
		}
		return "You must be in a team to do this";
	}

	@SuppressWarnings("deprecation")
	public Player getPlayerFromString(String pn) {
		for (Player p : getServer().getOnlinePlayers()) {
			if (p.getName().equals(pn)) {
				return p;
			}
		}
		return null;
	}

	public String createTeam(Player p, String tn) {
		reloadConfig();
		if (!deosTeamExist(tn) && !tn.isEmpty()) {
			if (!isInATeam(p)) {
				Team t = new Team(this, tn, p);
				getConfig().set(t.teamName + ".Open", t.isOpen);
				getConfig().set(t.teamName + ".FriendlyFire", t.friendlyFire);
				return "Team " + tn + " was created and you are now the leader";
			} else {
				return "You must first leave your current team in order to create a new one";
			}
		} else {
			return "Team " + tn + " already exists";
		}
	}

	public boolean deosTeamExist(String tn) {
		for (Team t : CurrentTeams) {
			if (t.teamName.equalsIgnoreCase(tn)) {
				return true;
			}
		}
		return false;
	}

	public Team getTeam(String tn) {
		for (Team t : CurrentTeams) {
			if (t.teamName.equalsIgnoreCase(tn)) {
				return t;
			}
		}
		return null;
	}

	public boolean isInATeam(Player p) {
		for (Team t : CurrentTeams) {
			if (t.Contains(p)) {
				return true;
			}
		}
		return false;
	}

	public void listTeams(Player p) {
		int count = 0;
		for (Team t : CurrentTeams) {
			if (t.isOpen) {
				count++;
				p.sendMessage(t.teamName);
			}
		}

		if (count == 0) {
			p.sendMessage("There are no open teams");
		}
	}

	public void listMembers(Player p) {
		if (isInATeam(p)) {
			Team t = getTeam(p);
			for (Player pl : t.getMembers()) {
				p.sendMessage(pl.getName() + " Rank: "
						+ TeamRankfriendlyNames.get(t.getRank(pl)));
			}
		} else {
			p.sendMessage("You must be in a team to do this");
		}
	}

	public Team getTeam(Player p) {
		for (Team t : CurrentTeams) {
			if (t.Contains(p)) {
				return t;
			}
		}
		return null;
	}

	public Team getChunkOwner(Chunk c) {
		for (Team t : CurrentTeams) {
			for (Chunk tc : t.ownedChunks) {
				if (tc.equals(c)) {
					return t;
				}
			}
		}
		return null;
	}

	@Override
	public void onDisable() {
		for (Team t : CurrentTeams) {
			t.SaveTeam();
		}

		this.saveConfig();
	}

	public boolean teamOwnsChunk(Chunk c) {

		for (Team t : CurrentTeams) {
			for (Chunk tc : t.ownedChunks) {
				if (tc.equals(c)) {
					return true;
				}
			}
		}
		return false;
		// return TeamChunks.containsKey(c);
	}

	private Location StringToLocation(String s, World world) {
		Location tempLoc;
		String[] strings = s.split(",");
		int x = Integer.valueOf(strings[0].trim());
		int y = Integer.valueOf(strings[1].trim());
		int z = Integer.valueOf(strings[2].trim());
		tempLoc = new Location(world, x, y, z);
		return tempLoc;
	}

	public String LocationToString(Location l) {
		String locationString = "";

		if (l != null) {
			locationString = String.valueOf(l.getBlockX()) + ","
					+ String.valueOf(l.getBlockY()) + ","
					+ String.valueOf(l.getBlockZ());
		}
		return locationString;
	}

	public void playerLeftTeamLand(Player p) {
		String tn = playersInTeamLand.get(p).teamName;
		if (getTeam(p) == playersInTeamLand.get(p)) {
			String msg = "You left " + tn + "s land.";
			p.sendMessage(msg);
		} else {
			String msg = "You left " + tn + "s land.";
			p.sendMessage(msg);
			msg = p.getName() + " left your teams land.";
			SendTeamChat(playersInTeamLand.get(p), msg);
		}
		playersInTeamLand.remove(p);
	}

	public void playerEnteredTeamLand(Player p) {
		Chunk c = p.getLocation().getChunk();
		playersInTeamLand.put(p, getChunkOwner(c));
		String tn = getChunkOwner(c).teamName;
		if (getTeam(p) == getChunkOwner(c)) {
			String msg = "You entered " + tn + "s land.";
			p.sendMessage(msg);
		} else {
			String msg = "You entered " + tn + "s land.";
			p.sendMessage(msg);
			msg = p.getName() + " entered your teams land.";
			SendTeamChat(getChunkOwner(c), msg);
		}
	}

}