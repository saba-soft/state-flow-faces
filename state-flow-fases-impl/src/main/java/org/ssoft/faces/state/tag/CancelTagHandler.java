/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state.tag;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.state.model.Cancel;
import javax.faces.state.model.Finalize;
import javax.faces.state.model.If;
import javax.faces.state.model.OnEntry;
import javax.faces.state.model.OnExit;
import javax.faces.state.model.StateChart;
import javax.faces.state.model.Transition;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class CancelTagHandler extends AbstractFlowTagHandler<Cancel> {

    protected final TagAttribute sendid;

    public CancelTagHandler(TagConfig config) {
        super(config, Cancel.class);
        
        in("onentry", OnEntry.class);
        in("onexit", OnExit.class);
        in("transition", Transition.class);
        in("finalize", Finalize.class);
        in("if", If.class);

        top("onentry", OnEntry.class);
        top("onexit", OnExit.class);
        top("transition", Transition.class);
        top("finalize", Finalize.class);

        this.sendid = this.getRequiredAttribute("sendid");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, StateChart chart, Object parentElement) throws IOException {
        Cancel action = new Cancel();
        decorate(ctx, parent, action);

        action.setSendid(sendid.getValue());
        
        applyNext(ctx, parent, action);
        
        addAction(ctx, parent, action);
    }

}
