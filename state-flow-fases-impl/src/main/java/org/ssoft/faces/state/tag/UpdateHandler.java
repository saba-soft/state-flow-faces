/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state.tag;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.state.model.StateChart;
import javax.faces.view.facelets.FaceletContext;

/**
 *
 * @author Waldemar Kłaczyński
 */
public interface UpdateHandler {
    
    void update(FaceletContext ctx, UIComponent parent, StateChart chart, Object parentFlow) throws IOException;
    
    
}