package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;

import java.util.UUID;

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
        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                runEachFrame(frame, world);
            }
        }
    }

    protected void runEachFrame(ItemFrame frame, World world) {
        NamespacedKey evolutionKey = FurnitureMechanic.EVOLUTION_KEY;
        NamespacedKey furnitureKey = FurnitureMechanic.FURNITURE_KEY;
        NamespacedKey lockEvolutionKey = FurnitureMechanic.LOCK_EVOLUTION_KEY;
        NamespacedKey progressTextKey = FurnitureMechanic.PROGESS_TEXT;

        PersistentDataContainer data = frame.getPersistentDataContainer();

        String itemID = data.get(furnitureKey, PersistentDataType.STRING);
        FurnitureMechanic mechanic = (FurnitureMechanic) furnitureFactory.getMechanic(itemID);

        if (mechanic == null || ! data.has(evolutionKey, PersistentDataType.INTEGER)) {
            return; // skip when not mechanic or it's final stage of furniture (no delay config)
        }

        if (mechanic.farmlandRequired && frame.getLocation().clone().subtract(0, 1, 0).getBlock().getType() != Material.FARMLAND) {
            return;
        }

        if (mechanic.hasProgressText() && data.has(progressTextKey, PersistentDataType.STRING) == false) {
            UUID uuid = mechanic.placeProgressText(frame.getLocation());

            data.set(progressTextKey, PersistentDataType.STRING, uuid.toString());
        }

        EvolvingFurniture evolution = mechanic.getEvolution();

        int evolutionStep = data.get(evolutionKey, PersistentDataType.INTEGER) + delay * frame.getLocation().getBlock().getLightLevel();

        if (evolutionStep > evolution.getDelay()) {
            evolutionStep = evolution.getDelay();
        }

        float rotation = mechanic.getYaw(frame.getRotation());

        if (mechanic.hasProgressText()) {
            UUID uuid = UUID.fromString(data.get(progressTextKey, PersistentDataType.STRING));

            mechanic.updateProgressText(uuid, evolutionStep, evolution.getDelay());
        }

        if (evolution.isRequiredItemToNextStage() && ! data.has(lockEvolutionKey, PersistentDataType.INTEGER)) {
            data.set(lockEvolutionKey, PersistentDataType.INTEGER, LOCK_EVOLUTION);
        }

        // AFTER CHECK DELAY
        if (evolutionStep < evolution.getDelay()) {
            data.set(evolutionKey, PersistentDataType.INTEGER, evolutionStep);
            return;
        }

        if (evolution.isRequiredItemToNextStage()) {
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

        if (mechanic.hasProgressText()) {
            UUID uuid = UUID.fromString(data.get(progressTextKey, PersistentDataType.STRING));
            mechanic.cleanProgressText(uuid);
        }

        FurnitureMechanic nextMechanic = (FurnitureMechanic) furnitureFactory.getMechanic(evolution.getNextStage());
        nextMechanic.place(frame.getRotation(),rotation, frame.getFacing(), frame.getLocation(), null);
    }
}
