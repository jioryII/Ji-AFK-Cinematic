package com.ji.afkcinematic.config;
import com.ji.afkcinematic.diagnostic.HttpSubscriptionTransport;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Only sends the fields the player explicitly enters. No identity lookup or local persistence. */
public final class SubscriptionScreen extends Screen {
    private final Screen parent;
    private EditBox nameField, emailField;
    private Button consentButton, sendButton;
    private boolean consent, sending, received;
    private Component feedback = label("privacy");
    private int left, top, formWidth;
    public SubscriptionScreen(Screen parent) { super(label("title")); this.parent = parent; }
    private static Component label(String key) { return Component.translatable("notifications.ji_afkcinematic." + key); }
    @Override protected void init() {
        String oldName = nameField == null ? "" : nameField.getValue();
        String oldEmail = emailField == null ? "" : emailField.getValue();
        formWidth = Math.min(360, Math.max(80, this.width - 40)); left = (this.width-formWidth)/2; top = Math.max(32,(this.height-194)/2);
        nameField = new EditBox(this.font,left,top+14,formWidth,20,label("name")); nameField.setMaxLength(60); nameField.setValue(oldName);nameField.setResponder(value -> update()); this.addRenderableWidget(nameField);
        emailField = new EditBox(this.font,left,top+54,formWidth,20,label("email")); emailField.setMaxLength(254);emailField.setValue(oldEmail);emailField.setResponder(value -> update());this.addRenderableWidget(emailField);
        consentButton=button(label(consent?"consent_yes":"consent_no"),top+82,()->{consent=!consent;consentButton.setMessage(label(consent?"consent_yes":"consent_no"));update();});
        sendButton=button(label("subscribe"),top+108,this::send);
        button(label("back"),top+162,this::onClose);update();
    }
    private Button button(Component text,int y,Runnable action) {return this.addRenderableWidget(Button.builder(text,b->action.run()).bounds(left,y,formWidth,20).build());}
    private void update() {
        if(sendButton!=null)sendButton.active=!sending&&!received&&consent&&nameField!=null&&emailField!=null&&!nameField.getValue().isBlank()&&!emailField.getValue().isBlank();
        if(nameField!=null)nameField.setEditable(!sending&&!received);
        if(emailField!=null)emailField.setEditable(!sending&&!received);
        if(consentButton!=null)consentButton.active=!sending&&!received;
    }
    private void send() {
        if(sending||received||!consent)return;
        String language = label("language").getString();
        sending=true;feedback=label("sending");update();
        HttpSubscriptionTransport.subscribe(nameField.getValue(),emailField.getValue(),"es".equals(language)?"es":"en",true).whenComplete((ignored,error)->this.minecraft.execute(()->{
            sending=false;received=error==null;feedback=label(error==null?"queued":"failed");
            if(received){nameField.setValue("");emailField.setValue("");consent=false;}
            if(com.ji.afkcinematic.ScreenHelper.getCurrentScreen(this.minecraft)==this)update();
        }));
    }
    @Override public void render(GuiGraphics context,int mouseX,int mouseY,float delta) {
        context.fill(0,0,this.width,this.height,0xEE17171D);super.render(context,mouseX,mouseY,delta);
        context.drawCenteredString(this.font,this.title,this.width/2,12,0xFFFFFFFF);
        context.drawString(this.font,label("name"),left,top+2,0xFFE5D8BD);
        context.drawString(this.font,label("email"),left,top+42,0xFFE5D8BD);
        var lines=this.font.split(feedback,formWidth);
        for(int i=0;i<Math.min(2,lines.size());i++)context.drawString(this.font,lines.get(i),left,top+134+i*10,0xFFE5D8BD);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){if(nameField!=null)nameField.setValue("");if(emailField!=null)emailField.setValue("");com.ji.afkcinematic.ScreenHelper.setScreen(this.minecraft,parent);}
}
