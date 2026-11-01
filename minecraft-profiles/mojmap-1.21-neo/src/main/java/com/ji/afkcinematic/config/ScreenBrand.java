package com.ji.afkcinematic.config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.ji.afkcinematic.client.ReleaseLabel;

final class ScreenBrand {
    private ScreenBrand() {}
    static void draw(GuiGraphics g,Font font,int width,int top) {
        g.drawCenteredString(font,Component.literal("§6§lJi AFK Cinematic"),width/2,top,0xFFFFFFFF);
        g.drawCenteredString(font,Component.literal("§5By jiory_"),width/2,top+11,0xFFFFFFFF);
        g.pose().pushPose();g.pose().scale(.55f,.55f,1f);
        g.drawCenteredString(font,Component.literal("v"+ReleaseLabel.display(com.ji.afkcinematic.PlatformAccess.modVersion())),
                (int)(width/(2*.55f)),(int)((top+23)/.55f),0xFFFFD75E);
        g.pose().popPose();
    }
    static void drawWebsiteGlobe(GuiGraphics g,net.minecraft.client.gui.components.Button button) {
        if(button==null||!button.visible)return;
        int x=button.getX()+15,y=button.getY()+15,c=0xFF69E4F1;
        g.fill(x-3,y-6,x+3,y-5,c);g.fill(x-3,y+5,x+3,y+6,c);
        g.fill(x-5,y-5,x-3,y-4,c);g.fill(x+3,y-5,x+5,y-4,c);
        g.fill(x-5,y+4,x-3,y+5,c);g.fill(x+3,y+4,x+5,y+5,c);
        g.fill(x-6,y-3,x-5,y+3,c);g.fill(x+5,y-3,x+6,y+3,c);
        g.fill(x-5,y-4,x-4,y+4,c);g.fill(x+4,y-4,x+5,y+4,c);
        g.fill(x-5,y,x+5,y+1,c);g.fill(x-4,y-3,x+4,y-2,c);g.fill(x-4,y+2,x+4,y+3,c);
        g.fill(x-2,y-5,x-1,y+5,c);g.fill(x+1,y-5,x+2,y+5,c);
    }

}
