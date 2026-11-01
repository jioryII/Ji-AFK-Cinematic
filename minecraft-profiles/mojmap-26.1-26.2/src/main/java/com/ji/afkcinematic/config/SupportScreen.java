package com.ji.afkcinematic.config;

import com.ji.afkcinematic.client.ReportComposer;
import com.ji.afkcinematic.diagnostic.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Human-readable form and review. Only the explicit final confirmation sends the v3 envelope. */
public final class SupportScreen extends Screen {
    private final Screen parent;
    private final ReportComposer composer = new ReportComposer(RuntimeDiagnostics::build, RuntimeDiagnostics::capture);
    private final List<AbstractWidget> fields = new ArrayList<>();
    private final Map<AbstractWidget,Integer> positions = new IdentityHashMap<>();
    private MultiLineEditBox messageField;
    private EditBox nameField, emailField;
    private Button bugButton, suggestionButton, diagnosticButton, updatesButton, sendButton, discordCornerButton, websiteCornerButton, supportWebsiteButton;
    private String name="", email="";
    private List<DiagnosticIncident> reviewedIncidents = List.of();
    private HttpReportTransport submission;
    private boolean sending;
    private String receivedId;
    private int scroll,maxScroll,formTop=88,formBottom,left,panelWidth,privacyY,noteY,typeY;
    private final DiagnosticReport.Build localBuild=RuntimeDiagnostics.build();
    private Component feedback=Component.empty();
    public SupportScreen(Screen parent){super(label("title"));this.parent=parent;}
    public static boolean isSupportScreen(Object screen){return screen instanceof SupportScreen || screen instanceof SupportScreen.PayloadReviewScreen;}
    private static net.minecraft.network.chat.MutableComponent label(String key,Object...args){return Component.translatable("support.ji_afkcinematic."+key,args);}
    private static Component toggle(String key,boolean enabled){return label(key).append(Component.literal(enabled?" ✓ ":" ✕ ")).append(label(enabled?"yes":"no")).withStyle(enabled?ChatFormatting.GREEN:ChatFormatting.RED);}
    private Button button(Component text,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(text,b->action.run()).bounds(x,y,w,20).build());}
    private EditBox input(String key,int maximum,String value,java.util.function.Consumer<String> changed){
        var box=new EditBox(font,left,formTop,panelWidth,20,label(key));box.setMaxLength(maximum);box.setValue(value);box.setHint(label(key));box.setResponder(changed);fields.add(box);return addRenderableWidget(box);
    }
    @Override protected void init(){
        fields.clear();positions.clear();panelWidth=Math.min(380,width-40);left=(width-panelWidth)/2;formBottom=height-65;
        int half=(panelWidth-8)/2;
        bugButton=button(label("bug"),left,60,half,()->choose(ReportComposer.Type.BUG));
        suggestionButton=button(label("suggestion"),left+half+8,60,half,()->choose(ReportComposer.Type.SUGGESTION));
        messageField=MultiLineEditBox.builder().setX(left).setY(formTop).setPlaceholder(label("message")).build(font,panelWidth,messageHeight(),label("message"));
        messageField.setCharacterLimit(4096);messageField.setValue(composer.message());messageField.setValueListener(value->{composer.message(value);invalidate();});
        addRenderableWidget(messageField);fields.add(messageField);
        diagnosticButton=button(toggle("diagnostic",composer.includeDiagnostic()),left,formTop,panelWidth,()->{
            composer.includeDiagnostic(!composer.includeDiagnostic());diagnosticButton.setMessage(toggle("diagnostic",composer.includeDiagnostic()));invalidate();
        });fields.add(diagnosticButton);
        updatesButton=button(toggle("updates",composer.updates()),left,formTop,panelWidth,()->{
            composer.updates(!composer.updates(),language());if(!composer.updates()){name="";email="";}invalidate();rebuildWidgets();
        });fields.add(updatesButton);
        nameField=input("name",60,name,value->{name=value;invalidate();});emailField=input("email",254,email,value->{email=value;invalidate();});
        supportWebsiteButton=addRenderableWidget(Button.builder(label("web_support").withStyle(ChatFormatting.AQUA),
                ConfirmLinkScreen.confirmLink(this,java.net.URI.create("https://ji-afk-cinematic.ji-mods.com/"+language()+"/support/")))
                .bounds(left,formTop,panelWidth,20).build());
        fields.add(supportWebsiteButton);
        int footerWidth=Math.min(panelWidth,width-150),footerLeft=(width-footerWidth)/2,footerHalf=(footerWidth-8)/2;
        sendButton=button(label("send").withStyle(ChatFormatting.GREEN),footerLeft,height-29,footerHalf,()->review());
        button(label("cancel"),footerLeft+footerHalf+8,height-29,footerHalf,this::onClose);
        discordCornerButton=addRenderableWidget(Button.builder(Component.literal("§e⚠"),ConfirmLinkScreen.confirmLink(this,java.net.URI.create("https://discord.gg/sE27D5SNaq")))
            .tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.report"))).bounds(width-35,height-35,30,30).build());
        websiteCornerButton=addRenderableWidget(Button.builder(Component.literal(""),ConfirmLinkScreen.confirmLink(this,java.net.URI.create("https://ji-afk-cinematic.ji-mods.com")))
            .tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.website"))).bounds(width-70,height-35,30,30).build());
        if(RuntimeResilience.pendingCount()>0)feedback=RuntimeIncidentText.summary();
        layout();updateButtons();
    }
    private int messageHeight(){return Math.max(24,Math.min(100,height-213));}
    private String language(){return minecraft.getLanguageManager().getSelected().startsWith("es")?"es":"en";}
    private void choose(ReportComposer.Type type){composer.type(type);scroll=0;invalidate();layout();}
    private void layout(){
        positions.clear();int y=formTop;typeY=y;y+=14;noteY=y;y+=46;
        positions.put(messageField,y);y+=messageField.getHeight()+10;
        privacyY=y;if(composer.type()==ReportComposer.Type.BUG){y+=font.split(label("diagnostic_privacy"),panelWidth).size()*10+8;positions.put(diagnosticButton,y);y+=30;}
        y+=10;positions.put(updatesButton,y);y+=28;
        if(composer.updates()){positions.put(nameField,y);y+=26;positions.put(emailField,y);y+=26;}
        y+=8;positions.put(supportWebsiteButton,y);y+=28;
        maxScroll=Math.max(0,y-formBottom);scroll=Math.max(0,Math.min(scroll,maxScroll));applyScroll();
    }
    private void applyScroll(){for(var widget:fields){
        int y=positions.getOrDefault(widget,-10000)-scroll;widget.setY(y);
        widget.visible=positions.containsKey(widget)&&y>=formTop&&y+widget.getHeight()<=formBottom;
        widget.active=widget.visible&&!sending;
    }}
    private void invalidate(){submission=null;receivedId=null;composer.invalidate();feedback=Component.empty();updateButtons();}
    private boolean prepare(){
        try{composer.message(messageField.getValue());composer.contact(composer.updates()?name:"",composer.updates()?email:"");composer.updates(composer.updates(),language());composer.prepare();return true;}
        catch(RuntimeException error){feedback=label("EMPTY_MESSAGE".equals(error.getMessage())?"error_empty":"INVALID_CONTACT".equals(error.getMessage())||"CONTACT_REQUIRED".equals(error.getMessage())?"contact_invalid":"error_generic");return false;}
    }
    private void updateButtons(){
        if(bugButton!=null)bugButton.active=!sending&&composer.type()!=ReportComposer.Type.BUG;
        if(suggestionButton!=null)suggestionButton.active=!sending&&composer.type()!=ReportComposer.Type.SUGGESTION;
        if(sendButton!=null)sendButton.setTooltip(HttpReportTransport.cooldownSeconds(composer.type()==ReportComposer.Type.SUGGESTION)>0?Tooltip.create(label("cooldown",Math.max(1,(HttpReportTransport.cooldownSeconds(composer.type()==ReportComposer.Type.SUGGESTION)+59)/60))):null);
        if(sendButton!=null)sendButton.active=!sending&&receivedId==null&&HttpReportTransport.cooldownSeconds(composer.type()==ReportComposer.Type.SUGGESTION)==0;
    }
    private void review(){if(sending||!prepare())return;com.ji.afkcinematic.ScreenHelper.setScreen(minecraft,new PayloadReviewScreen());}
    private void sendReviewed(DiagnosticReport.Preview reviewed,long revision,boolean suggestion){
        if(sending||receivedId!=null||composer.revision()!=revision||HttpReportTransport.cooldownSeconds(composer.type()==ReportComposer.Type.SUGGESTION)>0)return;
        if(submission==null||!submission.payload().equals(reviewed.payload()))submission=new HttpReportTransport(reviewed,suggestion);
        sending=true;feedback=label("sending");updateButtons();applyScroll();
        submission.send(true).whenComplete((receipt,error)->minecraft.execute(()->{
            sending=false;
            if(composer.revision()==revision){
                if(error==null){if(composer.includeDiagnostic()&&!suggestion)RuntimeResilience.reviewed(reviewedIncidents);receivedId=receipt.id();feedback=label(composer.updates()&&!receipt.updatesQueued()?"received_updates_failed":"received",receivedId);}
                else{Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();feedback=cause instanceof HttpReportTransport.CooldownException?label("cooldown",Math.max(1,(HttpReportTransport.cooldownSeconds(composer.type()==ReportComposer.Type.SUGGESTION)+59)/60)):label("send_failed");}
            }
            if(com.ji.afkcinematic.ScreenHelper.getCurrentScreen(minecraft)==this){updateButtons();applyScroll();}
        }));
    }
    @Override public void tick(){super.tick();updateButtons();}
    @Override public void extractRenderState(GuiGraphicsExtractor context,int mouseX,int mouseY,float delta){
        context.fill(0,0,width,height,0xEE17171D);context.fill(left-12,formTop-8,left+panelWidth+12,formBottom+5,0xFF202028);
        if(discordCornerButton!=null)discordCornerButton.setMessage(Component.literal(com.ji.afkcinematic.client.SupportCornerIcon.at(System.currentTimeMillis())));
        if(websiteCornerButton!=null)websiteCornerButton.setMessage(Component.literal(com.ji.afkcinematic.client.ReleaseLabel.websiteIcon(System.currentTimeMillis())));
        super.extractRenderState(context,mouseX,mouseY,delta);ScreenBrand.drawWebsiteGlobe(context,websiteCornerButton);ScreenBrand.draw(context,font,width,8);context.centeredText(font,title,width/2,43,0xFFE4D5ED);
        context.enableScissor(left,formTop,left+panelWidth,formBottom);
        context.text(font,label(composer.type()==ReportComposer.Type.BUG?"report_heading":"suggestion_heading"),left,typeY-scroll,0xFFF0E9DA);
        int noteTop=noteY-scroll;
        context.fill(left,noteTop,left+panelWidth,noteTop+40,0xFF302E23);
        context.fill(left,noteTop,left+2,noteTop+40,0xFFC4AC54);
        context.text(font,label("local_note"),left+9,noteTop+6,0xFFE9CF7A);
        context.text(font,Component.literal("Minecraft "+localBuild.mc()+" · "+localBuild.loader().name()+" "+localBuild.loaderVersion()),left+9,noteTop+17,0xFFE1DBCA);
        context.text(font,Component.literal("Ji AFK Cinematic "+com.ji.afkcinematic.client.ReleaseLabel.display(localBuild.ji())),left+9,noteTop+27,0xFFE1DBCA);
        if(composer.type()==ReportComposer.Type.BUG){var lines=font.split(label("diagnostic_privacy"),panelWidth);for(int i=0;i<lines.size();i++)context.text(font,lines.get(i),left,privacyY-scroll+i*10,0xFFB9BCC8);}
        context.disableScissor();
        var lines=font.split(feedback,panelWidth);for(int i=0;i<Math.min(2,lines.size());i++)context.text(font,lines.get(i),left,height-58+i*10,0xFFE5D8BD);
        if(maxScroll>0){int h=formBottom-formTop,thumb=Math.max(12,h*h/(h+maxScroll));context.fill(left+panelWidth+5,formTop,left+panelWidth+7,formBottom,0xFF303039);int y=formTop+(h-thumb)*scroll/maxScroll;context.fill(left+panelWidth+5,y,left+panelWidth+7,y+thumb,0xFFB8A6C9);}
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(messageField.visible&&messageField.isMouseOver(x,y))return super.mouseScrolled(x,y,horizontal,vertical);
        if(y>=formTop&&y<formBottom){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(vertical*24)));applyScroll();return true;}return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean isPauseScreen(){return true;}
    @Override public void onClose(){if(sending)return;name="";email="";composer.contact("","");composer.updates(false,language());composer.invalidate();com.ji.afkcinematic.ScreenHelper.setScreen(minecraft,parent);}

    private final class PayloadReviewScreen extends Screen {
        private final String message=composer.message();
        private final boolean suggestion=composer.type()==ReportComposer.Type.SUGGESTION;
        private DiagnosticReport.Preview diagnostic,reviewed;
        private String prettyDiagnostic="";
        private final Map<String,List<net.minecraft.util.FormattedCharSequence>> layoutCache=new HashMap<>();
        private long layoutGeneration=-1;
        private Object layoutLanguage,layoutFont;
        private long revision;
        private int offset,maximum,x,w,top=65,bottom,diagnosticY,toggleY;
        private Button diagnosticToggle;
        PayloadReviewScreen(){super(label("review_title"));}
        private void refresh(){
            if(composer.includeDiagnostic()&&!suggestion&&diagnostic==null){
                var snapshot=RuntimeDiagnostics.capture();reviewedIncidents=snapshot.incidents();
                diagnostic=DiagnosticReport.bug(snapshot,message,DiagnosticReport.MAX_BYTES-8192);
                var tree=com.google.gson.JsonParser.parseString(diagnostic.payload());
                int omitted=tree.getAsJsonObject().getAsJsonObject("omitted").get("incidents").getAsInt();
                reviewedIncidents=List.copyOf(snapshot.incidents().subList(omitted,snapshot.incidents().size()));
                prettyDiagnostic=new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(tree);
            }
            reviewed=composer.prepareWithDiagnostic(diagnostic);revision=composer.revision();
        }
        @Override protected void init(){
            w=Math.min(420,width-48);x=(width-w)/2;bottom=height-58;layoutCache.clear();refresh();
            diagnosticToggle=addRenderableWidget(Button.builder(toggle("diagnostic",composer.includeDiagnostic()),b->{
                composer.includeDiagnostic(!composer.includeDiagnostic());offset=0;refresh();rebuildWidgets();
            }).bounds(x,top,w,20).build());diagnosticToggle.visible=!suggestion;
            int half=(w-8)/2;
            var send=addRenderableWidget(Button.builder(label("send_confirm").withStyle(ChatFormatting.GREEN),b->{
                com.ji.afkcinematic.ScreenHelper.setScreen(minecraft,SupportScreen.this);sendReviewed(reviewed,revision,suggestion);
            }).bounds(x,height-29,half,20).build());send.active=!sending&&receivedId==null&&HttpReportTransport.cooldownSeconds(composer.type()==ReportComposer.Type.SUGGESTION)==0;
            addRenderableWidget(Button.builder(label("back"),b->onClose()).bounds(x+half+8,height-29,half,20).build());
        }
        private int box(GuiGraphicsExtractor g,String value,int y,int color){
            if(layoutGeneration!=RuntimeResilience.layoutGeneration() || layoutLanguage!=net.minecraft.locale.Language.getInstance() || layoutFont!=font){
                layoutCache.clear();layoutGeneration=RuntimeResilience.layoutGeneration();layoutLanguage=net.minecraft.locale.Language.getInstance();layoutFont=font;
            }
            var lines=layoutCache.computeIfAbsent(value,text->font.split(Component.literal(text),w-20));int h=Math.max(48,lines.size()*10+18);
            g.fill(x,y,x+w,y+h,0xFF24242E);for(int i=0;i<lines.size();i++)g.text(font,lines.get(i),x+10,y+9+i*10,color);return y+h;
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
            g.fill(0,0,width,height,0xFF17171D);ScreenBrand.draw(g,font,width,8);g.centeredText(font,title,width/2,44,0xFFE4D5ED);
            g.enableScissor(x,top,x+w,bottom);int y=box(g,message,top-offset,0xFFF0F0F4)+12;
            toggleY=y;diagnosticToggle.setY(y);diagnosticToggle.visible=!suggestion&&y>=top&&y+20<=bottom;
            if(!suggestion)y+=30;
            if(!suggestion&&composer.includeDiagnostic()&&diagnostic!=null){
                String pretty=prettyDiagnostic;
                y=box(g,pretty,y,0xFFB9C4DE)+12;
            }
            maximum=Math.max(0,y+offset-bottom);g.disableScissor();
            super.extractRenderState(g,mouseX,mouseY,delta);
            g.text(font,label("review_notice"),x,height-47,0xFFB9BCC8);
        }
        @Override public boolean mouseScrolled(double x,double y,double h,double v){if(y>=top&&y<bottom){offset=Math.max(0,Math.min(maximum,offset-(int)(v*28)));return true;}return super.mouseScrolled(x,y,h,v);}
        @Override public boolean isPauseScreen(){return true;}
        @Override public void onClose(){com.ji.afkcinematic.ScreenHelper.setScreen(minecraft,SupportScreen.this);}
    }
}
