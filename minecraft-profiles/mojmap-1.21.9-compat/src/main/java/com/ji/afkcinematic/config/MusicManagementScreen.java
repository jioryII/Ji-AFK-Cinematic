package com.ji.afkcinematic.config;
import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.music.LocalMusicPackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.nio.file.Path;
import java.util.*;

/** Edits the parent's draft. Only the main options screen commits configuration. */
public final class MusicManagementScreen extends Screen {
    private final Screen parent;
    private final ModConfig draft;
    private final com.ji.afkcinematic.client.UiWorkScope work=new com.ji.afkcinematic.client.UiWorkScope();
    private final List<Button> controls=new ArrayList<>();
    private final Map<Button,Integer> positions=new IdentityHashMap<>();
    private List<FormattedCharSequence> pathLines=List.of(),statusLines=List.of(),noticeLines=List.of();
    private Component status=Component.empty();
    private Button pathButton,exportButton;
    private Path lastExport;
    private boolean busy;
    private int left,panelWidth,contentTop,contentBottom,buttonTop,statusY,scroll,maxScroll;
    public MusicManagementScreen(Screen parent,ModConfig draft) {
        super(text("music_manager_title"));this.parent=parent;this.draft=draft;
    }
    private static net.minecraft.network.chat.MutableComponent text(String key,Object...args) {return Component.translatable("config.ji_afkcinematic."+key,args);}
    @Override protected void init() {
        controls.clear();positions.clear();
        int columns=width<320?1:2;panelWidth=Math.min(columns==2?280:220,width-40);left=(width-panelWidth)/2;
        contentTop=46;contentBottom=height-64;buttonTop=contentTop+42;
        add("open_music_folder",()->LocalMusicPackManager.openMusicFolder(draft.customMusicDirectory));
        add("change_music_folder",this::chooseFolder);
        pathButton=add("folder_path",()->ScreenHelper.setScreen(minecraft,new FolderPathScreen(this,draft.customMusicDirectory,value->{draft.customMusicDirectory=value;lastExport=null;status=Component.empty();})));
        add("refresh_music",this::refresh);
        exportButton=add("export_music_pack",this::export);
        add("music_view_exports",LocalMusicPackManager::openMusicPackExports);
        exportButton.setTooltip(Tooltip.create(text("tooltip.music_export_reuse")));
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width/2-70,height-28,140,20).build());
        statusY=buttonTop+((controls.size()+columns-1)/columns)*26+8;
        noticeLines=font.split(text("music_draft_notice"),panelWidth);
        updatePath();setStatus(status);sync();
    }
    private Button add(String key,Runnable action) {
        int columns=width<320?1:2,index=controls.size(),w=(panelWidth-(columns-1)*10)/columns;
        var button=addRenderableWidget(Button.builder(text(key),b->action.run()).bounds(left+index%columns*(w+10),buttonTop+index/columns*26,w,20).build());
        controls.add(button);positions.put(button,button.getY());return button;
    }
    private boolean accepts(long generation) {return work.accepts(generation)&&ScreenHelper.getCurrentScreen(Minecraft.getInstance())==this;}
    private void updatePath() {
        String path=LocalMusicPackManager.getMusicFolder(draft.customMusicDirectory).toString();
        var lines=font.split(Component.literal(path),panelWidth);pathLines=List.copyOf(lines.subList(0,Math.min(2,lines.size())));
        if(pathButton!=null)pathButton.setTooltip(Tooltip.create(Component.literal(path)));
    }
    private void setStatus(Component message) {
        status=message;statusLines=font.split(message,panelWidth);
        maxScroll=Math.max(0,statusY+Math.max(1,statusLines.size())*10-contentBottom);
        scroll=Math.max(0,Math.min(scroll,maxScroll));sync();
    }
    private void chooseFolder() {
        long generation=work.generation();busy=true;sync();
        LocalMusicPackManager.chooseMusicFolderAsync(draft.customMusicDirectory,()->accepts(generation),selected->{
            if(!accepts(generation))return;busy=false;
            if(selected!=null){draft.customMusicDirectory=selected;lastExport=null;updatePath();setStatus(text("music_draft_notice"));}
            sync();
        });
    }
    private void refresh() {
        if(!Objects.equals(draft.customMusicDirectory,ConfigManager.getConfig().customMusicDirectory)) {setStatus(text("music_save_folder"));return;}
        long generation=work.generation();busy=true;lastExport=null;sync();
        LocalMusicPackManager.rebuildAndReloadAsync(()->accepts(generation),count->{
            if(!accepts(generation))return;busy=false;
            setStatus(count<0?text("refresh_music_failed"):text("music_loaded",count));sync();
        });
    }
    private void export() {
        if(busy||lastExport!=null||LocalMusicPackManager.isExportingMusicPack())return;
        long generation=work.generation();busy=true;setStatus(text("music_export_preparing"));sync();
        LocalMusicPackManager.exportMusicPackAsync(draft.customMusicDirectory,()->accepts(generation),result->{
            if(!accepts(generation))return;busy=false;lastExport=result;
            setStatus(text(result==null?"music_export_failed_preserved":"music_pack_exported"));
            if(result!=null)exportButton.setTooltip(Tooltip.create(Component.literal(result.toString()).append("\n").append(text("tooltip.music_export_reuse"))));
            sync();
        });
    }
    private void sync() {
        for(Button button:controls) {
            int y=positions.get(button)-scroll;button.setY(y);button.visible=y>=contentTop&&y+button.getHeight()<=contentBottom;
            button.active=button.visible&&!busy&&(button!=exportButton||lastExport==null&&!LocalMusicPackManager.isExportingMusicPack());
        }
    }
    @Override public void tick() {super.tick();sync();}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(maxScroll>0){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)Math.round(vertical*22)));sync();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta) {
        super.render(g,x,y,delta);g.drawCenteredString(font,title,width/2,20,0xFFE5D4F1);
        g.enableScissor(left,contentTop,left+panelWidth,contentBottom);
        g.drawString(font,text("music_directory_label"),left,contentTop-scroll,0xFFCCCCCC);
        for(int i=0;i<pathLines.size();i++)g.drawString(font,pathLines.get(i),left,contentTop+12+i*10-scroll,0xFFFFFFFF);
        for(int i=0;i<statusLines.size();i++)g.drawString(font,statusLines.get(i),left,statusY+i*10-scroll,0xFFD8C2E8);
        g.disableScissor();
        for(int i=0;i<Math.min(2,noticeLines.size());i++)g.drawString(font,noticeLines.get(i),left,height-53+i*10,0xFFBBBBBB);
        if(maxScroll>0) {
            int h=contentBottom-contentTop,thumb=Math.max(18,h*h/(h+maxScroll)),at=contentTop+(h-thumb)*scroll/maxScroll;
            g.fill(left+panelWidth+10,contentTop,left+panelWidth+13,contentBottom,0x55333333);
            g.fill(left+panelWidth+10,at,left+panelWidth+13,at+thumb,0xFFAAAAAA);
        }
    }
    @Override public boolean isPauseScreen(){return true;}
    @Override public void removed(){work.begin();LocalMusicPackManager.cancelScreenJobs();busy=false;super.removed();}
    @Override public void onClose(){ScreenHelper.setScreen(minecraft,parent);}
}
