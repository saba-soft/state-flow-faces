/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.demo.basic;

import java.io.Serializable;
import javax.inject.Named;
import javax.faces.state.annotation.StateChartScoped;

/**
 *
 * @author Waldemar Kłaczyński
 */
@StateChartScoped
@Named("main")
public class MainBean implements Serializable {
    
    public boolean prepare() {
        return true;
    }
    
}
