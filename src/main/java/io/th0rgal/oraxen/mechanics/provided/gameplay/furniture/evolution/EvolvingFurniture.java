package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Random;

public class EvolvingFurniture {

    private final String currentStage;
    private final int delay;
    private final boolean lightBoost;
    private final String nextStage;
    private final int probability;
    private final boolean requiredItemToNextStage;

    private final Random random = new Random();
    private final List<Material> requiredItems = new java.util.ArrayList<>();

    public EvolvingFurniture(String itemID, ConfigurationSection plantSection) {
        currentStage = itemID;
        delay = plantSection.getInt("delay");
        lightBoost = plantSection.isBoolean("light_boost") && plantSection.getBoolean("light_boost");
        nextStage = plantSection.getString("next_stage");
        probability = (int) (1D / (double) plantSection.get("probability", 1));
        requiredItemToNextStage = plantSection.getBoolean("need_fertilizer", false);


        if (requiredItemToNextStage) {
            for (String requiredItem : plantSection.getStringList("fertilizer_materials")) {
                requiredItems.add(Material.valueOf(requiredItem));
            }
        }
    }

    public boolean isRequiredItemToNextStage() {
        return requiredItemToNextStage;
    }

    public List<Material> getRequiredItems() {
        return requiredItems;
    }

    public int getDelay() {
        return delay;
    }

    public boolean isLightBoosted() {
        return lightBoost;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public String getNextStage() {
        return nextStage;
    }

    public boolean bernoulliTest() {
        return random.nextInt(probability) == 0;
    }
}
