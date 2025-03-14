package qouteall.imm_ptl.core;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.q_misc_util.Helper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

@Environment(EnvType.CLIENT)
public class CHelper {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static int reportedErrorNum = 0;
    
    public static PlayerInfo getClientPlayerListEntry() {
        return Minecraft.getInstance().getConnection().getPlayerInfo(
            Minecraft.getInstance().player.getGameProfile().getId()
        );
    }
    
    public static Level getClientWorld(ResourceKey<Level> dimension) {
        return ClientWorldLoader.getWorld(dimension);
    }
    
    @Nullable
    public static List<Portal> getClientGlobalPortal(Level world) {
        if (world instanceof ClientLevel) {
            return ((IEClientWorld) world).getGlobalPortals();
        }
        else {
            return null;
        }
    }
    
    public static Stream<Portal> getClientNearbyPortals(double range) {
        return IPMcHelper.getNearbyPortals(Minecraft.getInstance().player, range);
    }
    
    public static void checkGlError() {
        if (!IPGlobal.doCheckGlError) {
            return;
        }
        if (reportedErrorNum > 100) {
            return;
        }
        doCheckGlError();
    }
    
    public static void doCheckGlError() {
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR) {
            Helper.err("OpenGL Error" + errorCode);
            new Throwable().printStackTrace();
            reportedErrorNum++;
        }
    }
    
    public static void printChat(String str) {
        Helper.log(str);
        printChat(Component.literal(str));
    }
    
    public static void printChat(Component text) {
        Minecraft.getInstance().gui.getChat().addMessage(text);
    }
    
    public static void openLinkConfirmScreen(
        Screen parent,
        String link
    ) {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new ConfirmLinkScreen(
            (result) -> {
                if (result) {
                    try {
                        Util.getPlatform().openUri(new URI(link));
                    }
                    catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
                client.setScreen(parent);
            },
            link, true
        ));
    }
    
    public static Vec3 getCurrentCameraPos() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }
    
    public static Iterable<Entity> getWorldEntityList(Level world) {
        if (!(world instanceof ClientLevel)) {
            return (Iterable<Entity>) Collections.emptyList().iterator();
        }
        
        ClientLevel clientWorld = (ClientLevel) world;
        return clientWorld.entitiesForRendering();
    }
    
    public static double getSmoothCycles(long unitTicks) {
        return (
            StableClientTimer.getStableTickTime() % unitTicks +
                StableClientTimer.getStablePartialTicks()
        ) / (double) unitTicks;
    }
    
    public static void disableDepthClamp() {
        if (IPGlobal.enableClippingMechanism) {
            GL11.glDisable(GL32.GL_DEPTH_CLAMP);
        }
    }
    
    public static void enableDepthClamp() {
        if (IPGlobal.enableClippingMechanism) {
            GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        }
    }
    
    /**
     * Get `modid/textures/dimension/dimension_id.png` first.
     * If missing, then try to get the mod icon.
     * If still missing, return null.
     */
    @Nullable
    public static ResourceLocation getDimensionIconPath(ResourceKey<Level> dimension) {
        ResourceLocation dimensionId = dimension.location();
        
        ResourceLocation dimIconPath = new ResourceLocation(
            dimensionId.getNamespace(),
            "textures/dimension/" + dimensionId.getPath() + ".png"
        );
        
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(dimIconPath);
        
        if (resource.isEmpty()) {
            LOGGER.info("Cannot load texture {}", dimIconPath);
            
            ResourceLocation modIconLocation = O_O.getModIconLocation(dimensionId.getNamespace());
            
            if (modIconLocation == null) {
                return null;
            }
            
            ResourceLocation modIconPath = new ResourceLocation(
                modIconLocation.getNamespace(),
                modIconLocation.getPath()
            );
            
            Optional<Resource> modIconResource = Minecraft.getInstance().getResourceManager().getResource(modIconPath);
            
            if (modIconResource.isEmpty()) {
                LOGGER.info("Cannot load texture {}", modIconPath);
                return null;
            }
            
            return modIconPath;
        }
        
        return dimIconPath;
    }
    
}
