/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ssoft.faces.state.cdi;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 *
 * @author Waldemar Kłaczyński
 * @param <T>
 */
public abstract class CdiProducer<T> implements Bean<T>, PassivationCapable, Serializable {
    
    /**
     * Serialization version
     */
    private static final long serialVersionUID = 1L;
    
    private String id = this.getClass().getName();
    private String name;
    private Class<?> beanClass = Object.class;
    private Set<Type> types = singleton(Object.class);
    private Set<Annotation> qualifiers = unmodifiableSet(asSet(new DefaultAnnotationLiteral(), new AnyAnnotationLiteral()));
    private Class<? extends Annotation> scope = Dependent.class;
    private Function<CreationalContext<T>, T> create;
    
    /**
     * Get the ID of this particular instantiation of the producer.
     * <p>
     * This is an implementation detail of CDI, where it wants to relocate
     * a particular producer in order to re-inject a value. This is typically
     * used in combination with passivation. Note that this is NOT about
     * the value we're producing, but about the producer itself.
     * 
     * @return the ID of this particular instantiation of the producer
     */
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Class<?> getBeanClass() {
       return beanClass;
    }
    
    @Override
    public Set<Type> getTypes() {
        return types;
    }
    
    /**
     * Get the default qualifier.
     *
     * @return the qualifiers, which in the default case only contains the Default
     */
    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }
    
    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }
    
    @Override
    public T create(CreationalContext<T> creationalContext) {
        return create.apply(creationalContext);
    }
    
    /**
     * Destroy the instance.
     *
     * <p>
     * Since most artifact that the sub classes are producing 
     * are artifacts that the JSF runtime really is
     * managing the destroy method here does not need to do anything.
     * </p>
     *
     * @param instance the instance.
     * @param creationalContext the creational context.
     */
    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
    }
    
    /**
     * Get the injection points.
     *
     * @return the injection points.
     */
    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return emptySet();
    }
    
    /**
     * Get the stereotypes.
     *
     * @return the stereotypes.
     */
    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return emptySet();
    }
    
    /**
     * Is this an alternative.
     *
     * @return false.
     */
    @Override
    public boolean isAlternative() {
        return false;
    }

    /**
     * Is this nullable.
     *
     * @return false.
     */
    @Override
    public boolean isNullable() {
        return false;
    }
    
    protected CdiProducer<T> name(String name) {
        this.name = name;
        return this;
    }
    
    protected CdiProducer<T> create(Function<CreationalContext<T>, T> create) {
        this.create = create;
        return this;
    }
    
    protected CdiProducer<T> beanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
        return this;
    }
    
    protected CdiProducer<T> types(Type... types) {
        this.types = asSet(types);
        return this;
    }
    
    protected CdiProducer<T> beanClassAndType(Class<?> beanClass) {
        beanClass(beanClass);
        types(beanClass);
        return this;
    }
    
    protected CdiProducer<T> qualifiers(Annotation... qualifiers) {
        this.qualifiers = asSet(qualifiers);
        return this;
    }
    
    
    protected CdiProducer<T> scope(Class<? extends Annotation> scope) {
        this.scope = scope;
        return this;
    }
    
    protected CdiProducer<T> addToId(Object object) {
        id = id + " " + object.toString();
        return this;
    }
    
    @SafeVarargs
    protected static <T> Set<T> asSet(T... a) {
        return new HashSet<>(asList(a));
    }
    
}
