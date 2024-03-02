/* Implement this class. */
/* Implement this class. */

import java.util.List;

public class MyDispatcher extends Dispatcher {

    int roundRobinLastSent = 0;
    long minNrTasks = Integer.MAX_VALUE;
    long minWorkLeft = Integer.MAX_VALUE;
    public MyDispatcher(SchedulingAlgorithm algorithm, List<Host> hosts) {
        super(algorithm, hosts);
    }

    @Override
    public void addTask(Task task) {
        synchronized (MyDispatcher.class) {
            if (algorithm == SchedulingAlgorithm.ROUND_ROBIN) {
                int sendTo = (roundRobinLastSent + 1) % hosts.size();
                hosts.get(sendTo).addTask(task);
                roundRobinLastSent = sendTo;
            }
            else if (algorithm == SchedulingAlgorithm.SHORTEST_QUEUE) {
                minNrTasks = hosts.get(0).getQueueSize();
                int idx = 0;
                for (int i = 1; i < hosts.size(); i++) {
                    if (hosts.get(i).getQueueSize() < minNrTasks) {
                        minNrTasks = hosts.get(i).getQueueSize();
                        idx = i;
                    }
                }
                hosts.get(idx).addTask(task);
            }
            else if (algorithm == SchedulingAlgorithm.SIZE_INTERVAL_TASK_ASSIGNMENT) {
                if (task.getType() == TaskType.SHORT) {
                    hosts.get(0).addTask(task);
                } else if (task.getType() == TaskType.MEDIUM) {
                    hosts.get(1).addTask(task);
                } else if (task.getType() == TaskType.LONG) {
                    hosts.get(2).addTask(task);
                }
            }
            else if (algorithm == SchedulingAlgorithm.LEAST_WORK_LEFT) {
                minWorkLeft = hosts.get(0).getWorkLeft();
                int idx = 0;
                for (int i = 1; i < hosts.size(); i++) {
                    if (hosts.get(i).getWorkLeft() < minWorkLeft) {
                        minWorkLeft = hosts.get(i).getWorkLeft();
                        idx = i;
                    }
                }
                hosts.get(idx).addTask(task);
            }
        }
    }

}