package qouteall.imm_ptl.core;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;
import qouteall.imm_ptl.core.chunk_loading.ChunkDataSyncManager;
import qouteall.imm_ptl.core.chunk_loading.EntitySync;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTickets;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.chunk_loading.ServerPerformanceMonitor;
import qouteall.imm_ptl.core.chunk_loading.WorldInfoSender;
import qouteall.imm_ptl.core.collision.CollisionHelper;
import qouteall.imm_ptl.core.commands.AxisArgumentType;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.commands.SubCommandArgumentType;
import qouteall.imm_ptl.core.commands.TimingFunctionArgumentType;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.debug.DebugUtil;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.network.IPNetworking;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.BreakableMirror;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.animation.NormalAnimation;
import qouteall.imm_ptl.core.portal.animation.RotationAnimation;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.GlobalTrackedPortal;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.imm_ptl.core.portal.global_portals.WorldWrappingPortal;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalEntity;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.q_misc_util.Helper;

import java.io.File;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class IPModMain {
    
    public static void init() {
        loadConfig();
        
        Helper.log("Immersive Portals Mod Initializing");
        
        IPNetworking.init();
        
        IPGlobal.postClientTickSignal.connect(IPGlobal.clientTaskList::processTasks);
        IPGlobal.postServerTickSignal.connect(IPGlobal.serverTaskList::processTasks);
        IPGlobal.preGameRenderSignal.connect(IPGlobal.preGameRenderTaskList::processTasks);
        
        IPGlobal.clientCleanupSignal.connect(() -> {
            if (ClientWorldLoader.getIsInitialized()) {
                IPGlobal.clientTaskList.forceClearTasks();
            }
        });
        IPGlobal.serverCleanupSignal.connect(IPGlobal.serverTaskList::forceClearTasks);
        
        IPGlobal.serverTeleportationManager = new ServerTeleportationManager();
        IPGlobal.chunkDataSyncManager = new ChunkDataSyncManager();
        
        NewChunkTrackingGraph.init();
        
        WorldInfoSender.init();
        
        GlobalPortalStorage.init();
        
        EntitySync.init();
        
        CollisionHelper.init();
        
        PortalExtension.init();
        
        GcMonitor.initCommon();
        
        ServerPerformanceMonitor.init();
        
        ImmPtlChunkTickets.init();
        
        IPPortingLibCompat.init();
        
        BlockManipulationServer.init();
        
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> PortalCommand.register(dispatcher)
        );
        SubCommandArgumentType.init();
        TimingFunctionArgumentType.init();
        AxisArgumentType.init();
    
        DebugUtil.init();
        
        // intrinsic animation driver types
        RotationAnimation.init();
        NormalAnimation.init();
    }
    
    private static void loadConfig() {
        // upgrade old config
        Path gameDir = O_O.getGameDir();
        File oldConfigFile = gameDir.resolve("config").resolve("immersive_portals_fabric.json").toFile();
        if (oldConfigFile.exists()) {
            File dest = gameDir.resolve("config").resolve("immersive_portals.json").toFile();
            boolean succeeded = oldConfigFile.renameTo(dest);
            if (succeeded) {
                Helper.log("Upgraded old config file");
            }
            else {
                Helper.err("Failed to upgrade old config file");
            }
        }
        
        Helper.log("Loading Immersive Portals config");
        IPGlobal.configHolder = AutoConfig.register(IPConfig.class, GsonConfigSerializer::new);
        IPGlobal.configHolder.registerSaveListener((configHolder, ipConfig) -> {
            ipConfig.onConfigChanged();
            return InteractionResult.SUCCESS;
        });
        IPConfig ipConfig = IPConfig.getConfig();
        ipConfig.onConfigChanged();
    }
    
    public static void registerBlocks(BiConsumer<ResourceLocation, PortalPlaceholderBlock> regFunc) {
        regFunc.accept(
            new ResourceLocation("immersive_portals", "nether_portal_block"),
            PortalPlaceholderBlock.instance
        );
    }
    
    public static void registerEntityTypes(BiConsumer<ResourceLocation, EntityType<?>> regFunc) {
    
        regFunc.accept(
            new ResourceLocation("immersive_portals", "portal"),
            Portal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "nether_portal_new"),
            NetherPortalEntity.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "end_portal"),
            EndPortalEntity.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "mirror"),
            Mirror.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "breakable_mirror"),
            BreakableMirror.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "global_tracked_portal"),
            GlobalTrackedPortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "border_portal"),
            WorldWrappingPortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "end_floor_portal"),
            VerticalConnectingPortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "general_breakable_portal"),
            GeneralBreakablePortal.entityType
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals", "loading_indicator"),
            LoadingIndicatorEntity.entityType
        );
    }
}
