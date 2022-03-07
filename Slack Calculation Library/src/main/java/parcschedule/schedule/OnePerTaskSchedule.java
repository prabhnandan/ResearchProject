package parcschedule.schedule;

import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.Slack;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

/**
 * A schedule where each task is to be executed once
 *
 * @param <V> Vertex class
 * @param <E> Edge class
 * @param <P> Processor class
 */
public abstract class OnePerTaskSchedule<V extends TaskVertex, E extends CommEdge<V>, P extends Proc> implements Schedule<V, E, P> {
    public final int STRING_OFFSET = 3;
    public final int Y_OFFSET = 25;
    public final int X_OFFSET = 20;

    /* (non-Javadoc)
     * @see schedule.Schedule#setFreeTasksObserver(schedule.FreeTasksObserver)
     */
    public abstract void setFreeTasksObserver(FreeTasksObserver<V> observer);

    public abstract int drt(V task);

    public abstract P enablingProc(V task);

    public abstract DrtInfo<P> drtAndEnablingProc(V task);

    public abstract int procFinishTime(P proc);

    public abstract int drtOnProc(V task, P proc);

    public abstract void put(V task, P proc, int startTime, boolean movable);

    public abstract void put(V task, TimeSlot<P> slot, boolean movable);

    public abstract void put(V task, P proc, int startTime, int insertionIndex, boolean movable);

    public abstract void unScheduleTask(V task);

    public abstract void relocate(V task, P newProc, int newStartTime, int insertionIndex, boolean movable);

    public abstract boolean isFinished();

    public abstract void checkValidity() throws Exception;

    public abstract Set<V> freeTasks();

    public abstract P taskProcAllocation(V task);

    public abstract int taskStartTime(V task);

    public abstract List<V> procTaskList(P proc);

    public abstract P earliestFinishingProc();

    public abstract int scheduleLength();

    public abstract boolean isScheduled(V task);


    @Override
    public void calculateSlack(StringBuilder sb) {
        ArrayList<Slack> allSlackTimes = new ArrayList<>();
        int totalSlack = 0;
        int totalSequentialTime = 0;

        TaskGraph<V, E> taskGraph = taskGraph();
        int scheduleLength = scheduleLength();

        for (P proc : system().processors()) {
            ArrayList<Slack> slackInProcessor = new ArrayList<>();

            List<V> procTaskList = procTaskList(proc);
            procTaskList.sort(new Comparator<V>() {
                @Override
                public int compare(V A, V B) {
                    int val = taskStartTime(A) - taskStartTime(B);
                    return val != 0 ? val : A.index() - B.index();
                }
            });
            for (V task : procTaskList) {
                totalSequentialTime += task.weight();
                Slack slack = new Slack(taskStartTime(task) + task.weight(), scheduleLength, proc.id(), task.name());
                int positionOnProc = procTaskList.indexOf(task);
                if (positionOnProc < procTaskList.size() - 1) {
                    slack.setEndTime(taskStartTime(procTaskList.get(positionOnProc + 1)));
                }
                if (slack.getStartTime() != slack.getEndTime()) {
                    for (E outEdge : taskGraph.outEdges(task)) {
                        if (taskProcAllocation(outEdge.to()) != taskProcAllocation(task)) {
                            int latestTaskEndTime = taskStartTime(outEdge.to()) - outEdge.weight();
                            if (latestTaskEndTime < slack.getEndTime()) slack.setEndTime(latestTaskEndTime);
                        }
                    }
                }
                if (slack.getEndTime() != slack.getStartTime()) slackInProcessor.add(slack);
            }

            slackInProcessor.sort(new Comparator<Slack>() {
                @Override
                public int compare(Slack s1, Slack s2) {
                    return s1.getStartTime().compareTo(s2.getStartTime());
                }
            });
            for (int slackIndex = 0; slackIndex < slackInProcessor.size() - 1; slackIndex++) {
                int slackEndTime = slackInProcessor.get(slackIndex).getEndTime();
                int nextSlackStartTime = slackInProcessor.get(slackIndex + 1).getStartTime();
                if (slackEndTime > nextSlackStartTime) {
                    slackInProcessor.get(slackIndex).setEndTime(slackInProcessor.get(slackIndex + 1).getEndTime());
                    slackInProcessor.remove(slackIndex + 1);
                    slackIndex--;
                }
            }
            for (int j = 0; j < slackInProcessor.size(); j++) {
                Slack slack = slackInProcessor.get(j);
                totalSlack += slack.getAmount();
            }
            allSlackTimes.addAll(slackInProcessor);
        }
        int totalScheduleTime = system().numProcs() * scheduleLength();
        int idleTime = totalScheduleTime - totalSlack - totalSequentialTime;
        System.out.println("Total processor time = " + totalScheduleTime);
        System.out.println("Total slack time = " + totalSlack + "(" + String.format("%.2f", (float) totalSlack * 100 / totalScheduleTime) + "%)");
        System.out.println();
        System.out.println("Total idle time = " + idleTime + "(" + String.format("%.2f", (float) idleTime * 100 / totalScheduleTime) + "%)");

        float avgSlackChunkSize = 0;
        if (allSlackTimes.size() != 0) avgSlackChunkSize = totalSlack / allSlackTimes.size();
        sb.append(totalSlack + ",");
        sb.append(String.format("%.2f", (float) totalSlack * 100 / totalScheduleTime) + ",");

        sb.append(idleTime + ",");
        sb.append(String.format("%.2f", (float) idleTime * 100 / totalScheduleTime) + ",");

        sb.append(avgSlackChunkSize + ",");
        if (avgSlackChunkSize == 0) sb.append(0);
        else sb.append(String.format("%.2f", (float) avgSlackChunkSize * 100 / totalSlack));
		drawGraph(allSlackTimes);
    }

    @Override
    public int slackTime() {
        int totalSlack = 0;
        TaskGraph<V, E> taskGraph = taskGraph();
        int scheduleLength = scheduleLength();

        for (V task : taskGraph.vertices()) {
            int alapStartTime = Integer.MAX_VALUE;
            P taskProc = taskProcAllocation(task);
            for (E outEdge : taskGraph.outEdges(task)) {
                V child = outEdge.to();
                int dataNeededTime = taskStartTime(child) - (taskProcAllocation(child) == taskProc ? 0 : outEdge.weight());
                alapStartTime = Math.min(alapStartTime, dataNeededTime - task.weight());
            }

            List<V> procTaskList = procTaskList(taskProc);
            int locationOnProc = Collections.binarySearch(procTaskList, task, new Comparator<V>() {

                @Override
                public int compare(V A, V B) {
                    int val = taskStartTime(A) - taskStartTime(B);
                    return val != 0 ? val : A.index() - B.index();
                }
            });

            if (locationOnProc < procTaskList(taskProc).size() - 1) {
                alapStartTime = Math.min(alapStartTime, taskStartTime(procTaskList.get(locationOnProc + 1)) - task.weight());
            } else {
                alapStartTime = Math.min(alapStartTime, scheduleLength);
            }

            int slack = alapStartTime - taskStartTime(task);
            if (alapStartTime == scheduleLength) slack -= task.weight();
            totalSlack += slack;
        }

        return totalSlack;
    }

    @Override
    public void removeTaskSchedule(ScheduledTask<V, P> scheduledTask) {
        unScheduleTask(scheduledTask.task());
    }

    @Override
    public List<ScheduledTask<V, P>> taskSchedulesOnProc(P proc) {
        return new TaskScheduleDecoratorList(procTaskList(proc));
    }

    @Override
    public List<ScheduledTask<V, P>> schedulesForTask(V task) {
        return new TaskScheduleDecoratorList(Arrays.asList(task));
    }

    @Override
    public List<ScheduledTask<V, P>> taskSchedules() {
        List<V> tasks = new ArrayList<>();
        for (V task : taskGraph().vertices()) {
            if (isScheduled(task)) tasks.add(task);
        }
        return new TaskScheduleDecoratorList(tasks);
    }

    public void drawGraph(ArrayList<Slack> allSlackTimes) {

        int width = (50 * system().numProcs()) + X_OFFSET;
        int height = (10 * scheduleLength()) + Y_OFFSET + STRING_OFFSET;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        makeScheduleImage(width, height, allSlackTimes, bufferedImage);
        JFrame frame = new JFrame();
        JPanel map = new JPanel();
        JScrollPane scroll = new JScrollPane(map);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        map.setLayout(new GridLayout());
        bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(new ImageIcon(bufferedImage));
        JScrollPane scrollpane = new JScrollPane(imageLabel);
        map.add(scrollpane);
        frame.add(map);
        frame.setSize(new Dimension(600, 800));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    public void makeScheduleImage(int width, int height, ArrayList<Slack> allSlackTimes, BufferedImage image) {
        Graphics graph = image.createGraphics();
        int xUnit = (width - X_OFFSET) / (system().numProcs());
        int yUnit = (height - Y_OFFSET) / scheduleLength();
        graph.setColor(Color.WHITE);
        graph.fillRect(0, 0, width, height);
        graph.setColor(Color.DARK_GRAY);
        graph.fillRect(X_OFFSET, Y_OFFSET, width - X_OFFSET, height - Y_OFFSET);
        for (P processor : system().processors()) {
            graph.drawString("P" + processor.id(), xUnit * Integer.valueOf(processor.id()) + xUnit / 2 + X_OFFSET, yUnit + STRING_OFFSET);
            for (V task : procTaskList(processor)) {
                int x = xUnit * Integer.valueOf(processor.id()) + X_OFFSET;
                int y = yUnit * taskStartTime(task) + Y_OFFSET;
                graph.setColor(Color.RED);
                graph.fillRect(x, y, xUnit, task.weight() * yUnit);
                graph.setColor(Color.BLACK);
                graph.drawString(task.name(), (x + (xUnit / 2)), y + (task.weight() * yUnit / 2) + STRING_OFFSET);
                graph.drawRect(x, y, xUnit, task.weight() * yUnit);
            }
        }
        for (Slack slack : allSlackTimes) {
            graph.setColor(Color.GREEN);
            int x = xUnit * Integer.valueOf(slack.getProcessor()) + X_OFFSET;
            int y = yUnit * slack.getStartTime() + Y_OFFSET;
            int weight = slack.getEndTime() - slack.getStartTime();
            graph.fillRect(x, y, xUnit, weight * yUnit);
            graph.setColor(Color.BLACK);
            graph.drawString(slack.getFromTask(), (x + (xUnit / 2)), y + (weight * yUnit) / 2 + STRING_OFFSET);
            graph.drawRect(x, y, xUnit, weight * yUnit);
        }

        yUnit = height / (scheduleLength());

        for (int i = 0; i < scheduleLength() + 1; i++) {
            int y = yUnit * i + Y_OFFSET;
            graph.drawString(i + " -", 0, y + STRING_OFFSET);
        }
    }

    /**
     * This class is used by OnePerTaskSchedule to implement the Schedule interface which allows duplication (whereas OnePerTaskSchedule does not have duplicated tasks).
     * The Schedule interface has methods requesting Lists of ScheduledTasks that represent duplicated instances of tasks. OnePerTaskSchedule does not deal with ScheduledTask objects,
     * but to implement the Schedule interface it uses this class to wrap lists of tasks and present them as ScheduledTasks.
     */
    class TaskScheduleDecoratorList implements List<ScheduledTask<V, P>> {
        List<V> tasks;

        public TaskScheduleDecoratorList(List<V> tasks) {
            this.tasks = tasks;
        }

        @Override
        public boolean contains(Object o) {
            try {
                @SuppressWarnings("unchecked")
                ScheduledTask<V, P> taskSchedule = (ScheduledTask<V, P>) o;
                V task = taskSchedule.task();
                return (tasks.contains(task) && taskSchedule.startTime() == taskStartTime(task) && taskSchedule.processor() == taskProcAllocation(task));
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!contains(o)) return false;
            }
            return true;
        }

        @Override
        public ScheduledTask<V, P> get(int index) {
            return new AdhocScheduledTask(tasks.get(index));
        }

        @SuppressWarnings("unchecked")
        @Override
        public int indexOf(Object o) {
            return contains(o) ? tasks.indexOf(((ScheduledTask<V, P>) o).task()) : -1;
        }

        @Override
        public boolean isEmpty() {
            return tasks.isEmpty();
        }

        @Override
        public Iterator<ScheduledTask<V, P>> iterator() {
            return new Iterator<ScheduledTask<V, P>>() {

                Iterator<V> underlying = tasks.iterator();

                @Override
                public boolean hasNext() {
                    return underlying.hasNext();
                }

                @Override
                public ScheduledTask<V, P> next() {
                    return new AdhocScheduledTask(underlying.next());
                }
            };
        }

        @Override
        public int size() {
            return tasks.size();
        }

        @Override
        public List<ScheduledTask<V, P>> subList(int fromIndex, int toIndex) {
            return new TaskScheduleDecoratorList(tasks.subList(fromIndex, toIndex));
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(ScheduledTask<V, P> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int arg0, ScheduledTask<V, P> arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends ScheduledTask<V, P>> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int arg0, Collection<? extends ScheduledTask<V, P>> arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<ScheduledTask<V, P>> listIterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<ScheduledTask<V, P>> listIterator(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledTask<V, P> remove(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledTask<V, P> set(int arg0, ScheduledTask<V, P> arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] arg0) {
            throw new UnsupportedOperationException();
        }

        class AdhocScheduledTask extends ScheduledTask<V, P> {
            public AdhocScheduledTask(V task) {
                super(task, taskProcAllocation(task), taskStartTime(task));
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) return false;
                try {
                    @SuppressWarnings("unchecked")
                    ScheduledTask<V, P> other = (ScheduledTask<V, P>) obj;
                    return (other.task().equals(this.task()) && other.processor().equals(this.processor()) && other.startTime() == this.startTime());
                } catch (ClassCastException e) {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return task.hashCode();
            }

            @Override
            public void setProcessor(P proc) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setStartTime(int startTime) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void reSchedule(P proc, int startTime) {
                throw new UnsupportedOperationException();
            }
        }
    }
}