package belven.teams.listeners;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import resources.MaterialFunctions;
import belven.teams.PlayerTeamData.CHATLVL;
import belven.teams.Team;
import belven.teams.TeamManager;

public class PlayerListener implements Listener {
	private final TeamManager plugin;

	public PlayerListener(TeamManager instance) {
		plugin = instance;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if (event.getEntityType() == EntityType.PLAYER) {
			PlayerTakenDamage(event);
		}
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (MaterialFunctions.isInteractiveBlock(event.getClickedBlock().getType())) {
				Chunk c = event.getClickedBlock().getChunk();
				Player p = event.getPlayer();
				if (plugin.teamOwnsChunk(c)) {
					if (!plugin.isInATeam(p) || plugin.getTeam(p) != plugin.getChunkOwner(c)) {
						event.setCancelled(true);
						p.sendMessage("You can't use blocks that your team deson't own");
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerChatEvent(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if (plugin.isInATeam(p)) {
			Team t = plugin.getTeam(p);
			if (t.pData.get(p).chatLvl == CHATLVL.Team) {
				plugin.SendTeamChat(p, e.getMessage());
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		Chunk c = event.getBlock().getChunk();
		Player p = event.getPlayer();
		if (plugin.teamOwnsChunk(c)) {
			if (!plugin.isInATeam(p) || plugin.getTeam(p) != plugin.getChunkOwner(c)) {
				event.setCancelled(true);
				p.sendMessage("You can't break blocks that your team deson't own");
			}
		}
	}

	@EventHandler
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		Chunk c = event.getBlock().getChunk();
		Player p = event.getPlayer();
		if (plugin.teamOwnsChunk(c)) {
			if (!plugin.isInATeam(p) || plugin.getTeam(p) != plugin.getChunkOwner(c)) {
				event.setCancelled(true);
				p.sendMessage("You can't place blocks in land your team deson't own");
			}
		}
	}

	@EventHandler
	public void onPlayerMoveEvent(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		Chunk c = p.getLocation().getChunk();
		TeamManager tm = plugin;
		Team owningTeam = tm.getChunkOwner(c);

		// Someone owns the chunk
		if (owningTeam != null) {
			if (tm.isPlayerInTeamLand(p)) {
				if (!tm.getPlayersCurrentTeamLand(p).equals(owningTeam)) {
					tm.playerLeftTeamLand(p);
				}
			} else {
				tm.playerEnteredTeamLand(p);
			}
		} else {
			tm.playerLeftTeamLand(p);
		}

	}

	@EventHandler
	public void onPlayerLoginEvent(PlayerLoginEvent event) {
		plugin.AddPlayerToTeamFromConfig(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
		plugin.RemovePlayerFromTeam(event.getPlayer());
	}

	public void PlayerTakenDamage(EntityDamageByEntityEvent event) {
		Player damagedPlayer = (Player) event.getEntity();
		Entity damagerEntity = event.getDamager();
		Player damagerPlayer = null;

		if (damagerEntity instanceof Player) {
			damagerPlayer = (Player) event.getDamager();
		}

		if (damagerPlayer != null) {
			if (plugin.isInSameTeam(damagedPlayer, damagerPlayer)) {
				if (!plugin.getTeam(damagedPlayer).friendlyFire) {
					event.setDamage(0.0);
					event.setCancelled(true);
				} else {
					event.setDamage(event.getDamage() / 2);
				}
			}
		}
	}
}