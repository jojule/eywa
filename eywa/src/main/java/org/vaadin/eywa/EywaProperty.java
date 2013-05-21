package org.vaadin.eywa;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.data.Property;
import com.vaadin.server.ClientConnector;
import com.vaadin.ui.UI;

public class EywaProperty<T> implements Property<T>,
        Property.ValueChangeNotifier, Property.ReadOnlyStatusChangeNotifier {

    private String id;
    boolean readOnly;
    final Shared<T> sharedInstance;
    static private ConcurrentHashMap<String, Shared<?>> shared = new ConcurrentHashMap<String, Shared<?>>();
    static private ExecutorService service = Executors
            .newSingleThreadExecutor();
    LinkedList<ReadOnlyStatusChangeListener> readOnlyStatusListeners;
    LinkedList<ValueChangeListener> valueChangeListeners;

    private static class Shared<T> {
        final AtomicReference<T> value = new AtomicReference<T>();
        final Class<T> type;
        // ConcurrentHashSet<X> by wrapping ConcurrentHashMap<X, Boolean>
        final Set<WeakReference<EywaProperty<T>>> listeners = Collections
                .newSetFromMap(new ConcurrentHashMap<WeakReference<EywaProperty<T>>, Boolean>());

        Shared(Class<T> type) {
            this.type = type;
        }

    }

    public EywaProperty(String globalId, Class<T> type) {
        this.id = globalId;

        Shared<?> s = shared.get(globalId);
        if (s == null) {
            // Create instance if none found
            Shared<T> newShared = new Shared<T>(type);

            // Use existing value if it suddenly appeared...
            s = shared.putIfAbsent(globalId, newShared);
            if (s == null) {
                // ...else keep using the newly created instance
                s = newShared;
            }
        }

        if (s.type != type) {
            throw new IllegalArgumentException("Type of the '" + id
                    + "' property is already set to be " + s.type.getName());
        }

        // Keep a reference to avoid doing lookups all the time
        sharedInstance = (Shared<T>) s;
    }

    @Override
    public T getValue() {
        return sharedInstance.value.get();
    }

    @Override
    public void setValue(T newValue) {
        LinkedList<EywaProperty<T>> propertiesToNotify = null;

        /*
         * Using atomic access instead of synchronizing the entire block means
         * that a listener might get added or removed between the instant when
         * the value changes and when the listeners set is accessed. This should
         * not change the semantics in any way as the actual events are fired
         * asynchronously.
         */
        T oldValue = sharedInstance.value.getAndSet(newValue);
        if (oldValue != newValue) {
            notifyOwnListeners();
            propertiesToNotify = new LinkedList<EywaProperty<T>>();
            LinkedList<WeakReference<EywaProperty<T>>> deadListenerReferences = null;

            // ConcurrentHashMap iteration uses a snapshot based on the state at
            // "some point at or since the creation of the iterator"
            for (WeakReference<EywaProperty<T>> r : sharedInstance.listeners) {
                EywaProperty<T> p = r.get();
                if (p == null) {
                    if (deadListenerReferences == null) {
                        deadListenerReferences = new LinkedList<WeakReference<EywaProperty<T>>>();
                    }
                    deadListenerReferences.add(r);
                } else if (p != this) {
                    propertiesToNotify.add(p);
                }
            }

            if (deadListenerReferences != null) {
                for (WeakReference<EywaProperty<T>> deadReference : deadListenerReferences) {
                    sharedInstance.listeners.remove(deadReference);
                }
            }
        }

        if (propertiesToNotify != null) {
            for (EywaProperty<T> p : propertiesToNotify) {
                p.notifyForeignListeners(UI.getCurrent());
            }
        }
    }

    private void notifyOwnListeners() {
        if (valueChangeListeners != null) {
            ValueChangeEvent event = new ValueChangeEvent() {
                @Override
                public Property getProperty() {
                    return EywaProperty.this;
                }
            };
            for (ValueChangeListener listener : valueChangeListeners) {
                listener.valueChange(event);
            }
        }
    }

    private void notifyForeignListeners(UI currentUI) {
        // TODO valueChangeListeners is accessed by other thread without locking
        if (valueChangeListeners != null) {
            final ValueChangeEvent event = new ValueChangeEvent() {
                @Override
                public Property getProperty() {
                    return EywaProperty.this;
                }
            };
            for (final ValueChangeListener listener : valueChangeListeners) {
                if (listener instanceof ClientConnector) {
                    final UI listenerUI = ((ClientConnector) listener).getUI();
                    if (currentUI != null
                            && listenerUI != null
                            && listenerUI.getSession() == currentUI
                                    .getSession()) {
                        // TODO Can call listener with UI.getCurrent() pointing
                        // to another UI in the same session
                        listener.valueChange(event);
                    } else {
                        service.execute(new Runnable() {
                            @Override
                            public void run() {
                                // TODO listenerUI can be null
                                listenerUI.access(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.valueChange(event);
                                    }
                                });
                            }
                        });
                    }
                } else {
                    service.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.valueChange(event);
                        }
                    });
                }
            }
        }
    }

    @Override
    public Class<? extends T> getType() {
        // Can read final field without locking
        return sharedInstance.type;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean newStatus) {
        if (readOnly != newStatus) {
            readOnly = newStatus;
            if (readOnlyStatusListeners != null) {
                ReadOnlyStatusChangeEvent event = new ReadOnlyStatusChangeEvent() {
                    @Override
                    public Property getProperty() {
                        return EywaProperty.this;
                    }
                };
                for (ReadOnlyStatusChangeListener listener : readOnlyStatusListeners) {
                    listener.readOnlyStatusChange(event);
                }
            }
        }
    }

    @Override
    public void addReadOnlyStatusChangeListener(
            ReadOnlyStatusChangeListener listener) {
        if (listener != null) {
            if (readOnlyStatusListeners == null) {
                readOnlyStatusListeners = new LinkedList<ReadOnlyStatusChangeListener>();
            }
            if (!readOnlyStatusListeners.contains(listener)) {
                readOnlyStatusListeners.add(listener);
            }
        }
    }

    @Override
    @Deprecated
    public void addListener(ReadOnlyStatusChangeListener listener) {
        addReadOnlyStatusChangeListener(listener);
    }

    @Override
    public void removeReadOnlyStatusChangeListener(
            ReadOnlyStatusChangeListener listener) {
        if (listener != null && readOnlyStatusListeners != null) {
            readOnlyStatusListeners.remove(listener);
            if (readOnlyStatusListeners.isEmpty()) {
                readOnlyStatusListeners = null;
            }
        }
    }

    @Override
    @Deprecated
    public void removeListener(ReadOnlyStatusChangeListener listener) {
        removeReadOnlyStatusChangeListener(listener);
    }

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {
        if (listener != null) {
            if (valueChangeListeners == null) {
                valueChangeListeners = new LinkedList<ValueChangeListener>();
                sharedInstance.listeners
                        .add(new WeakReference<EywaProperty<T>>(this));
            }
            if (!valueChangeListeners.contains(listener)) {
                valueChangeListeners.add(listener);
            }
        }
    }

    @Override
    @Deprecated
    public void addListener(ValueChangeListener listener) {
        addValueChangeListener(listener);
    }

    @Override
    public void removeValueChangeListener(ValueChangeListener listener) {
        if (listener != null && valueChangeListeners != null) {
            valueChangeListeners.remove(listener);
            if (valueChangeListeners.isEmpty()) {
                valueChangeListeners = null;
                /*
                 * Not removing reference from sharedInstance.listeners, because
                 * finding the right WeakReference requires iterating or using a
                 * Map with a key unique for each instance. It will be cleaned
                 * out later on
                 */
            }
        }
    }

    @Override
    @Deprecated
    public void removeListener(ValueChangeListener listener) {
        removeValueChangeListener(listener);
    }

}
