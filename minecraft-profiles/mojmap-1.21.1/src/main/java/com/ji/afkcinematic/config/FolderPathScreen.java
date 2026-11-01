package com.ji.afkcinematic.config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
/** Portable path editor; changes only the parent's working copy. */
public final class FolderPathScreen extends Screen {
    private final Screen parent; private final java.util.function.Consumer<String> result;
    private String value; private EditBox field;
    public FolderPathScreen(Screen parent, String value, java.util.function.Consumer<String> result) {
        super(Component.translatable("config.ji_afkcinematic.folder_path")); this.parent=parent; this.value=value; this.result=result;
    }
    @Override protected void init() {
        int w=Math.min(380,width-32), x=(width-w)/2, y=height/2;
        field=addRenderableWidget(new EditBox(font,x,y-20,w,20,title)); field.setMaxLength(1024); field.setValue(value); field.setResponder(v -> value=v);
        addRenderableWidget(Button.builder(Component.translatable("config.ji_afkcinematic.save"), b -> {
            try { if (!value.isBlank()) java.nio.file.Path.of(value); result.accept(value.strip()); onClose(); }
            catch (RuntimeException ignored) { field.setTextColor(0xFFFF7777); }
        }).bounds(x,y+12,(w-8)/2,20).build());
        addRenderableWidget(Button.builder(Component.translatable("config.ji_afkcinematic.cancel"),b->onClose()).bounds(x+(w+8)/2,y+12,(w-8)/2,20).build());
        setInitialFocus(field);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta) { g.fill(0,0,width,height,0xEE17171D); super.render(g,x,y,delta); g.drawCenteredString(font,title,width/2,height/2-44,0xFFFFFFFF); }
    @Override public boolean isPauseScreen(){return true;}
    @Override public void onClose(){com.ji.afkcinematic.ScreenHelper.setScreen(minecraft,parent);}
}
