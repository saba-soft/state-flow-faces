/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state.el;

import java.beans.FeatureDescriptor;
import java.io.Serializable;
import java.util.Iterator;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.faces.state.FlowContext;
import javax.faces.state.StateFlowExecutor;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class FlowELResolver extends ELResolver implements Serializable {

    public static final String STATE_RESULT_NAME = "result";

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        Object result = null;
        if (null == property) {
            String message = " base " + base + " property " + property;
            throw new PropertyNotFoundException(message);
        }
        if (null == base) {
            if (STATE_RESULT_NAME.equals(property.toString())) {
                context.setPropertyResolved(true);
                result = getResultParams(context);
            } else {
                FlowContext ctx = (FlowContext) context.getContext(FlowContext.class);
                if (ctx != null && ctx.has(property.toString())) {
                    Object value = ctx.get(property.toString());
                    if (value != null) {
                        context.setPropertyResolved(true);
                        result = value;
                    }
                }
            }
        } else if (base instanceof ResultParams) {
            context.setPropertyResolved(true);
            ResultParams scope = (ResultParams) base;
            result = scope.get(property.toString());
        }
        return result;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        Class result = null;
        if (null == property) {
            String message = " base " + base + " property " + property;
            throw new PropertyNotFoundException(message);
        }
        if (null == base) {
            if (STATE_RESULT_NAME.equals(property.toString())) {
                result = ResultParams.class;
            } else {
                FlowContext ctx = (FlowContext) context.getContext(FlowContext.class);
                if (ctx != null && ctx.has(property.toString())) {
                    Object value = ctx.get(property.toString());
                    if (value != null) {
                        context.setPropertyResolved(true);
                        result = value.getClass();
                    }
                }
            }
        } else if (base instanceof ResultParams) {
            context.setPropertyResolved(true);
            ResultParams scope = (ResultParams) base;
            Object value = scope.get(property.toString());
            if (value != null) {
                result = value.getClass();
            }
        }
        return result;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        if (null == property) {
            String message = " base " + base + " property " + property;
            throw new PropertyNotFoundException(message);
        }
        if (null == base) {
            FlowContext ctx = (FlowContext) context.getContext(FlowContext.class);
            if (ctx != null && ctx.has(property.toString())) {
                Object old = ctx.get(property.toString());
                if (old != null) {
                    context.setPropertyResolved(true);
                    ctx.set(property.toString(), value);
                }
            }
        } else if (base instanceof ResultParams) {
            context.setPropertyResolved(true);
            String message = "Read Only Property";
            message = message + " base " + base + " property " + property;
            throw new PropertyNotWritableException(message);
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        boolean result = false;
        if (null == base) {
            if (property.toString().equals(STATE_RESULT_NAME)) {
                context.setPropertyResolved(true);
                result = true;
            } else {
                FlowContext ctx = (FlowContext) context.getContext(FlowContext.class);
                if (ctx != null && ctx.has(property.toString())) {
                    context.setPropertyResolved(true);
                    result = false;
                }
            }
        } else if (base instanceof ResultParams) {
            context.setPropertyResolved(true);
            result = true;
        }
        return result;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

    private ResultParams getResultParams(ELContext context) {
        ResultParams attrScope = null;
        StateFlowExecutor executor = (StateFlowExecutor) context.getContext(StateFlowExecutor.class);
        if (executor != null) {
            FlowContext ctx = (FlowContext) context.getContext(FlowContext.class);
            if (ctx != null) {
                FlowContext result = (FlowContext) ctx.get("__@result@__");
                if (result != null) {
                    attrScope = new ResultParams(result);
                }
            }
        }
        return attrScope;
    }

    private static class ResultParams implements Serializable {

        private final FlowContext ctx;

        public ResultParams(FlowContext ctx) {
            this.ctx = ctx;
        }

        public FlowContext getCtx() {
            return ctx;
        }

        public Object get(String name) {
            return ctx.get(name);
        }

        public void set(String name, Object value) {
            ctx.setLocal(name, value);
        }

        public boolean has(String name) {
            return ctx.has(name);
        }
    }

}
