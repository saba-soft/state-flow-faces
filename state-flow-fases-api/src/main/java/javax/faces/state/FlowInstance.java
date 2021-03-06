/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javax.faces.state;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.faces.context.FacesContext;
import javax.faces.state.invoke.Invoker;
import javax.faces.state.invoke.InvokerException;
import javax.faces.state.model.Datamodel;
import javax.faces.state.model.History;
import javax.faces.state.model.Invoke;
import javax.faces.state.model.State;
import javax.faces.state.model.StateChart;
import static javax.faces.state.model.StateChart.STATE_MACHINE_HINT;
import javax.faces.state.model.TransitionTarget;
import javax.faces.state.utils.StateFlowHelper;

/**
 *
 * @author Waldemar Kłaczyński
 */
public abstract class FlowInstance {

    static final String CURRENT_STACK_KEY = "javax.faces.state.CURRENT_STACK";

    /**
     * The notification registry.
     */
    private FlowNotificationRegistry notificationRegistry;

    /**
     * The <code>Map</code> of <code>Context</code>s per
     * <code>TransitionTarget</code>.
     */
    private final Map<TransitionTarget, FlowContext> contexts;

    /**
     * The <code>Map</code> of last known configurations per
     * <code>History</code>.
     */
    private final Map<History, Set<TransitionTarget>> histories;

    /**
     * <code>Map</code> for recording the processInvoke to completion status of
     * composite states.
     */
    private final Map<TransitionTarget, Boolean> completions;

    /**
     * The <code>Invoker</code> classes <code>Map</code>, keyed by
     * &lt;invoke&gt; target types (specified using "targettype" attribute).
     */
    private final Map<String, Class> invokerClasses;

    /**
     * The <code>Map</code> of active <code>Invoker</code>s, keyed by (leaf)
     * <code>State</code>s.
     */
    private final Map<TransitionTarget, Invoker> invokers;

    /**
     * The evaluator for expressions.
     */
    private FlowEvaluator evaluator;

    /**
     * The root context.
     */
    private FlowContext rootContext;

    /**
     * The owning state machine executor.
     */
    private final StateFlowExecutor executor;

    /**
     * Constructor.
     *
     * @param executor The executor that this instance is attached to.
     */
    public FlowInstance(final StateFlowExecutor executor) {
        this.notificationRegistry = new FlowNotificationRegistry();
        this.contexts = Collections.synchronizedMap(new HashMap());
        this.histories = Collections.synchronizedMap(new HashMap());
        this.invokerClasses = Collections.synchronizedMap(new HashMap());
        this.invokers = Collections.synchronizedMap(new HashMap());
        this.completions = Collections.synchronizedMap(new HashMap());
        this.evaluator = null;
        this.rootContext = null;
        this.executor = executor;
    }

    /**
     * Get the <code>Evaluator</code>.
     *
     * @return The evaluator.
     */
    public FlowEvaluator getEvaluator() {
        return evaluator;
    }

    /**
     * Set the <code>Evaluator</code>.
     *
     * @param evaluator The evaluator.
     */
    public void setEvaluator(final FlowEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Get the root context.
     *
     * @return The root context.
     */
    public FlowContext getRootContext() {
        if (rootContext == null && evaluator != null) {
            rootContext = evaluator.newContext(null, null);
        }
        return rootContext;
    }

    /**
     * Set the root context.
     *
     * @param context The root context.
     */
    void setRootContext(final FlowContext context) {
        this.rootContext = context;
    }

    /**
     * Get the notification registry.
     *
     * @return The notification registry.
     */
    public FlowNotificationRegistry getNotificationRegistry() {
        return notificationRegistry;
    }

    /**
     * Set the notification registry.
     *
     * @param notifRegistry The notification registry.
     */
    void setNotificationRegistry(final FlowNotificationRegistry notifRegistry) {
        this.notificationRegistry = notifRegistry;
    }

    /**
     * Get the <code>Context</code> for this <code>TransitionTarget</code>. If
     * one is not available it is created.
     *
     * @param transitionTarget The TransitionTarget.
     * @return The Context.
     */
    public FlowContext getContext(final TransitionTarget transitionTarget) {
        FlowContext context = contexts.get(transitionTarget);
        if (context == null) {
            TransitionTarget parent = transitionTarget.getParent();
            if (parent == null) {
                // docroot
                context = evaluator.newContext(transitionTarget, getRootContext());
            } else {
                context = evaluator.newContext(transitionTarget, getContext(parent));
            }
            Datamodel datamodel = transitionTarget.getDatamodel();
            StateFlowHelper.cloneDatamodel(datamodel, context, evaluator);
            contexts.put(transitionTarget, context);
        }
        return context;
    }

    /**
     * Get the <code>Context</code> for this <code>TransitionTarget</code>. May
     * return <code>null</code>.
     *
     * @param transitionTarget The <code>TransitionTarget</code>.
     * @return The Context.
     */
    FlowContext lookupContext(final TransitionTarget transitionTarget) {
        return (FlowContext) contexts.get(transitionTarget);
    }

    /**
     * Set the <code>Context</code> for this <code>TransitionTarget</code>.
     *
     * @param transitionTarget The TransitionTarget.
     * @param context The Context.
     */
    void setContext(final TransitionTarget transitionTarget, final FlowContext context) {
        contexts.put(transitionTarget, context);
    }

    /**
     * Get the last configuration for this history.
     *
     * @param history The history.
     * @return Returns the lastConfiguration.
     */
    public Set<TransitionTarget> getLastConfiguration(final History history) {
        Set<TransitionTarget> lastConfiguration = (Set) histories.get(history);
        if (lastConfiguration == null) {
            lastConfiguration = new HashSet();
            histories.put(history, lastConfiguration);
        }
        return lastConfiguration;
    }

    /**
     * Set the last configuration for this history.
     *
     * @param history The history.
     * @param lc The lastConfiguration to set.
     */
    public void setLastConfiguration(final History history, final Set lc) {
        Set<TransitionTarget> lastConfiguration = getLastConfiguration(history);
        lastConfiguration.clear();
        lastConfiguration.addAll(lc);
    }

    /**
     * Check whether we have prior history.
     *
     * @param history The history.
     * @return Whether we have a non-empty last configuration
     */
    public boolean isEmpty(final History history) {
        Set<TransitionTarget> lastConfiguration = (Set) histories.get(history);
        return lastConfiguration == null || lastConfiguration.isEmpty();
    }

    /**
     * Resets the history state.
     *
     * @param history The history.
     * @see org.apache.commons.scxml.SCXMLExecutor#reset()
     */
    public void reset(final History history) {
        Set lastConfiguration = (Set) histories.get(history);
        if (lastConfiguration != null) {
            lastConfiguration.clear();
        }
    }

    /**
     * Get the {@link SCXMLExecutor} this instance is attached to.
     *
     * @return The SCXMLExecutor this instance is attached to.
     * @see org.apache.commons.scxml.SCXMLExecutor
     */
    public StateFlowExecutor getExecutor() {
        return executor;
    }

    /**
     * Register an {@link Invoker} class for this target type.
     *
     * @param targettype The target type (specified by "targettype" attribute of
     * &lt;invoke&gt; tag).
     * @param invokerClass The <code>Invoker</code> <code>Class</code>.
     */
    void registerInvokerClass(final String targettype, final Class invokerClass) {
        invokerClasses.put(targettype, invokerClass);
    }

    /**
     * Remove the {@link Invoker} class registered for this target type (if
     * there is one registered).
     *
     * @param targettype The target type (specified by "targettype" attribute of
     * &lt;invoke&gt; tag).
     */
    void unregisterInvokerClass(final String targettype) {
        invokerClasses.remove(targettype);
    }

    /**
     * Get the {@link Invoker} for this {@link TransitionTarget}. May return
     * <code>null</code>. A non-null <code>Invoker</code> will be returned if
     * and only if the <code>TransitionTarget</code> is currently active and
     * contains an &lt;invoke&gt; child.
     *
     * @param invoke
     * @param target
     * @return An {@link Invoker} for the specified type, if an invoker class is
     * registered against that type, <code>null</code> otherwise.
     * @throws InvokerException When a suitable {@link Invoker} cannot be
     * instantiated.
     */
    public Invoker newInvoker(final Invoke invoke, TransitionTarget target) throws InvokerException {
        String targettype = invoke.getTargettype();

        Class invokerClass = (Class) invokerClasses.get(targettype);
        if (invokerClass == null) {
            throw new InvokerException("No Invoker registered for "
                    + "targettype \"" + targettype + "\"");
        }
        Invoker invoker = null;
        try {
            invoker = (Invoker) invokerClass.newInstance();

            invoker.setParentStateId(target.getId());
            invoker.setInstance(this);

            if (invoker instanceof PathResolverHolder) {
                PathResolver pr = current(PathResolver.class);
                PathResolverHolder ph = (PathResolverHolder) invoker;
                ph.setPathResolver(pr);
            }

            postNewInvoker(invoke, invoker);

        } catch (InstantiationException | IllegalAccessException | IOException ie) {
            throw new InvokerException(ie.getMessage(), ie.getCause());
        }
        return invoker;
    }

    public <V> V process(final State target, final Invoker invoker, final Callable<V> fn) throws InvokerException {
        Invoke invoke = target.getInvoke();
        try {
            return processInvoke(target, invoke, invoker, fn);
        } catch (Throwable th) {
            throw new InvokerException(th);
        }
    }

    protected abstract <V> V processInvoke(final State target, final Invoke invoke, final Invoker invoker, final Callable<V> fn) throws Exception;

    protected abstract void postNewInvoker(final Invoke invoke, final Invoker invoker) throws IOException;

    /**
     * Get the {@link Invoker} for this {@link TransitionTarget}. May return
     * <code>null</code>. A non-null {@link Invoker} will be returned if and
     * only if the {@link TransitionTarget} is currently active and contains an
     * &lt;invoke&gt; child.
     *
     * @param transitionTarget The <code>TransitionTarget</code>.
     * @return The Invoker.
     */
    public Invoker getInvoker(final TransitionTarget transitionTarget) {
        return (Invoker) invokers.get(transitionTarget);
    }

    /**
     * Set the {@link Invoker} for this {@link TransitionTarget}.
     *
     * @param invoke
     * @param transitionTarget The TransitionTarget.
     * @param invoker The Invoker.
     */
    public void setInvoker(final TransitionTarget transitionTarget, final Invoke invoke, final Invoker invoker) {
        invokers.put(transitionTarget, invoker);
    }

    /**
     * Return the Map of {@link Invoker}s currently "active".
     *
     * @return The map of invokers.
     */
    public Map<TransitionTarget, Invoker> getInvokers() {
        return invokers;
    }

    /**
     * Get the completion status for this composite {@link TransitionTarget}.
     *
     * @param transitionTarget The <code>TransitionTarget</code>.
     * @return The completion status.
     *
     * @since 0.7
     */
    public boolean isDone(final TransitionTarget transitionTarget) {
        Boolean done = completions.get(transitionTarget);
        if (done == null) {
            return false;
        } else {
            return done;
        }
    }

    /**
     * Set the completion status for this composite {@link TransitionTarget}.
     *
     * @param transitionTarget The TransitionTarget.
     * @param done The completion status.
     *
     * @since 0.7
     */
    public void setDone(final TransitionTarget transitionTarget, final boolean done) {
        completions.put(transitionTarget, done ? Boolean.TRUE : Boolean.FALSE);
    }

    public Object saveState(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }

        Object values[] = new Object[5];

        if (rootContext != null) {
            values[0] = rootContext.saveState(context);
        }

        values[1] = saveContextsState(context);
        values[2] = saveHistoriesState(context);
        values[3] = saveCompletionsState(context);
        values[4] = saveInvokersState(context);

        return values;
    }

    public void restoreState(FacesContext context, Object state) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (state == null) {
            return;
        }

        Object[] values = (Object[]) state;

        FlowContext rctx = getRootContext();
        rctx.restoreState(context, values[0]);

        restoreContextsState(context, values[1]);
        restoreHistoriesState(context, values[2]);
        restoreCompletionsState(context, values[3]);
        restoreInvokersState(context, values[4]);

    }

    private Object saveContextsState(FacesContext context) {
        Object state = null;
        if (null != contexts && contexts.size() > 0) {
            Object[] attached = new Object[contexts.size()];
            int i = 0;
            for (Map.Entry<TransitionTarget, FlowContext> entry : contexts.entrySet()) {
                Object values[] = new Object[2];
                values[0] = entry.getKey().getClientId();
                values[1] = entry.getValue().saveState(context);
                attached[i++] = values;
            }
            state = attached;
        }
        return state;
    }

    private void restoreContextsState(FacesContext context, Object state) {
        StateChart chart = (StateChart) context.getAttributes().get(STATE_MACHINE_HINT);
        contexts.clear();

        if (null != state) {
            Object[] values = (Object[]) state;
            for (Object value : values) {
                Object[] entry = (Object[]) value;

                String ttid = (String) entry[0];
                Object found = chart.findElement(ttid);
                if (found == null) {
                    throw new IllegalStateException(String.format("Restored element %s not found.", ttid));
                }

                TransitionTarget target = (TransitionTarget) found;
                FlowContext tctx = getContext(target);
                tctx.restoreState(context, entry[1]);

            }
        }
    }

    private Object saveHistoriesState(FacesContext context) {
        Object state = null;
        if (null != histories && histories.size() > 0) {
            Object[] attached = new Object[histories.size()];
            int i = 0;
            for (Map.Entry<History, Set<TransitionTarget>> entry : histories.entrySet()) {
                Object values[] = new Object[2];
                values[0] = entry.getKey().getClientId();
                values[1] = saveTargetsState(context, entry.getValue());
                attached[i++] = values;
            }
            state = attached;
        }
        return state;
    }

    private void restoreHistoriesState(FacesContext context, Object state) {
        StateChart chart = (StateChart) context.getAttributes().get(STATE_MACHINE_HINT);
        histories.clear();

        if (null != state) {
            Object[] values = (Object[]) state;
            for (Object value : values) {
                Object[] entry = (Object[]) value;

                String ttid = (String) entry[0];
                Object found = chart.findElement(ttid);

                if (found == null) {
                    throw new IllegalStateException(String.format("Restored element %s not found.", ttid));
                }

                History history = (History) found;

                Set<TransitionTarget> last = (Set) histories.get(history);
                if (last == null) {
                    last = new HashSet();
                    histories.put(history, last);
                }
                restoreTargetsState(context, chart, last, entry[1]);
            }
        }
    }

    private Object saveTargetsState(FacesContext context, Collection<TransitionTarget> tatgets) {
        Object state = null;
        if (null != tatgets && tatgets.size() > 0) {
            Object[] attached = new Object[tatgets.size()];
            int i = 0;
            for (TransitionTarget ratget : tatgets) {
                attached[i++] = ratget.getClientId();
            }
            state = attached;
        }
        return state;
    }

    private void restoreTargetsState(FacesContext context, StateChart chart, Set<TransitionTarget> targets, Object state) {
        targets.clear();
        if (null != state) {
            Object[] values = (Object[]) state;
            for (Object value : values) {
                String ttid = (String) value;
                Object found = chart.findElement(ttid);
                if (found == null) {
                    throw new IllegalStateException(String.format("Restored element %s not found.", ttid));
                }

                TransitionTarget tt = (TransitionTarget) found;
                targets.add(tt);
            }
        }
    }

    private Object saveCompletionsState(FacesContext context) {
        Object state = null;
        if (null != completions && completions.size() > 0) {
            Object[] attached = new Object[completions.size()];
            int i = 0;
            for (Map.Entry<TransitionTarget, Boolean> entry : completions.entrySet()) {
                Object values[] = new Object[2];
                values[0] = entry.getKey().getClientId();
                values[1] = entry.getValue();
                attached[i++] = values;
            }
            state = attached;
        }
        return state;
    }

    private void restoreCompletionsState(FacesContext context, Object state) {
        StateChart chart = (StateChart) context.getAttributes().get(STATE_MACHINE_HINT);
        completions.clear();

        if (null != state) {
            Object[] values = (Object[]) state;
            for (Object value : values) {
                Object[] entry = (Object[]) value;

                String ttid = (String) entry[0];
                Object found = chart.findElement(ttid);
                if (found == null) {
                    throw new IllegalStateException(String.format("Restored element %s not found.", ttid));
                }

                TransitionTarget tt = (TransitionTarget) found;
                completions.put(tt, (Boolean) entry[1]);
            }
        }
    }

    private Object saveInvokersState(FacesContext context) {
        Object state = null;
        if (null != invokers && invokers.size() > 0) {
            Object[] attached = new Object[invokers.size()];
            int i = 0;
            for (Map.Entry<TransitionTarget, Invoker> entry : invokers.entrySet()) {
                Object values[] = new Object[3];
                Invoker invoker = entry.getValue();
                State tt = (State) entry.getKey();

                values[0] = entry.getKey().getClientId();
                values[1] = tt.getInvoke().getTargettype();
                values[2] = invoker.saveState(context);
                attached[i++] = values;
            }
            state = attached;
        }
        return state;
    }

    private void restoreInvokersState(FacesContext context, Object state) {
        StateChart chart = (StateChart) context.getAttributes().get(STATE_MACHINE_HINT);
        invokers.clear();

        if (null != state) {
            Object[] values = (Object[]) state;
            for (Object value : values) {
                Object[] entry = (Object[]) value;

                String ttid = (String) entry[0];
                Object found = chart.findElement(ttid);
                if (found == null) {
                    throw new IllegalStateException(String.format("Restored element %s not found.", ttid));
                }

                TransitionTarget tt = (TransitionTarget) found;
                String type = (String) values[1];

                State stt = (State) tt;
                Invoke invoke = stt.getInvoke();
                if (!invoke.getTargettype().equals(type)) {
                    throw new IllegalStateException(String.format("Bad invoker type %s in failed.", type, ttid));
                }

                Invoker invoker = null;
                try {

                    PathResolver pr = invoke.getPathResolver();

                    invoker = newInvoker(invoke, tt);
                } catch (InvokerException ex) {
                    throw new IllegalStateException(String.format("Restored invoker %s failed.", ttid), ex);
                } finally {

                }

                invokers.put(tt, invoker);
            }
        }
    }

    public static FlowInstance current() {
        return current(FlowInstance.class);
    }

    public static <T> T current(Class<T> type) {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<Object, Object> contextAttributes = context.getAttributes();
        ArrayDeque<T> elStack = getObjectStack(type, contextAttributes);
        return elStack.peek();
    }

    public static <T> void push(Class<T> type, T component) {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<Object, Object> contextAttributes = context.getAttributes();
        ArrayDeque<T> elStack = getObjectStack(type, contextAttributes);
        elStack.push(component);
    }

    public static <T> void pop(Class<T> type, T component) {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<Object, Object> contextAttributes = context.getAttributes();
        ArrayDeque<T> elStack = getObjectStack(type, contextAttributes);

        for (T topComponent = elStack.peek(); topComponent != component; topComponent = elStack.peek()) {
            pop(type, topComponent);
        }

        elStack.pop();
    }

    private static <T> ArrayDeque<T> getObjectStack(Class<T> type, Map<Object, Object> contextAttributes) {
        String keyName = CURRENT_STACK_KEY + ":" + type.getName();
        ArrayDeque<T> elStack = (ArrayDeque<T>) contextAttributes.get(keyName);

        if (elStack == null) {
            elStack = new ArrayDeque<>();
            contextAttributes.put(keyName, elStack);
        }
        return elStack;
    }

}
