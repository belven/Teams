package belven.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import belven.teams.listeners.PlayerListener;

public class TeamManager extends JavaPlugin
{
    public List<Team> CurrentTeams = new ArrayList<Team>();
    private final PlayerListener playerListener = new PlayerListener(this);

    public enum TeamRank
    {
        LEADER, MEMBER
    }

    @Override
    public void onEnable()
    {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(playerListener, this);
        RecreateTeams();
    }

    public void RecreateTeams()
    {
        reloadConfig();
        for (String s : getConfig().getKeys(false))
        {
            getLogger().info(s);
            Team t = new Team(this, s);

            t.friendlyFire = getConfig().getBoolean(
                    t.teamName + ".FriendlyFire");
            t.isOpen = getConfig().getBoolean(t.teamName + ".Open");
        }
    }

    public void AddPlayerToTeam(Player p)
    {
        for (Team t : CurrentTeams)
        {
            Set<String> teamPlayers = getConfig().getConfigurationSection(
                    t.teamName + ".Players").getKeys(false);
            for (String tp : teamPlayers)
            {
                getLogger().info("Player found " + tp);

                if (p.getUniqueId().equals(UUID.fromString(tp)))
                {
                    getLogger().info(
                            "Player " + p.getName() + " was added to team "
                                    + t.teamName);

                    String something = getConfig().getString(
                            t.teamName + ".Players." + tp);

                    getLogger().info("Player was givin Rank: " + something);
                    t.Add(p, TeamRank.valueOf(something));
                }
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label,
            String[] args)
    {
        Player p = (Player) sender;
        String commandSent = cmd.getName();

        if (commandSent.equalsIgnoreCase("bt"))
        {
            if (args.length < 1)
            {
                return false;
            }

            if (args[0].equalsIgnoreCase("jt")
                    || args[0].equalsIgnoreCase("jointeam"))
            {
                joinTeam(p, args[1]);
                return true;
            }
            else if (args[0].equalsIgnoreCase("lt")
                    || args[0].equalsIgnoreCase("leaveteam"))
            {
                leaveTeam(p);
                return true;
            }
            else if (args[0].equalsIgnoreCase("ct")
                    || args[0].equalsIgnoreCase("createteam"))
            {
                p.sendMessage(createTeam(p, args[1]));
                return true;
            }
            else if (args[0].equalsIgnoreCase("sl")
                    || args[0].equalsIgnoreCase("setleader"))
            {
                p.sendMessage(setTeamLeader(p, args[1]));
                return true;
            }
            else if (args[0].equalsIgnoreCase("so")
                    || args[0].equalsIgnoreCase("setopen"))
            {
                p.sendMessage(setOpen(p, args[1]));
                return true;
            }
            else if (args[0].equalsIgnoreCase("sff")
                    || args[0].equalsIgnoreCase("setff"))
            {
                p.sendMessage(setfriendlyFire(p, args[1]));
                return true;
            }
            else if (args[0].equalsIgnoreCase("rm")
                    || args[0].equalsIgnoreCase("removemember"))
            {
                p.sendMessage(removeMember(p, args[1]));
                return true;
            }
            else if (args[0].equalsIgnoreCase("rt")
                    || args[0].equalsIgnoreCase("removeteam"))
            {
                removeTeam(p);
                return true;
            }
        }
        else if (commandSent.equalsIgnoreCase("lt")
                || commandSent.equalsIgnoreCase("listteams"))
        {
            listTeams(p);
            return true;
        }
        else if (commandSent.equalsIgnoreCase("lm")
                || commandSent.equalsIgnoreCase("listmembers"))
        {
            listMembers(p);
            return true;
        }
        else if (commandSent.equalsIgnoreCase("t")
                || commandSent.equalsIgnoreCase("team"))
        {
            SendTeamChat(p, args);
            return true;
        }

        return true;
    }

    private void SendTeamChat(Player p, String[] args)
    {
        if (isInATeam(p))
        {
            Team t = getTeam(p);

            String message = "";

            for (String s : args)
            {
                message += s + " ";
            }

            for (Player pl : t.getMembers())
            {
                pl.sendMessage(ChatColor.BLUE + message);
            }
        }

    }

    private String setfriendlyFire(Player p, String bool)
    {
        if (isInATeam(p))
        {
            Team t = getTeam(p);
            if (t.getRank(p) == TeamRank.LEADER)
            {
                boolean friendlyFire = Boolean.valueOf(bool);
                t.friendlyFire = friendlyFire;

                getConfig().set(t.teamName + ".FriendlyFire", friendlyFire);
                return "Team "
                        + t.teamName
                        + " is now "
                        + (friendlyFire ? "Friendly Fire is now on"
                                : "Friendly Fire is now off");
            }
            else
            {
                return "Only leaders can do this!!";
            }
        }
        return "You must be in a team to do this.";
    }

    private boolean joinTeam(Player p, String tn)
    {
        Team t = getTeam(tn);
        if (t != null)
        {
            if (t.isOpen)
            {
                p.sendMessage("You have joined Team: " + tn);
                return AddPlayerToTeam(p, t);
            }
            else
            {
                p.sendMessage("You cannot join closed teams");
                return true;
            }
        }
        else
        {
            p.sendMessage("Team: " + tn + " does not exist");
            return false;
        }
    }

    public boolean isInSameTeam(Player p1, Player p2)
    {
        if (!isInATeam(p1) || !isInATeam(p2))
        {
            return false;
        }

        Team p1Team = getTeam(p1);
        Team p2Team = getTeam(p2);

        if (p1Team == p2Team)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public String setOpen(Player p, String bool)
    {
        if (isInATeam(p))
        {
            Team t = getTeam(p);
            if (t.getRank(p) == TeamRank.LEADER)
            {
                boolean open = Boolean.valueOf(bool);
                t.isOpen = open;

                getConfig().set(t.teamName + ".Open", open);

                return "Team " + t.teamName + " is now "
                        + (open ? "Open" : "Closed");
            }
            else
            {
                return "Only leaders can do this!!";
            }
        }
        return "You must be in a team to do this.";
    }

    public String removeTeam(Player p)
    {
        if (isInATeam(p))
        {
            Team t = getTeam(p);

            if (t.getRank(p) == TeamRank.LEADER)
            {
                for (Player pl : t.getMembers())
                {
                    pl.sendMessage("Team " + t.teamName + " was removed");
                }

                t.RemoveTeam();

                return "Team " + t.teamName + " was removed";
            }
            else
            {
                return "Only leaders can do this!!";
            }
        }
        return "You must be in a team to do this.";
    }

    public String removeMember(Player p, String playerName)
    {
        if (isInATeam(p))
        {
            Team t = getTeam(p);
            if (t.getRank(p) == TeamRank.LEADER)
            {
                Player otherPlayer = getPlayerFromString(playerName);

                // is the other player online/exist
                if (otherPlayer != null)
                {
                    // is the other player in the team
                    if (t.Contains(otherPlayer))
                    {
                        t.RemoveMember(otherPlayer);
                    }
                }
                return "The player " + playerName + " is not online";
            }
            return "Only leaders can do this!!";

        }
        return "You must be in a team to do this!!";
    }

    public boolean AddPlayerToTeam(Player p, Team t)
    {
        if (t != null && p != null)
        {
            t.Add(p, TeamRank.MEMBER);

            for (Player pl : t.getMembers())
            {
                pl.sendMessage(p.getName() + " has joined the team!!");
            }

            return true;
        }

        return false;
    }

    public void leaveTeam(Player p)
    {
        if (isInATeam(p))
        {
            getTeam(p).Leave(p);
        }
    }

    public String setTeamLeader(Player p, String playerName)
    {
        // is the player in a team?
        if (isInATeam(p))
        {
            Team t = getTeam(p);
            Player otherPlayer = getPlayerFromString(playerName);

            // is the other player online/exist
            if (otherPlayer != null)
            {
                // is the other player in the team
                if (t.Contains(otherPlayer))
                {
                    t.SetLeader(otherPlayer);
                    return p.getName() + " is now the leader of team "
                            + t.teamName;
                }
                else
                {
                    return p.getName() + " is not in the team " + t.teamName;
                }
            }
            else
            {
                return "The player " + playerName + " is not online";
            }
        }
        return "You must be in a team to do this!!";
    }

    @SuppressWarnings("deprecation")
    public Player getPlayerFromString(String pn)
    {
        for (Player p : getServer().getOnlinePlayers())
        {
            if (p.getName().equals(pn))
            {
                return p;
            }
        }
        return null;
    }

    public String createTeam(Player p, String tn)
    {
        reloadConfig();
        if (!deosTeamExist(tn) && !tn.isEmpty())
        {
            if (!isInATeam(p))
            {
                Team t = new Team(this, tn, p);

                getConfig().set(t.teamName + ".Open", t.isOpen);
                getConfig().set(t.teamName + ".FriendlyFire", t.friendlyFire);

                return "Team " + tn
                        + " was created and you are now the leader!!";
            }
            else
            {
                return "You must first leave your current team in order to create a new one";
            }
        }
        else
        {
            return "Team " + tn + " already exists";
        }
    }

    public boolean deosTeamExist(String tn)
    {
        for (Team t : CurrentTeams)
        {
            if (t.teamName.equalsIgnoreCase(tn))
            {
                return true;
            }
        }
        return false;
    }

    public Team getTeam(String tn)
    {
        for (Team t : CurrentTeams)
        {
            if (t.teamName.equalsIgnoreCase(tn))
            {
                return t;
            }
        }
        return null;
    }

    public boolean isInATeam(Player p)
    {
        for (Team t : CurrentTeams)
        {
            if (t.Contains(p))
            {
                return true;
            }
        }
        return false;
    }

    public void listTeams(Player p)
    {
        for (Team t : CurrentTeams)
        {
            if (t.isOpen)
            {
                p.sendMessage(t.teamName);
            }
        }
    }

    public void listMembers(Player p)
    {
        if (isInATeam(p))
        {
            Team t = getTeam(p);

            for (Player pl : t.getMembers())
            {
                p.sendMessage(pl.getName() + " Rank: " + t.getRank(pl));
            }
        }
        else
        {
            p.sendMessage("You must be in a team to do this!!");
        }
    }

    public Team getTeam(Player p)
    {
        for (Team t : CurrentTeams)
        {
            if (t.Contains(p))
            {
                return t;
            }
        }
        return null;
    }

    @Override
    public void onDisable()
    {
        getLogger().info("Goodbye world!");
        this.saveConfig();
    }
}