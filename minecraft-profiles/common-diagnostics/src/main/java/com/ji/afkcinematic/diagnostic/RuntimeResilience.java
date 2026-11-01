package com.ji.afkcinematic.diagnostic;

import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.session.CinematicSession;
import com.ji.afkcinematic.session.PresentationPolicy;
import com.ji.afkcinematic.platform.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import java.time.Duration;
import java.util.List;

/** Native facts at the boundary; decisions/evidence remain bounded and platform-independent. */
public final class RuntimeResilience {
    private static final IncidentLedger INCIDENTS=new IncidentLedger();
    private static final CameraOutputHealth CAMERA=new CameraOutputHealth();
    private static SpatialGuiBridge spatial;
    private static IncidentRepository repository;
    private static long generation,savedRevision;
    private static final java.util.concurrent.atomic.AtomicLong LAYOUT=new java.util.concurrent.atomic.AtomicLong();
    private static long pendingActivation;
    private static int writeDelay;
    private static boolean loaded,held,initialized;
    private static CameraOutputHealth.Fault pending=CameraOutputHealth.Fault.NONE;
    private RuntimeResilience(){ }
    public static void init() {
        if(initialized)return;initialized=true;spatial=new SpatialGuiBridge();
        repository=new IncidentRepository(DiagnosticPlatform.configDir().resolve("ji-afk-cinematic-incidents.json"));
        repository.read().whenComplete((saved,error)->Minecraft.getInstance().execute(()->{
            boolean dirty=INCIDENTS.revision()!=0;
            if(error==null)INCIDENTS.restore(saved);
            else JiAFKCinematic.LOGGER.warn("Could not read the local cinematic incident journal; existing file preserved",error);
            loaded=true;savedRevision=dirty?-1:INCIDENTS.revision();
        }));
        ClientEvents.START_CLIENT_TICK.register(client->{
            generation++;AFKDetector.synchronizeContext();
            var reason=obstruction();boolean blocked=reason!=CinematicSession.Reason.NONE;
            if(blocked)CinematicManager.onPresentationOpened(reason);
            if(blocked || held)com.ji.afkcinematic.input.RuntimeLookInput.rebase();
            held=blocked;
            if(pending!=CameraOutputHealth.Fault.NONE) {
                var fault=pending;pending=CameraOutputHealth.Fault.NONE;
                if(AFKDetector.session().active() && AFKDetector.session().activationId()==pendingActivation && !blocked) {
                    record(switch(fault){case NOT_APPLIED->DiagnosticIncident.Code.CAMERA_NOT_APPLIED;
                        case OVERWRITTEN->DiagnosticIncident.Code.CAMERA_OVERWRITTEN;
                        default->DiagnosticIncident.Code.CAMERA_CONSUMER_MISSING;});
                    CinematicManager.failForCompatibility();
                }
            }
            if(loaded && INCIDENTS.revision()!=savedRevision && ++writeDelay>=20)flush();
        });
        ClientEvents.CLIENT_STOPPING.register(client->{if(loaded)flush();repository.shutdown(Duration.ofSeconds(2));});
    }
    public static PresentationPolicy.Screen classify(Screen screen) {
        if(screen==null)return PresentationPolicy.Screen.GAMEPLAY;
        if(screen instanceof InBedChatScreen)return PresentationPolicy.Screen.SLEEP;
        if(screen instanceof ChatScreen)return PresentationPolicy.Screen.CHAT;
        if(screen instanceof com.ji.afkcinematic.config.MusicManagementScreen
                || screen instanceof com.ji.afkcinematic.config.ConfigScreen
                || screen instanceof com.ji.afkcinematic.config.FolderPathScreen
                || com.ji.afkcinematic.config.SupportScreen.isSupportScreen(screen))return PresentationPolicy.Screen.OWN_SETTINGS;
        return PresentationPolicy.Screen.INTERFACE;
    }
    public static CinematicSession.Reason obstruction() {
        var screen=ScreenHelper.getCurrentScreen(Minecraft.getInstance());
        return PresentationPolicy.obstruction(classify(screen),spatial!=null && spatial.busy(generation,screen));
    }
    public static void beforeScreen(Screen screen) {
        // The existing pause adapter consumes the first Escape/direct pause request.
        if(screen instanceof net.minecraft.client.gui.screens.PauseScreen)return;
        if(PresentationPolicy.obstruction(classify(screen),false)==CinematicSession.Reason.SCREEN_OPEN)
            CinematicManager.onPresentationOpened(CinematicSession.Reason.SCREEN_OPEN);
    }
    public static void beginRender() {
        var client=Minecraft.getInstance();
        CAMERA.begin(AFKDetector.session().activationId(),AFKDetector.session().active() && client.level!=null
                && client.player!=null && client.isWindowActive() && !client.isPaused() && !ScreenHelper.hasOverlay(client)
                && obstruction()==CinematicSession.Reason.NONE);
    }
    public static void applied(double x,double y,double z,double qx,double qy,double qz,double qw) {
        CAMERA.applied(AFKDetector.session().activationId(),x,y,z,qx,qy,qz,qw);
    }
    public static void consumed(double x,double y,double z,double qx,double qy,double qz,double qw) {
        HookCapabilities.observed(DiagnosticReport.Hook.CAMERA_CONSUMER);
        var fault=CAMERA.consume(AFKDetector.session().activationId(),x,y,z,qx,qy,qz,qw);
        if(fault!=CameraOutputHealth.Fault.NONE){pending=fault;pendingActivation=AFKDetector.session().activationId();}
    }
    public static void endRender() {var fault=CAMERA.end();if(fault!=CameraOutputHealth.Fault.NONE){pending=fault;pendingActivation=AFKDetector.session().activationId();}}
    public static void record(DiagnosticIncident.Code code) {
        // Presence is a troubleshooting candidate, never confirmed attribution.
        var candidates=code==DiagnosticIncident.Code.CAMERA_OVERWRITTEN && spatial!=null && spatial.present()?List.of("spatial_gui"):List.<String>of();
        int before=INCIDENTS.pending();
        INCIDENTS.record(code,candidates.isEmpty()?DiagnosticIncident.Confidence.UNKNOWN:DiagnosticIncident.Confidence.POSSIBLE,
                candidates,System.currentTimeMillis(),AFKDetector.session().traceSnapshot(),BuildIdentity.id());
        if(INCIDENTS.pending()>before)JiAFKCinematic.LOGGER.warn("Cinematic diagnostic pending: {} (attribution: {}; candidate providers: {})",code,
                candidates.isEmpty()?"unknown":"possible",candidates);
    }
    public static List<DiagnosticIncident> incidents(){return INCIDENTS.snapshot();}
    public static int pendingCount(){return INCIDENTS.pending();}
    public static boolean spatialPresent(){return spatial!=null && spatial.present();}
    public static void reviewed(List<DiagnosticIncident> reviewed){INCIDENTS.review(reviewed);}
    public static void invalidateLayout(){LAYOUT.incrementAndGet();}
    public static long layoutGeneration(){return LAYOUT.get();}
    private static void flush() {
        long revision=INCIDENTS.revision();var snapshot=INCIDENTS.snapshot();writeDelay=0;savedRevision=revision;
        repository.write(snapshot).whenComplete((ignored,error)->{
            if(error!=null)Minecraft.getInstance().execute(()->{savedRevision=-1;JiAFKCinematic.LOGGER.warn("Could not persist cinematic incident journal",error);});
        });
    }
}
