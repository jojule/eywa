package org.vaadin.eywa;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.vaadin.data.Property;
import com.vaadin.server.ClientConnector;
import com.vaadin.ui.UI;

public class EywaProperty<T> implements Property<T>,
        Property.ValueChangeNotifier, Property.ReadOnlyStatusChangeNotifier {

    private String id;
    boolean readOnly;
    static private HashMap<String, Shared> shared = new HashMap<String, Shared>();
    LinkedList<ReadOnlyStatusChangeListener> readOnlyStatusListeners;
    LinkedList<ValueChangeListener> valueChangeListeners;

    private static class Shared<T> {
        T value;
        Class<T> type;
        HashSet<WeakReference<EywaProperty<T>>> listeners = new HashSet<WeakReference<EywaProperty<T>>>();
    }

    public EywaProperty(String globalId, Class<T> type) {
        this.id = globalId;
        synchronized (shared) {
            Shared<T> s = getShared(id);
            if (s != null) {
                if (s.type != type) {
                    throw new IllegalArgumentException("Type of the '" + id
                            + "' property is already set to be "
                            + s.type.getName());
                }
            } else {
                s = new Shared<T>();
                s.type = type;
                shared.put(id, s);
            }
        }
    }

    @Override
    public T getValue() {
        synchronized (shared) {
            return getShared(id).value;
        }
    }

    @Override
    public void setValue(T newValue) {
        LinkedList<EywaProperty<T>> propertiesToNotify = null;
        synchronized (shared) {
            T oldValue = getShared(id).value;
            if (oldValue != newValue) {
                Shared<T> shared = getShared(id);
                shared.value = newValue;
                notifyOwnListeners();
                propertiesToNotify = new LinkedList<EywaProperty<T>>();
                LinkedList<WeakReference<EywaProperty<T>>> deadListenerReferences = null;
                for (WeakReference<EywaProperty<T>> r : shared.listeners) {
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
                        listener.valueChange(event);
                    } else {
                        EywaService.get().run(new Runnable() {
                            @Override
                            public void run() {
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
                    EywaService.get().run(new Runnable() {
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
        synchronized (shared) {
            return getShared(id).type;
        }

    }

    /* This should only be called from inside synchronized block */
    private Shared<T> getShared(String id) {
        return shared.get(id);
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
                synchronized (shared) {
                    getShared(id).listeners
                            .add(new WeakReference<EywaProperty<T>>(this));
                }
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
            }
        }
    }

    @Override
    @Deprecated
    public void removeListener(ValueChangeListener listener) {
        removeValueChangeListener(listener);
    }

}
