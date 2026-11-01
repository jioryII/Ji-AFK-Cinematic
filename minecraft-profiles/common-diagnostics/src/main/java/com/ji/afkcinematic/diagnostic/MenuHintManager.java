package com.ji.afkcinematic.diagnostic;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.input.MenuHintPolicy;
import com.ji.afkcinematic.input.PortableKeyBinding;
import com.ji.afkcinematic.platform.ClientEvents;
import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.render.ToggleToastManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

/** First-world hint; asynchronous marker IO, stale world results ignored, no gameplay input. */
public final class MenuHintManager {
    private static WorldHintRepository repository;
    private static Object context;
    private static WorldHintRepository.Entry entry;
    private static int readyTicks;
    private static boolean handled;
    private static int shownFirst,shownSecond;
    private MenuHintManager() {}
    public static void init() {
        if(repository!=null)return;
        repository=new WorldHintRepository(DiagnosticPlatform.configDir());
        ClientEvents.END_CLIENT_TICK.register(MenuHintManager::tick);
    }
    private static void tick(Minecraft client) {
        var cfg=ConfigManager.getConfig();
        String first=cfg.menuKey1<0?null:cfg.menuKey1Name,second=cfg.menuKey2<0?null:cfg.menuKey2Name;
        boolean canPresent=MenuHintPolicy.eligible(cfg.modEnabled,first,second) && ScreenHelper.getCurrentScreen(client)==null
                && !ScreenHelper.hasOverlay(client) && !AFKDetector.session().active();
        if(!canPresent)ToggleToastManager.hideMenuHint();
        else if(handled && (shownFirst!=cfg.menuKey1 || shownSecond!=cfg.menuKey2)) {
            ToggleToastManager.refreshMenuHint(shortcut(cfg));shownFirst=cfg.menuKey1;shownSecond=cfg.menuKey2;
        }
        Object current=client.player==null||client.level==null?null:client.getSingleplayerServer()!=null?client.getSingleplayerServer():client.getConnection();
        if(current!=context) {
            context=current;entry=null;handled=false;readyTicks=0;
            if(current!=null) {
                var local=client.getSingleplayerServer();var server=client.getCurrentServer();
                var path=local==null?null:local.getWorldPath(LevelResource.ROOT);
                String remote=server==null?null:"server:"+server.ip.trim().toLowerCase(java.util.Locale.ROOT);
                repository.lookup(path,remote).whenComplete((result,error)->client.execute(()->{
                    if(context!=current)return;
                    if(error!=null){handled=true;JiAFKCinematic.LOGGER.warn("Could not read first-world menu hint state",error);}
                    else{entry=result;handled=result.seen();}
                }));
            } else ToggleToastManager.hideMenuHint();
        }
        if(current==null||handled||entry==null)return;
        if(ConfigManager.isLoading()||!MenuHintPolicy.eligible(cfg.modEnabled,first,second)||!client.isWindowActive()||client.isPaused()
                ||ScreenHelper.getCurrentScreen(client)!=null||ScreenHelper.hasOverlay(client)||AFKDetector.session().active()) {readyTicks=0;return;}
        if(++readyTicks<60)return;
        // Resolve native codes at display time; names are never hardcoded to F7 + H.
        first=PortableKeyBinding.nameOf(cfg.menuKey1);second=PortableKeyBinding.nameOf(cfg.menuKey2);
        if(!MenuHintPolicy.eligible(cfg.modEnabled,first,second)){readyTicks=0;return;}
        if(ToggleToastManager.showMenuHint(shortcut(cfg))) {
            shownFirst=cfg.menuKey1;shownSecond=cfg.menuKey2;
            handled=true;repository.mark(entry).exceptionally(error->{JiAFKCinematic.LOGGER.warn("Could not persist first-world menu hint state",error);return null;});
        }
    }
    private static Component shortcut(com.ji.afkcinematic.config.ModConfig cfg) {
        var first=com.mojang.blaze3d.platform.InputConstants.getKey(PortableKeyBinding.nameOf(cfg.menuKey1)).getDisplayName();
        if(cfg.menuKey1==cfg.menuKey2)return first;
        return first.copy().append(" + ").append(com.mojang.blaze3d.platform.InputConstants.getKey(PortableKeyBinding.nameOf(cfg.menuKey2)).getDisplayName());
    }
}
