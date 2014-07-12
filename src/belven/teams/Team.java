package belven.teams;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

import belven.teams.TeamManager.TeamRank;

public class Team
{
    public String teamName;
    public HashMap<Player, TeamRank> members = new HashMap<Player, TeamRank>();

    public TeamManager plugin;
    public boolean friendlyFire = false;
    public boolean isOpen = true;

    public Team(TeamManager tm, String tn)
    {
        teamName = tn;
        plugin = tm;
        plugin.CurrentTeams.add(this);
    }

    public Team(TeamManager tm, String tn, List<Player> players)
    {
        this(tm, tn);

        for (Player p : players)
        {
            members.put(p, TeamRank.MEMBER);
        }

        if (players.size() > 0)
        {
            members.put(players.get(0), TeamRank.LEADER);
        }
    }

    public Team(TeamManager tm, String tn, Player p)
    {
        this(tm, tn);
        members.put(p, TeamRank.LEADER);
    }

    public boolean Contains(Player p)
    {
        return members.keySet().contains(p);
    }

    public void RemoveMember(Player p)
    {
        members.remove(p);
    }

    public Set<Player> getMembers()
    {
        return members.keySet();
    }

    public TeamRank getRank(Player p)
    {
        return members.get(p);
    }

    public void SetLeader(Player p)
    {
        for (Player pl : members.keySet())
        {
            if (members.get(pl) == TeamRank.LEADER)
            {
                SetRank(pl, TeamRank.MEMBER);
            }
        }
        SetRank(p, TeamRank.LEADER);
    }

    public void SetRank(Player p, TeamRank tr)
    {
        Add(p, tr);
    }

    public void Leave(Player p)
    {
        members.remove(p);
    }

    public void RemoveTeam()
    {
        plugin.CurrentTeams.remove(this);
    }

    public void Add(Player p, TeamRank tr)
    {
        members.put(p, tr);
    }
}
