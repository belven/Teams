package belven.teams;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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

    @SuppressWarnings("deprecation")
    public void PlayerTakenDamage(EntityDamageByEntityEvent event)
    {
        Player damagedPlayer = (Player) event.getEntity();
        Entity damagerEntity = event.getDamager();
        Player damagerPlayer = null;

        if (damagerEntity instanceof Player)
        {
            damagerPlayer = (Player) event.getDamager();
        }
        else if (damagerEntity.getType() == EntityType.ARROW)
        {
            Arrow currentArrow = (Arrow) damagerEntity;

            if (currentArrow.getShooter().getType() == EntityType.PLAYER)
            {
                damagerPlayer = (Player) currentArrow.getShooter();
            }
        }
        else if (damagerEntity.getType() == EntityType.FIREBALL)
        {
            Projectile currentFireball = (Projectile) damagerEntity;

            if (currentFireball instanceof Fireball
                    && currentFireball.getShooter().getType() == EntityType.PLAYER)
            {
                damagerPlayer = (Player) currentFireball.getShooter();
            }
        }

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