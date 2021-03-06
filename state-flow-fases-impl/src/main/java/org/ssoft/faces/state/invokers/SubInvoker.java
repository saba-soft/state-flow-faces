/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state.invokers;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.state.FlowContext;
import javax.faces.state.FlowStatus;
import javax.faces.state.FlowTriggerEvent;
import javax.faces.state.ModelException;
import javax.faces.state.StateFlowExecutor;
import javax.faces.state.StateFlowHandler;
import javax.faces.state.invoke.AbstractInvoker;
import javax.faces.state.invoke.Invoker;
import javax.faces.state.invoke.InvokerException;
import javax.faces.state.model.State;
import javax.faces.state.model.StateChart;
import org.ssoft.faces.state.utils.AsyncTrigger;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class SubInvoker extends AbstractInvoker implements Invoker {

    /**
     * Event prefix, all events sent to the parent executor must begin with this
     * prefix.
     */
    private String eventPrefix;
    /**
     * Cancellation status.
     */
    private boolean cancelled;
    //// Constants
    /**
     * Prefix for all events sent to the parent state machine.
     */
    private static final String invokePrefix = ".invoke.";
    /**
     * Suffix for invoke done event.
     */
    private static final String invokeCancelResponse = "cancel.response";

    public SubInvoker() {
        super();
    }

    @Override
    public void setParentStateId(final String parentStateId) {
        super.setParentStateId(parentStateId);
        this.eventPrefix = this.parentStateId + invokePrefix;
        this.cancelled = false;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void invoke(final String source, final Map params) throws InvokerException {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();

            StateFlowHandler handler = StateFlowHandler.getInstance();

            String viewId = source;
            String realPath = ec.getRealPath("/");
            if (viewId.contains(realPath)) {
                viewId = viewId.substring(realPath.length());
            }
            String contextPath = ec.getApplicationContextPath();
            if (viewId.startsWith(contextPath)) {
                //viewId = viewId.substring(viewId.indexOf(contextPath));
                viewId = viewId.substring(contextPath.length());
            }
            int pos = viewId.indexOf("META-INF/resources/");
            if (pos >= 0) {
                viewId = viewId.substring(pos + 18);
            }

            StateChart stateMachine = handler.createStateMachine(fc, viewId);

            handler.startExecutor(fc, stateMachine, params, false);
        } catch (ModelException ex) {
            Logger.getLogger(SubInvoker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void parentEvents(final FlowTriggerEvent[] evts) throws InvokerException {
        if (cancelled) {
            return;
        }
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        StateFlowHandler handler = StateFlowHandler.getInstance();

        StateFlowExecutor executor = handler.getExecutor(fc, instance.getExecutor());
        boolean doneBefore = executor.getCurrentStatus().isFinal();
        try {
            executor.triggerEvents(evts);
        } catch (ModelException me) {
            throw new InvokerException(me.getMessage(), me.getCause());
        }
        if (!doneBefore && executor.getCurrentStatus().isFinal()) {
            FlowContext ctx = executor.getRootContext();
            if (ctx.has("__@result@__")) {
                FlowContext result = (FlowContext) ctx.get("__@result@__");
                FlowStatus pstatus = instance.getExecutor().getCurrentStatus();
                State pstate = (State) pstatus.getStates().iterator().next();
                FlowContext pcontext = instance.getContext(pstate);
                pcontext.setLocal("__@result@__", result);
            }
            handler.stopExecutor(fc, instance.getExecutor());
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void cancel() throws InvokerException {
        cancelled = true;
        FlowTriggerEvent te = new FlowTriggerEvent(eventPrefix + invokeCancelResponse, FlowTriggerEvent.SIGNAL_EVENT);
        new AsyncTrigger(instance.getExecutor(), te).start();
    }

}
