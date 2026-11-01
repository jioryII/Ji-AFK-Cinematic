package com.ji.afkcinematic.diagnostic;
import net.minecraft.network.chat.Component;
/** User-facing attribution reflects evidence confidence, never installation alone. */
public final class RuntimeIncidentText {
    private RuntimeIncidentText(){ }
    public static Component summary() {
        var incidents=RuntimeResilience.incidents();
        for(int i=incidents.size()-1;i>=0;i--) {
            var entry=incidents.get(i);
            if(entry.status()!=DiagnosticIncident.Status.PENDING)continue;
            String provider=entry.candidates().isEmpty()?"":String.join(", ",entry.candidates());
            if(provider.equals("spatial_gui"))provider="Spatial GUI";
            return Component.translatable("support.ji_afkcinematic."+switch(entry.confidence()) {
                case UNKNOWN->"incident_unknown";case POSSIBLE->"incident_possible";case CONFIRMED->"incident_confirmed";
            },provider);
        }
        return Component.empty();
    }
}
