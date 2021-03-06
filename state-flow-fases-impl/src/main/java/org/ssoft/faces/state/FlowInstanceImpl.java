/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state;

import java.io.IOException;
import java.util.concurrent.Callable;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.FacesContext;
import javax.faces.state.FlowInstance;
import javax.faces.state.PathResolver;
import javax.faces.state.PathResolverHolder;
import javax.faces.state.StateFlowExecutor;
import javax.faces.state.invoke.Invoker;
import javax.faces.state.model.Invoke;
import javax.faces.state.model.State;
import org.ssoft.faces.state.cdi.CdiUtil;
import org.ssoft.faces.state.utils.Util;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class FlowInstanceImpl extends FlowInstance {

    public FlowInstanceImpl(StateFlowExecutor executor) {
        super(executor);
    }

    @Override
    protected void postNewInvoker(Invoke invoke, Invoker invoker) throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();

        PathResolver pr = invoke.getPathResolver();
        try {
            if (pr != null) {
                FlowInstance.push(PathResolver.class, pr);
            }

            if (invoker instanceof PathResolverHolder) {
                PathResolverHolder ph = (PathResolverHolder) invoker;
                ph.setPathResolver(pr);
            }

            if (Util.isCdiAvailable(fc)) {
                BeanManager bm = Util.getCdiBeanManager(fc);
                CdiUtil.injectFields(bm, invoker);
            }

            Util.postConstruct(invoker);

        } finally {
            if (pr != null) {
                FlowInstance.pop(PathResolver.class, pr);
            }
        }
    }

    @Override
    protected <V> V processInvoke(State target, Invoke invoke, Invoker invoker, Callable<V> fn) throws Exception {
        FacesContext fc = FacesContext.getCurrentInstance();

        PathResolver pr = invoke.getPathResolver();

        try {
            if (pr != null) {
                FlowInstance.push(PathResolver.class, pr);
            }

            return fn.call();

        } finally {
            if (pr != null) {
                FlowInstance.pop(PathResolver.class, pr);
            }
        }

    }

}
