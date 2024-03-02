/* Implement this class. */

import java.util.concurrent.PriorityBlockingQueue;

public class MyHost extends Host {

    private final Object lock = new Object(); 

    private boolean running = true;
    private boolean gotSubstitute = false;
    private long start_time = 0;


    private Task currentRunningTask = null;
    private Task substituteTask = null;
    public PriorityBlockingQueue<Task> queueTasks = new PriorityBlockingQueue<>();
    public PriorityBlockingQueue<Task> pausedTasks = new PriorityBlockingQueue<>();

    @Override
    public void run() {
        while (running) {
            synchronized (lock) {
                try {
                    if (currentRunningTask != null) {
                        start_time = System.currentTimeMillis();
                        if (currentRunningTask.getLeft() > 0) {
                            lock.wait(currentRunningTask.getLeft());
                        }

                        if (gotSubstitute) {
                            currentRunningTask.setLeft(currentRunningTask.getDuration() - (substituteTask.getStart() * 1000L - currentRunningTask.getStart() * 1000L));
                            pausedTasks.add(currentRunningTask);
                            currentRunningTask = substituteTask;
                            gotSubstitute = false;
                            continue;
                        }

                        currentRunningTask.finish();

                        if (!queueTasks.isEmpty() || !pausedTasks.isEmpty()) {
                            currentRunningTask = getNextTask(queueTasks, pausedTasks, currentRunningTask);
                        } else {
                            currentRunningTask = null;
                        }

                    }
                    else {
                        lock.wait();
                        if (!queueTasks.isEmpty()) {
                            currentRunningTask = queueTasks.take();
                        }
                    }


                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                 
                }
            }
        }
    }

    public Task getNextTask(PriorityBlockingQueue<Task> queueTasks, PriorityBlockingQueue<Task> pausedTasks, Task lastRanTask) {
        Task toReturn = null;

        int biggestPriority = 0;
        int idx = -1;
        int i = 0;

        for (Task task : queueTasks) {
            if (task.getStart() < lastRanTask.getStart() + lastRanTask.getDuration()) {
                if (task.getPriority() > biggestPriority) {
                    biggestPriority = task.getPriority();
                    toReturn = task;
                    idx = i;
                }
            }
            i += 1;
        }

        int biggestPausedPriority = 0;
        int idxPaused = -1;
        int j = 0;

        for (Task task : pausedTasks) {
            if (task.getPriority() >= biggestPriority && task.getPriority() > biggestPausedPriority) {
                biggestPausedPriority = task.getPriority();
                toReturn = task;
                idxPaused = j;
            }
            j += 1;
        }

        if (idxPaused != -1) {
            if (idxPaused != 0) {
                pausedTasks.remove(toReturn);
                return toReturn;
            } else {
                try {
                    return pausedTasks.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            if (idx != 0) {
                queueTasks.remove(toReturn);
                return toReturn;
            } else {
                try {
                    return queueTasks.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    @Override
    public void addTask(Task task) {
        synchronized (lock) {
            if (currentRunningTask != null && task.getPriority() > currentRunningTask.getPriority() && currentRunningTask.isPreemptible()) {
                lock.notify();
                gotSubstitute = true;
                substituteTask = task;
                return;
            }

            queueTasks.put(task);

            if (currentRunningTask == null) {
                lock.notify();
            }
        }
    }

    @Override
    public int getQueueSize() {
        int queueSize = queueTasks.size();
        queueSize += pausedTasks.size();

        if (currentRunningTask != null) {
            queueSize += 1;
        }
        if (substituteTask != null) {
            queueSize += 1;
        }

        return queueSize;
    }

    @Override
    public long getWorkLeft() {
        long workLeft = 0;
        for (Task task : queueTasks) {
            workLeft += task.getLeft();
        }

        for (Task task : pausedTasks) {
            workLeft += task.getLeft();
        }

        if (currentRunningTask != null) {
            long end_time = System.currentTimeMillis();
            workLeft += currentRunningTask.getDuration() - (end_time - start_time);
        }

        workLeft = Math.round(workLeft / 1000.0f) * 1000L;
        return workLeft;
    }

    @Override
    public void shutdown() {
        synchronized (lock) {
            lock.notify();
            this.running = false;
        }
    }
}
