package belven.teams.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import belven.teams.PlayerTeamData;
import belven.teams.PlayerTeamData.CHATLVL;
import belven.teams.Team;
import belven.teams.TeamManager;

public class PlayerListener implements Listener
{
    private final TeamManager plugin;

    public PlayerListener(TeamManager instance)
    {
        plugin = instance;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event)
    {
        if (event.getEntityType() == EntityType.PLAYER)
        {
            PlayerTakenDamage(event);
        }
    }
    
    @EventHandler
    public void onPlayerChatEvent(AsyncPlayerChatEvent e){
    	Player p = e.getPlayer();
    	
    	if(plugin.isInATeam(p)){
    		Team t = plugin.getTeam(p);
    		
    		if(t.pData.get(p).chatLvl == CHATLVL.Team){
	    		for(Player m : t.getMembers()){
	    			m.sendMessage(ChatColor.GREEN + e.getMessage());
	    		}
	    		
	    		e.setCancelled(true);
    		}
    	}
    }

    @EventHandler
    public void onPlayerLoginEvent(PlayerLoginEvent event)
    {
        plugin.AddPlayerToTeam(event.getPlayer());
    }

    public void PlayerTakenDamage(EntityDamageByEntityEvent event)
    {
        Player damagedPlayer = (Player) event.getEntity();
        Entity damagerEntity = event.getDamager();
        Player damagerPlayer = null;

        if (damagerEntity instanceof Player)
        {
            damagerPlayer = (Player) event.getDamager();
        }
        // else if (damagerEntity.getType() == EntityType.ARROW)
        // {
        // Arrow currentArrow = (Arrow) damagerEntity;
        //
        // if (currentArrow.getShooter().getType() == EntityType.PLAYER)
        // {
        // damagerPlayer = (Player) currentArrow.getShooter();
        // }
        // }
        // else if (damagerEntity.getType() == EntityType.FIREBALL)
        // {
        // Projectile currentFireball = (Projectile) damagerEntity;
        //
        // if (currentFireball instanceof Fireball
        // && currentFireball.getShooter().getType() == EntityType.PLAYER)
        // {
        // damagerPlayer = (Player) currentFireball.getShooter();
        // }
        // }

        if (damagerPlayer != null)
        {
            if (plugin.isInSameTeam(damagedPlayer, damagerPlayer))
            {
                if (!plugin.getTeam(damagedPlayer).friendlyFire)
                {
                    event.setDamage(0.0);
                    event.setCancelled(true);
                }
                else
                {
                    event.setDamage(event.getDamage() / 2);
                }
            }
        }
    }
}