package parcschedule.schedulers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import parcschedule.graphGenerator.GXLGraphGenerator;
import joptsimple.*;
import parcschedule.schedule.*;
import parcschedule.schedule.model.ClassicHomogeneousSystem;
import parcschedule.schedule.model.Proc;
import parcschedule.schedule.model.TargetSystem;
import parcschedule.schedulers.clusterSchedulers.ClusterMergers.ClusterMerger;
import parcschedule.schedulers.clusterSchedulers.ClusterMergers.GuidedLoadBalancingMerger;
import parcschedule.schedulers.clusterSchedulers.ClusterMergers.ListSchedulingMerger;
import parcschedule.schedulers.clusterSchedulers.ClusterScheduler;
import parcschedule.schedulers.clusterSchedulers.Clusterers.Clusterer;
import parcschedule.schedulers.clusterSchedulers.Clusterers.DcpClusterer;
import parcschedule.schedulers.clusterSchedulers.Clusterers.DscClusterer;
import parcschedule.schedulers.clusterSchedulers.Clusterers.EzClusterer;
import parcschedule.schedulers.clusterSchedulers.Clusterers.LcClusterer;
import parcschedule.schedulers.clusterSchedulers.Clustering.ClusteringConvert;
import parcschedule.schedulers.listSchedulers.ListScheduler;
import parcschedule.schedulers.listSchedulers.placementSchemes.AllChildrenLatestEstLookahead;
import parcschedule.schedulers.listSchedulers.placementSchemes.AllChildrenWeightedEstLookahead;
import parcschedule.schedulers.listSchedulers.placementSchemes.CriticalChildEstLookahead;
import parcschedule.schedulers.listSchedulers.placementSchemes.MinEst;
import parcschedule.schedulers.listSchedulers.placementSchemes.MinEstWithCriticalPathOnOneProc;
import parcschedule.schedulers.listSchedulers.placementSchemes.TaskPlacementScheme;
import parcschedule.schedulers.listSchedulers.priorities.BLevelMinusEstPriority;
import parcschedule.schedulers.listSchedulers.priorities.BLevelPriority;
import parcschedule.schedulers.listSchedulers.priorities.CPBasedBAndTLevelsPriority;
import parcschedule.schedulers.listSchedulers.priorities.CPBasedConstrainingParentsPriority;
import parcschedule.schedulers.listSchedulers.priorities.EarliestStartTimePriority;
import parcschedule.schedulers.listSchedulers.priorities.ModifiedCriticalPathPriority;
import parcschedule.schedulers.listSchedulers.priorities.TaskPrioritizingIterator;

public class SchedulerApp
{
	static List<TaskPrioritizingIterator<TaskVertex, Proc>> priorities;
	static List<TaskPlacementScheme<TaskVertex, Proc>> placementSchemes;
	static List<Clusterer<TaskVertex>> clusterers;
	static List<ClusterMerger<TaskVertex>> clusterMergers;

	static{
		priorities = Arrays.asList(
				new BLevelPriority<>(),
				new BLevelMinusEstPriority<>(false),
				new CPBasedBAndTLevelsPriority<>(),
				new CPBasedConstrainingParentsPriority<>(),
				new EarliestStartTimePriority<>(),
				new ModifiedCriticalPathPriority<>()
		);
		placementSchemes = Arrays.asList(
				new MinEst<>(),
				new AllChildrenLatestEstLookahead<>(),
				new AllChildrenWeightedEstLookahead<>(),
				new CriticalChildEstLookahead<>(),
				new MinEstWithCriticalPathOnOneProc<>()
		);

		clusterers = Arrays.asList(
				new DscClusterer<>(),
				new DcpClusterer<>(),
				//new DcpClusterer<>(true),
				new EzClusterer<>(EzClusterer.BLevelType.computation),
				new LcClusterer<>()
		);
		clusterMergers = Arrays.asList(
				new GuidedLoadBalancingMerger<>(false),
				// new ListLoadBalancingMerger<>(),
				new ListSchedulingMerger<>(false)
		);
	}


	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException
	{

		OptionParser parser = new OptionParser();
		parser.acceptsAll( Arrays.asList("file", "f"), "path to gxl file containing task graph to be scheduled").withRequiredArg().required().ofType( File.class );
		parser.acceptsAll( Arrays.asList("out", "o"), "location of output file").withRequiredArg().required().ofType( File.class );
		parser.acceptsAll( Arrays.asList("processors", "cores", "p"), "number of processing elements").withRequiredArg().ofType(Integer.class);

		parser.acceptsAll( Arrays.asList("priority", "ordering"), "task priority in list scheduling/intra-cluster ordering in cluster scheduling"
				+ listArgs(priorities, "Choices for list scheduling") + listArgs(EnumSet.allOf(ClusterScheduler.OrderingScheme.class), "Choices for cluster scheduling"))
				.withRequiredArg();

		parser.accepts("placement", "task placement scheme for list scheduling" + listArgs(placementSchemes)).withRequiredArg();
		parser.accepts("no-insertion", "disables insertion for list-scheduling which is used by default").availableIf("placement");

		parser.accepts("clusterer", "clustering algorithm for cluster scheduling" + listArgs(clusterers)).withRequiredArg();
		parser.acceptsAll( Arrays.asList("cluster-merger", "merger"), "cluster-merging algorithm for cluster scheduling" + listArgs(clusterMergers)).withRequiredArg();

		parser.acceptsAll( Arrays.asList("h", "help", "?"), "show help" ).forHelp();

		OptionSet options = parser.parse(args);

		if (options.has("help"))
		{
			parser.printHelpOn(System.out);
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(options.valueOf("priority")+",");
		sb.append(options.valueOf("placement")+",");
		sb.append(options.valueOf("clusterer")+",");
		sb.append(options.valueOf("merger")+",");
		sb.append(options.valueOf("ordering")+",");

		OnePerTaskSchedule schedule = makeScheduler(options).schedule( GXLGraphGenerator.generate((File) options.valueOf("file"), sb),
				new ClassicHomogeneousSystem(options.has("p") ? (int) options.valueOf("p") : 0));
		IOUtils.writeScheduleToGXL((File) options.valueOf("out"), schedule /*(OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc>) needs this cast for now*/
		,sb);
		schedule.calculateSlack(sb);

		sb.append("\n");
		File CSVFile = new File("results.csv");
		try (FileWriter writer = new FileWriter(CSVFile,true)) {
			writer.append(sb.toString());
			writer.flush();
			writer.close();
		}
	}

	public static Schedule<TaskVertex, CommEdge<TaskVertex>, Proc> runAll(TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph, int numProcs){

		Schedule<TaskVertex, CommEdge<TaskVertex>, Proc> best = null;

		for(TaskPrioritizingIterator<TaskVertex, Proc> priority: priorities){
			for(TaskPlacementScheme<TaskVertex, Proc> placement: placementSchemes){
				Scheduler<TaskVertex, CommEdge<TaskVertex>, Proc> scheduler = new ListScheduler<>(placement, priority);
				Schedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule = scheduler.schedule(graph, new ClassicHomogeneousSystem(numProcs));
				if(best == null || schedule.scheduleLength() < best.scheduleLength()){
					best = schedule;
				}
			}
		}

		for(Clusterer<TaskVertex> clusterer: clusterers){
			for(ClusterMerger<TaskVertex> merger: clusterMergers){
				Scheduler<TaskVertex, CommEdge<TaskVertex>, Proc> scheduler = new ClusterScheduler<TaskVertex, CommEdge<TaskVertex>, Proc>(clusterer, merger, ClusterScheduler.OrderingScheme.blevel);
				Schedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule = scheduler.schedule(graph, new ClassicHomogeneousSystem(numProcs));
				if(best == null || schedule.scheduleLength() < best.scheduleLength()){
					best = schedule;
				}
			}
		}

		return best;
	}

	public static Scheduler<TaskVertex, CommEdge<TaskVertex>, Proc> makeScheduler(OptionSet options)
	{
		
		/*if (!options.has("placement") && !options.has("clusterer") && !options.has("cluster-merger")
			|| options.has("placement") && (options.has("clusterer") || options.has("cluster-merger"))
			|| (options.has("clusterer") ^ options.has("cluster-merger")))
		{
			error("invalid composition");
		}*/

		if (options.has("placement"))
		{
			TaskPlacementScheme<TaskVertex, Proc> placementScheme = null;
			TaskPrioritizingIterator<TaskVertex, Proc> priority = null;

			for (TaskPlacementScheme<TaskVertex, Proc> scheme : placementSchemes)
			{
				if (scheme.code().equalsIgnoreCase((String) options.valueOf("placement")))
				{
					placementScheme = scheme;
					break;
				}
			}
			for (TaskPrioritizingIterator<TaskVertex, Proc> scheme : priorities)
			{
				if (scheme.code().equalsIgnoreCase((String) options.valueOf("priority")))
				{
					priority = scheme;
					break;
				}
			}

			if (placementScheme == null) error("unrecognized placement scheme");
			if (priority == null) error("unrecognized priority scheme");

			placementScheme.setInsertion(! options.has("no-insertion"));

			return new ListScheduler<>(placementScheme, priority);
		}

		if (options.has("clusterer") && options.has("cluster-merger"))
		{
			Clusterer<TaskVertex> clusterer = null;
			ClusterMerger<TaskVertex> clusterMerger = null;
			ClusterScheduler.OrderingScheme orderingScheme = null;

			for (Clusterer<TaskVertex> algorithm : clusterers)
			{
				if (algorithm.code().equalsIgnoreCase((String) options.valueOf("clusterer")))
				{
					clusterer = algorithm;
					break;
				}
			}
			for (ClusterMerger<TaskVertex> algorithm : clusterMergers)
			{
				if (algorithm.code().equalsIgnoreCase((String) options.valueOf("cluster-merger")))
				{
					clusterMerger = algorithm;
					break;
				}
			}
			for (ClusterScheduler.OrderingScheme scheme : ClusterScheduler.OrderingScheme.values())
			{
				if (scheme.name().equalsIgnoreCase((String) options.valueOf("ordering")))
				{
					orderingScheme = scheme;
					break;
				}
			}

			if (clusterer == null) error("unrecognized clusterer");
			if (clusterMerger == null) error("unrecognized cluster-merger");
			if (orderingScheme == null) error("unrecognized ordering scheme (for cluster scheduling)");

			return new ClusterScheduler<>(clusterer, clusterMerger, orderingScheme);
		}

		else if (options.has("clusterer"))
		{
			return new Scheduler<TaskVertex, CommEdge<TaskVertex>, Proc>() {

				@Override
				public OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule(
						TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph, TargetSystem<Proc> system) {

					Clusterer<TaskVertex> clusterer = null;
					for (Clusterer<TaskVertex> algorithm : clusterers)
					{
						if (algorithm.code().equalsIgnoreCase((String) options.valueOf("clusterer")))
						{
							clusterer = algorithm;
							break;
						}
					}
					return ClusteringConvert.toSchedule(clusterer.cluster(taskGraph), taskGraph);
				}

				@Override
				public String description() { return null; }
			};
		}

		error("no action");
		throw new UnsupportedOperationException();
	}

	static String listArgs(List<? extends SchedulerComponent> schedulerComponents)
	{
		return listArgs(schedulerComponents, "Choices");
	}

	static String listArgs(List<? extends SchedulerComponent> schedulerComponents, String extra)
	{
		StringBuilder string = new StringBuilder(". " + extra + ": ");
		int i = 1;
		for (SchedulerComponent component : schedulerComponents)
		{
			string.append(String.format("\"%s\" - %s", component.code(), component.description()));
			if (i++ < schedulerComponents.size()) string.append(", ");
		}
		return string.toString();
	}

	static String listArgs(EnumSet<?> enumArgs, String extra)
	{
		StringBuilder string = new StringBuilder(extra + ": ");
		int i = 1;
		for (Enum<?> arg : enumArgs)
		{
			string.append(String.format("\"%s\"", arg.name()));
			if (i++ < enumArgs.size()) string.append(", ");
		}
		return string.toString();
	}

	static void error(String message)
	{
		System.out.println(message);
		System.exit(0);
	}

	static int toInt(Object o)
	{
		return o == null ? 0 : (int) o;
	}

}
