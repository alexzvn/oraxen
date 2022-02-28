package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class EvolutionTask extends BukkitRunnable {

    private final FurnitureFactory furnitureFactory;
    private final int delay;

    public static final int LOCK_EVOLUTION = 0;
    public static final int UNLOCK_EVOLUTION = 1;

    public EvolutionTask(FurnitureFactory furnitureFactory, int delay) {
        this.furnitureFactory = furnitureFactory;
        this.delay = delay;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds())
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class))
                runEachFrame(frame, world);
    }

    protected void runEachFrame(ItemFrame frame, World world) {
        NamespacedKey evolutionKey = FurnitureMechanic.EVOLUTION_KEY;
        NamespacedKey furnitureKey = FurnitureMechanic.FURNITURE_KEY;
        NamespacedKey lockEvolutionKey = FurnitureMechanic.LOCK_EVOLUTION_KEY;
        PersistentDataContainer data = frame.getPersistentDataContainer();

        String itemID = data.get(furnitureKey, PersistentDataType.STRING);
        FurnitureMechanic mechanic = (FurnitureMechanic) furnitureFactory.getMechanic(itemID);

        if (mechanic.farmlandRequired && frame.getLocation().clone().subtract(0, 1, 0).getBlock().getType() != Material.FARMLAND) {
            return;
        }

        EvolvingFurniture evolution = mechanic.getEvolution();
        int evolutionStep = data.get(evolutionKey, PersistentDataType.INTEGER) + delay * frame.getLocation().getBlock().getLightLevel();
        float rotation = mechanic.getYaw(frame.getRotation());

        if (evolutionStep < evolution.getDelay()) {
            data.set(evolutionKey, PersistentDataType.INTEGER, evolutionStep);
            return;
        }

        if (evolution.isRequiredItemToNextStage()) {
            if (! data.has(lockEvolutionKey, PersistentDataType.INTEGER)) {
                data.set(lockEvolutionKey, PersistentDataType.INTEGER, LOCK_EVOLUTION);
            }

            if (data.get(lockEvolutionKey, PersistentDataType.INTEGER) != UNLOCK_EVOLUTION) {
                return;
            }
        }

        if (! evolution.bernoulliTest()) {
            return;
        }


        if (mechanic.hasBarriers()) {
            mechanic.removeSolid(world, new BlockLocation(frame.getLocation()), rotation);
            return;
        }

        mechanic.removeAirFurniture(frame);
        FurnitureMechanic nextMechanic = (FurnitureMechanic) furnitureFactory.getMechanic(evolution.getNextStage());
        nextMechanic.place(frame.getRotation(),rotation, frame.getFacing(), frame.getLocation(), null);
    }
}
