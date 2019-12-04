package its_meow.claimitdynmap;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import com.mojang.authlib.GameProfile;

import its_meow.claimit.api.claim.ClaimArea;
import its_meow.claimit.api.claim.ClaimManager;
import its_meow.claimit.api.event.claim.ClaimAddedEvent;
import its_meow.claimit.api.event.claim.ClaimRemovedEvent;
import its_meow.claimit.api.event.claim.ClaimsClearedEvent;
import its_meow.claimit.api.userconfig.UserConfigType;
import its_meow.claimit.api.userconfig.UserConfigType.UserConfig;
import its_meow.claimit.api.userconfig.UserConfigTypeRegistry;
import its_meow.claimit.api.util.nbt.NBTDeserializer;
import its_meow.claimit.api.util.nbt.NBTSerializer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ClaimItDynmap.MODID)
@Mod(modid = ClaimItDynmap.MODID, name = ClaimItDynmap.NAME, version = ClaimItDynmap.VERSION, acceptableRemoteVersions = "*", dependencies = "after-required:claimitapi;after-required:claimit;after-required:dynmap")
public class ClaimItDynmap extends DynmapCommonAPIListener {

    public static final String MODID = "claimitdynmap";
    public static final String NAME = "ClaimIt-Dynmap";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private static DynmapCommonAPI dynmapAPI = null;
    private static MarkerAPI markerAPI = null;
    private static MarkerSet markerSet = null;

    public static final UserConfigTypeColor CONFIG_TYPE_COLOR = new UserConfigTypeColor();
    static {
        UserConfigTypeRegistry.addType(UserConfigTypeColor.class, CONFIG_TYPE_COLOR);
    }
    public static final UserConfig<String> CONFIG_FILL_COLOR = new UserConfig<String>("fill_color", "0x545454", "Color to fill your claims with on dynmap (if changed applies after server restart)");
    public static final UserConfig<String> CONFIG_BORDER_COLOR = new UserConfig<String>("border_color", "0x545454", "Color to outline your claims with on dynmap (if changed applies after server restart)");

    private static boolean dynmapLoaded = false;
    private static final Set<ClaimArea> queuedClaimEvents = new HashSet<ClaimArea>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CONFIG_TYPE_COLOR.addConfig(CONFIG_FILL_COLOR);
        CONFIG_TYPE_COLOR.addConfig(CONFIG_BORDER_COLOR);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        DynmapCommonAPIListener.register(this);
    }

    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            @Override
            public String getName() {
                return "claimitdynmapreload";
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return "/claimitdynmapreload";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                for(ClaimArea claim : ClaimManager.getManager().getClaimsList()) {
                    removeMarker(claim);
                    addMarker(claim);
                }
                sender.sendMessage(new TextComponentString("Reloaded ClaimIt dynmap markers!"));
            }

            @Override
            public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
                return sender.canUseCommand(4, "claimitdynmap.command.claimitdynmapreload");
            }
        });
    }

    @Override
    public void apiEnabled(DynmapCommonAPI api) {
        dynmapLoaded = true;
        if(api != null) {
            dynmapAPI = api;
            markerAPI = dynmapAPI.getMarkerAPI();
            markerSet = markerAPI.getMarkerSet("claimit.claims.markerset");
            if(markerSet == null) {
                markerSet = markerAPI.createMarkerSet("claimit.claims.markerset", "ClaimIt", null, false);
            } else {
                markerSet.setMarkerSetLabel("ClaimIt");
            }
            queuedClaimEvents.forEach(ClaimItDynmap::addMarker);
            queuedClaimEvents.clear();
        }
    }

    @SubscribeEvent
    public static void claimAdded(ClaimAddedEvent e) {
        addMarker(e.getClaim());
    }

    @SubscribeEvent
    public static void claimRemoved(ClaimRemovedEvent e) {
        removeMarker(e.getClaim());
    }

    @SubscribeEvent
    public static void claimsCleared(ClaimsClearedEvent.Pre e) {
        for(ClaimArea claim : ClaimManager.getManager().getClaimsList()) {
            removeMarker(claim);
        }
    }

    public static void removeMarker(ClaimArea claim) {
        if(markerSet != null) {
            AreaMarker marker = markerSet.findAreaMarker("CLAIMIT_" + String.valueOf(claim.hashCode()));
            if(marker != null) {
                marker.deleteMarker();
            } else {
                ClaimItDynmap.LOGGER.error("Failed to find marker for Dynmap removal for claim " + claim.getTrueViewName());
            }
        } else {
            ClaimItDynmap.LOGGER.error("Failed to load Dynmap marker set.");
        }
    }

    public static void addMarker(ClaimArea claim) {
        if(markerSet != null) {
            String worldName = claim.getWorld().getWorldInfo().getWorldName();
            String markerID = "CLAIMIT_" + String.valueOf(claim.hashCode());

            double[] xList = new double[] { claim.getMainPosition().getX(), claim.getLXHZPosition().getX(), claim.getHXZPosition().getX(), claim.getHXLZPosition().getX() };
            double[] zList = new double[] { claim.getMainPosition().getZ(), claim.getLXHZPosition().getZ(), claim.getHXZPosition().getZ(), claim.getHXLZPosition().getZ() };
            String tooltip = "<strong>Claim Name: </strong>" + claim.getDisplayedViewName() +
            "<br><strong>Area: </strong>" + (claim.getSideLengthX() + 1) + "x" + (claim.getSideLengthZ() + 1) + " (" + claim.getArea() + ")" +
            "<br><strong>Owner: </strong>" + getNameForUUID(claim.getOwner(), claim.getWorld().getMinecraftServer()) + " (" + claim.getOwner() + ")";

            // Create the area marker for the claim
            AreaMarker marker = markerSet.createAreaMarker(markerID, tooltip, true /* tooltip is HTML */, worldName, xList, zList, false /* non-persistent marker */);

            // Configure the marker style
            if(marker != null) {
                int border_color = 0x545454;
                int fill_color = 0x545454;
                String border_color_o = CONFIG_TYPE_COLOR.storage.getValueFor(CONFIG_BORDER_COLOR, claim.getOwner());
                if(border_color_o != null) {
                    border_color = getColorInt(border_color_o);
                }
                String fill_color_o = CONFIG_TYPE_COLOR.storage.getValueFor(CONFIG_FILL_COLOR, claim.getOwner());
                if(fill_color_o != null) {
                    fill_color = getColorInt(fill_color_o);
                }
                marker.setLineStyle(3, 0.8, border_color);
                marker.setFillStyle(0.35, fill_color);
            } else {
                ClaimItDynmap.LOGGER.error("Failed to create Dynmap marker for claim " + claim.getTrueViewName());
            }
        } else {
            // Queue loaded claims while API is not initialized
            if(dynmapLoaded) {
                ClaimItDynmap.LOGGER.error("Failed to load Dynmap marker set.");
            } else {
                queuedClaimEvents.add(claim);
            }
        }
    }

    private static Integer getColorInt(String in) {
        return Integer.parseInt(in.substring(2), 16);
    }

    @Nonnull
    private static String getNameForUUID(UUID uuid, MinecraftServer server) {
        String name = null;
        GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(uuid);
        if(profile != null) {
            name = profile.getName();
        } else {
            name = uuid.toString();
        }
        return name;
    }

    public static class UserConfigTypeColor extends UserConfigType<String> {

        public UserConfigTypeColor() {
            super(String.class);
        }

        @Override
        protected NBTSerializer<String> getSerializer() {
            return (c, s, v) -> c.setString(s, (String) v);
        }

        @Override
        protected NBTDeserializer<String> getDeserializer() {
            return (c, s) -> c.getString(s);
        }

        @Override
        public boolean isValidValue(String in) {
            if(in.matches("0x[0-9a-fA-F]{6}")) {
                try {
                    Integer.parseInt(in.substring(2), 16);
                } catch(NumberFormatException e) {
                    return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public String fromString(String valueStr) {
            return valueStr;
        }

    }

}