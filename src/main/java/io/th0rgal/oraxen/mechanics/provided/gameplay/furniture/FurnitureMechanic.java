package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import de.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolvingFurniture;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FurnitureMechanic extends Mechanic {

    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");
    public static final NamespacedKey ROOT_KEY = new NamespacedKey(OraxenPlugin.get(), "root");
    public static final NamespacedKey ORIENTATION_KEY = new NamespacedKey(OraxenPlugin.get(), "orientation");
    public static final NamespacedKey EVOLUTION_KEY = new NamespacedKey(OraxenPlugin.get(), "evolution");
    public static final NamespacedKey LOCK_EVOLUTION_KEY = new NamespacedKey(OraxenPlugin.get(), "lock_evolution");
    public static final NamespacedKey PROGESS_TEXT = new NamespacedKey(OraxenPlugin.get(), "progress_text");

    public final boolean farmlandRequired;
    private final List<BlockLocation> barriers;
    private final boolean hasRotation;
    private final boolean hasSeat;
    private final BlockFace facing;
    private final Drop drop;
    private final EvolvingFurniture evolvingFurniture;
    private final boolean resetFarmland;
    private final boolean isFarmToolRequired;
    private final int light;
    private String progressText;
    private String placedItemId;
    private ItemStack placedItem;
    private Rotation rotation;
    private float seatHeight;
    private float seatYaw;

    @SuppressWarnings("unchecked")
    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY,
                PersistentDataType.BYTE, (byte) 1));

        if (section.isString("item"))
            placedItemId = section.getString("item");

        barriers = new ArrayList<>();
        if (CompatibilitiesManager.hasPlugin("ProtocolLib") && section.getBoolean("barrier", false))
            barriers.add(new BlockLocation(0, 0, 0));
        if (CompatibilitiesManager.hasPlugin("ProtocolLib") && section.isList("barriers"))
            for (Object barrierObject : section.getList("barriers"))
                barriers.add(new BlockLocation((Map<String, Object>) barrierObject));

        if (section.isConfigurationSection("seat")) {
            ConfigurationSection seatSection = section.getConfigurationSection("seat");
            hasSeat = true;
            seatHeight = (float) seatSection.getDouble("height");
            seatYaw = (float) seatSection.getDouble("yaw");
        } else
            hasSeat = false;

        if (section.isConfigurationSection("evolution")) {
            evolvingFurniture = new EvolvingFurniture(getItemID(), section.getConfigurationSection("evolution"));
            ((FurnitureFactory) getFactory()).registerEvolution();
        } else evolvingFurniture = null;

        if (section.isString("rotation")) {
            rotation = Rotation.valueOf(section.getString("rotation", "NONE").toUpperCase());
            hasRotation = true;
        } else
            hasRotation = false;

        light = section.getInt("light", -1);

        farmlandRequired = section.getBoolean("farmland_required", false);
        resetFarmland = section.getBoolean("reset_farmland", false);
        isFarmToolRequired = section.getBoolean("harvest_tool", false);


        facing = section.isString("facing")
                ? BlockFace.valueOf(section.getString("facing").toUpperCase())
                : null;

        List<Loot> loots = new ArrayList<>();
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>)
                    drop.getList("loots"))
                loots.add(new Loot(lootConfig));

            if (drop.isString("minimal_type")) {
                FurnitureFactory mechanic = (FurnitureFactory) mechanicFactory;
                List<String> bestTools = drop.isList("best_tools")
                        ? drop.getStringList("best_tools")
                        : new ArrayList<>();
                this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                        drop.getBoolean("fortune"), getItemID(),
                        drop.getString("minimal_type"),
                        bestTools);
            } else
                this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"),
                        getItemID());
        } else
            drop = new Drop(loots, false, false, getItemID());

        if (section.isString("evolution.progress")) {
            progressText = section.getString("evolution.progress");
        }
    }

    public static ItemFrame getItemFrame(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1))
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == location.getBlockX()
                    && entity.getLocation().getBlockY() == location.getBlockY()
                    && entity.getLocation().getBlockZ() == location.getBlockZ()
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING))
                return frame;
        return null;
    }

    public boolean hasBarriers() {
        return !barriers.isEmpty();
    }

    public boolean isFarmToolRequired() {
        return isFarmToolRequired;
    }

    public boolean ableToResetFarmland() {
        return resetFarmland;
    }

    public List<BlockLocation> getBarriers() {
        return barriers;
    }

    public boolean hasRotation() {
        return hasRotation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean hasSeat() {
        return hasSeat;
    }

    public float getSeatHeight() {
        return seatHeight;
    }

    public float getSeatYaw() {
        return seatYaw;
    }

    public boolean hasFacing() {
        return facing != null;
    }

    public BlockFace getFacing() {
        return facing;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean hasEvolution() {
        return evolvingFurniture != null;
    }

    public EvolvingFurniture getEvolution() {
        return evolvingFurniture;
    }

    public boolean hasProgressText() {
        return progressText != null;
    }

    public UUID placeProgressText(Location location) {
        ArmorStand text = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

        text.setGravity(false);
        text.setVisible(false);
        text.setCustomNameVisible(true);

        return text.getUniqueId();
    }

    public void updateProgressText(UUID entityId, int delay, int maxDelay) {
        ArmorStand text = (ArmorStand) Bukkit.getEntity(entityId);

        text.setCustomName(convertProgressText(delay, maxDelay));
    }

    public void cleanProgressText(UUID entityId) {
        Bukkit.getEntity(entityId).remove();
    }

    private String convertProgressText(int delay, int maxDelay) {
        int percent = (int) (((double) delay / (double) maxDelay) * 100);

        // round to nearest 10
        int percentNearest10 = (int) Math.round(percent / 10.0) * 10;

        Pattern pattern = Pattern.compile("%percent_replace_10:([^%]*)%");

        Matcher matcher = pattern.matcher(progressText);

        if (matcher.matches()) {
            String replacement = matcher.group(0);
            String value = matcher.group(1);
            progressText = progressText.replace(replacement, value.charAt(percentNearest10 / 10) + "");
        }

        return progressText.replace("%percent%", percent + "")
                .replace("%delay%", delay + "")
                .replace("%max_delay%", maxDelay + "");
    }

    private void setPlacedItem() {
        if (placedItem == null) {
            placedItem = OraxenItems.getItemById(placedItemId != null ? placedItemId : getItemID()).build();
            ItemMeta meta = placedItem.getItemMeta();
            meta.setDisplayName("");
            placedItem.setItemMeta(meta);
        }
    }

    public boolean place(Rotation rotation, float yaw, BlockFace facing, Location location, String entityId) {
        setPlacedItem();
        return place(rotation, yaw, facing, location, entityId, placedItem);
    }

    public boolean place(Rotation rotation, float yaw, BlockFace facing, Location location, String entityId,
                         ItemStack item) {
        if (hasBarriers())
            for (Location sideLocation : getLocations(yaw, location, getBarriers())) {
                if (!sideLocation.getBlock().getType().isAir())
                    return false;
            }

        setPlacedItem();
        location.getWorld().spawn(location, ItemFrame.class, (ItemFrame frame) -> {
            frame.setVisible(false);
            frame.setFixed(false);
            frame.setPersistent(true);
            frame.setItemDropChance(0);
            if (evolvingFurniture == null) {
                ItemStack clone = item.clone();
                ItemMeta meta = clone.getItemMeta();
                meta.setDisplayName("");
                clone.setItemMeta(meta);
                frame.setItem(clone);
            } else
                frame.setItem(placedItem);

            frame.setRotation(rotation);

            frame.setFacingDirection(hasFacing() ? getFacing() : facing, true);
            frame.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
            if (hasSeat())
                frame.getPersistentDataContainer().set(SEAT_KEY, PersistentDataType.STRING, entityId);
            if (hasEvolution())
                frame.getPersistentDataContainer().set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        });

        if (hasBarriers())
            for (Location sideLocation : getLocations(yaw, location, getBarriers())) {
                Block block = sideLocation.getBlock();
                PersistentDataContainer data = new CustomBlockData(block, OraxenPlugin.get());
                data.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
                if (hasSeat())
                    data.set(SEAT_KEY, PersistentDataType.STRING, entityId);
                data.set(ROOT_KEY, PersistentDataType.STRING, new BlockLocation(location).toString());
                data.set(ORIENTATION_KEY, PersistentDataType.FLOAT, yaw);
                block.setType(Material.BARRIER, false);
                if (light != -1)
                    WrappedLightAPI.createBlockLight(sideLocation, light, false);
            }
        else if (light != -1)
            WrappedLightAPI.createBlockLight(location, light, false);

        if (light != -1)
            WrappedLightAPI.refreshBlockLights(light, location);

        return true;
    }

    public boolean removeSolid(World world, BlockLocation rootBlockLocation, float orientation) {
        Location rootLocation = rootBlockLocation.toLocation(world);

        for (Location location : getLocations(orientation,
                rootLocation,
                getBarriers())) {
            if (light != -1)
                WrappedLightAPI.removeBlockLight(location, false);
            location.getBlock().setType(Material.AIR);
        }

        boolean removed = false;
        for (Entity entity : rootLocation.getWorld().getNearbyEntities(rootLocation, 1, 1, 1))
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == rootLocation.getX()
                    && entity.getLocation().getBlockY() == rootLocation.getY()
                    && entity.getLocation().getBlockZ() == rootLocation.getZ()
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING)) {
                if (entity.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                    Entity stand = Bukkit.getEntity(UUID.fromString(entity.getPersistentDataContainer()
                            .get(SEAT_KEY, PersistentDataType.STRING)));
                    for (Entity passenger : stand.getPassengers())
                        stand.removePassenger(passenger);
                    stand.remove();
                }
                frame.remove();
                if (light != -1)
                    WrappedLightAPI.removeBlockLight(rootLocation, false);
                rootLocation.getBlock().setType(Material.AIR);
                removed = true;
                break;
            }

        if (light != -1)
            WrappedLightAPI.refreshBlockLights(light, rootLocation);
        return removed;
    }

    public void removeAirFurniture(ItemFrame frame) {
        if (frame.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
            Entity stand = Bukkit.getEntity(UUID.fromString(frame.getPersistentDataContainer()
                    .get(SEAT_KEY, PersistentDataType.STRING)));
            for (Entity passenger : stand.getPassengers())
                stand.removePassenger(passenger);
            stand.remove();
        }
        Location location = frame.getLocation().getBlock().getLocation();
        if (light != -1) {
            WrappedLightAPI.removeBlockLight(location, false);
            WrappedLightAPI.refreshBlockLights(light, location);
        }
        frame.remove();
    }

    public List<Location> getLocations(float rotation, Location center, List<BlockLocation> relativeCoordinates) {
        List<Location> output = new ArrayList<>();
        for (BlockLocation modifier : relativeCoordinates)
            output.add(modifier.groundRotate(rotation).add(center));
        return output;
    }

    public float getYaw(Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }
}
