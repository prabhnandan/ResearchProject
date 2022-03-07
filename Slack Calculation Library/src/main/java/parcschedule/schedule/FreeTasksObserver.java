package parcschedule.schedule;

import java.util.Collection;

public interface FreeTasksObserver<V extends TaskVertex>
{
	/**
	 * Receive information about new free tasks
	 */
	void handleFreeTasksAdded(Collection<V> addedTasks);

	/**
	 * Receive information about tasks removed from free tasks pool
	 */
	void handleFreeTasksRemoved(Collection<V> removedTasks);
}
