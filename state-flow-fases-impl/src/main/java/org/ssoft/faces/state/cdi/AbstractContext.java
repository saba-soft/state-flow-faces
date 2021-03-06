/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state.cdi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 *
 * @author Waldemar Kłaczyński
 */
public abstract class AbstractContext implements Context {

    /**
     * Whether the Context is for a passivating scope.
     */
    private final boolean passivatingScope;

    protected AbstractContext(BeanManager beanManager) {
        passivatingScope = beanManager.isPassivatingScope(getScope());
    }

    /**
     * An implementation has to return the underlying storage which contains the
     * items held in the Context.
     *
     * @param contextual
     * @param createIfNotExist whether a ContextualStorage shall get created if
     * it doesn't yet exist.
     * @return the underlying storage
     */
    protected abstract ContextualStorage getContextualStorage(Contextual<?> contextual, boolean createIfNotExist);

    protected List<ContextualStorage> getActiveContextualStorages() {
        List<ContextualStorage> result = new ArrayList<>();
        result.add(getContextualStorage(null, false));
        return result;
    }

    /**
     * @return whether the served scope is a passivating scope
     */
    public boolean isPassivatingScope() {
        return passivatingScope;
    }

    @Override
    public <T> T get(Contextual<T> bean) {
        checkActive();

        ContextualStorage storage = getContextualStorage(bean, false);
        if (storage == null) {
            return null;
        }

        Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));
        if (contextualInstanceInfo == null) {
            return null;
        }

        return (T) contextualInstanceInfo.getContextualInstance();
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> creationalContext) {
        if (creationalContext == null) {
            return get(bean);
        }

        checkActive();

        if (passivatingScope) {
            if (!(bean instanceof PassivationCapable)) {
                throw new IllegalStateException(bean.toString()
                        + " doesn't implement " + PassivationCapable.class.getName());
            }
        }

        ContextualStorage storage = getContextualStorage(bean, true);

        Map<Object, ContextualInstanceInfo<?>> contextMap = storage.getStorage();
        ContextualInstanceInfo<?> contextualInstanceInfo = contextMap.get(storage.getBeanKey(bean));

        if (contextualInstanceInfo != null) {
            @SuppressWarnings("unchecked")
            final T instance = (T) contextualInstanceInfo.getContextualInstance();

            if (instance != null) {
                return instance;
            }
        }

        return storage.createContextualInstance(bean, creationalContext);
    }

    /**
     * Destroy the Contextual Instance of the given Bean.
     *
     * @param bean dictates which bean shall get cleaned up
     * @return <code>true</code> if the bean was destroyed, <code>false</code>
     * if there was no such bean.
     */
    public boolean destroy(Contextual bean) {
        ContextualStorage storage = getContextualStorage(bean, false);
        if (storage == null) {
            return false;
        }

        ContextualInstanceInfo<?> contextualInstanceInfo = storage.getStorage().remove(storage.getBeanKey(bean));

        if (contextualInstanceInfo == null) {
            return false;
        }

        destroyBean(bean, contextualInstanceInfo);

        return true;
    }

    /**
     * destroys all the Contextual Instances in the Storage returned by
     * {@link #getContextualStorage(Contextual, boolean)}.
     */
    public void destroyAllActive() {
        List<ContextualStorage> storages = getActiveContextualStorages();
        if (storages == null) {
            return;
        }

        for (ContextualStorage storage : storages) {
            if (storage != null) {
                destroyAllActive(storage);
            }
        }
    }

    /**
     * Destroys all the Contextual Instances in the specified ContextualStorage.
     * This is a static method to allow various holder objects to cleanup
     * properly in &#064;PreDestroy.
     * @param storage
     * @return 
     */
    public static Map<Object, ContextualInstanceInfo<?>> destroyAllActive(ContextualStorage storage) {
        //drop all entries in the storage before starting with destroying the original entries
        Map<Object, ContextualInstanceInfo<?>> contextMap = new HashMap<>(storage.getStorage());
        storage.getStorage().clear();

        for (Map.Entry<Object, ContextualInstanceInfo<?>> entry : contextMap.entrySet()) {
            Contextual bean = storage.getBean(entry.getKey());

            ContextualInstanceInfo<?> contextualInstanceInfo = entry.getValue();
            destroyBean(bean, contextualInstanceInfo);
        }
        return contextMap;
    }

    public static void destroyBean(Contextual bean, ContextualInstanceInfo<?> contextualInstanceInfo) {
        bean.destroy(contextualInstanceInfo.getContextualInstance(), contextualInstanceInfo.getCreationalContext());
    }

    /**
     * Make sure that the Context is really active.
     *
     * @throws ContextNotActiveException if there is no active Context for the
     * current Thread.
     */
    protected void checkActive() {
        if (!isActive()) {
            throw new ContextNotActiveException("CDI context with scope annotation @"
                    + getScope().getName() + " is not active with respect to the current thread");
        }
    }

}
