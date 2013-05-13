package org.vaadin.eywa;

import java.util.LinkedList;

class EywaService {

    private static EywaService singleton;
    private LinkedList<Runnable> todo = new LinkedList<Runnable>();
    private Object bell = new Object();
    private Thread serviceThread = new Thread() {

        @Override
        public void run() {
            while (true) {

                if (todo.isEmpty()) {
                    try {
                        synchronized (bell) {
                            bell.wait();
                        }
                    } catch (InterruptedException e) {
                    }
                }

                LinkedList<Runnable> tasks = new LinkedList<Runnable>();
                synchronized (todo) {
                    tasks.addAll(todo);
                    todo.clear();
                }

                for (Runnable task : tasks) {
                    task.run();
                }
            }
        }

    };

    static EywaService get() {
        if (singleton == null) {
            singleton = new EywaService();
            singleton.serviceThread.start();
        }
        return singleton;
    }

    void run(Runnable task) {
        synchronized (todo) {
            todo.add(task);
        }
        synchronized (bell) {
            bell.notify();
        }
    }

}
