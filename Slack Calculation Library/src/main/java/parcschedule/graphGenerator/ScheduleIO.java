package parcschedule.graphGenerator;

import net.sourceforge.gxl.*;
import parcschedule.schedule.*;
import parcschedule.schedule.model.BasicProc;
import parcschedule.schedule.model.ClassicHomogeneousSystem;
import parcschedule.schedule.model.Proc;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceDOT;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

public class ScheduleIO {

	public static <V extends TaskVertex> void writeScheduleToGxl(String fileName, String graphName, OnePerTaskSchedule<V, ?, ?> schedule) {
		TaskGraph<V, ?> taskGraph = schedule.taskGraph();
		GXLDocument document = new GXLDocument();
		GXLGraph GXLGraph = new GXLGraph(graphName);

		DecimalFormat df = new DecimalFormat("0.00");
		GXLGraph.setAttr("No of nodes", new GXLInt(taskGraph.sizeTasks()));
		GXLGraph.setAttr("No of edges", new GXLInt(taskGraph.sizeEdges()));
		GXLGraph.setAttr("Density", new GXLFloat(Float.parseFloat(df.format(taskGraph.density()))));
		GXLGraph.setAttr("CCR", new GXLFloat(Float.parseFloat(df.format(taskGraph.getCCR()))));
		GXLGraph.setAttr("Total sequential time",new GXLInt(taskGraph.getTotalComputationCost()));

		Iterable<V> tasks = taskGraph.tasks();
		for (V t : tasks){
			GXLNode n = new GXLNode(t.name());
			n.setAttr("Weight", new GXLInt(t.weight()));
			n.setAttr("Start", new GXLInt(schedule.taskStartTime(t)));
			n.setAttr("Processor", new GXLString(schedule.taskProcAllocation(t).id()));
			GXLGraph.add(n);
		}

		Iterable<? extends CommEdge<? extends TaskVertex>> edges = taskGraph.edges();
		for(CommEdge<? extends TaskVertex> e : edges){
			GXLEdge edge = new GXLEdge(e.from().name(),e.to().name());
			edge.setAttr("Weight",new GXLInt(e.weight()));
			GXLGraph.add(edge);
		}

		document.getDocumentElement().add(GXLGraph);
		try {
			if (fileName.endsWith(".gxl")) {
				if (fileName.indexOf('/') > 0)
				{
					Files.createDirectories(Paths.get(fileName).getParent());
				}
				new File(fileName).createNewFile();
				document.write(new File(fileName));
			}
			else { // fileName is actually path name TODO: make this consistent.
				(new File(fileName)).mkdirs();
				document.write(new File(fileName+"/"+graphName+".gxl"));
			}
		}
		catch (IOException ioe) {
			System.err.println("Error while writing to file: "+ioe);
		}

		System.out.println(fileName + "/" + graphName);
	}

	public static OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> readScheduleFromGxl(File fileName){
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph = null;
		Map<TaskVertex, Proc> procAllocations = new HashMap<>();
		Map<TaskVertex, Integer> startTimes = new HashMap<>();
		Map<String, Proc> idToProcMap = new HashMap<>();

		Document document = null;
		Map<String, TaskVertex> idToTaskMap = new HashMap<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;

		if(!fileName.canRead()){
			System.out.println("File "+fileName.getName()+" cannot be read");
			System.exit(1);		//Abnormal exit
		}

		try {
			builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				@Override
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					return new InputSource(new StringReader(""));
				}
			});
			document = builder.parse(fileName);
		} catch (ParserConfigurationException e) {
			System.out.println("Parser configuration exception");
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("SAX exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOexception ");
			e.printStackTrace();
		}

		NodeList graphList = document.getElementsByTagName("graph");
		if(graphList.getLength() > 1){
			System.out.println("More than one graph in the xml document.");
			System.exit(1);
		}

		NodeList nodeList = document.getElementsByTagName("node");
		List<TaskVertex> NodesByStartTime = new ArrayList<>();

		graph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(
				fileName.getName().substring(0,fileName.getName().length()-4), nodeList.getLength());

		// read tasks
		for(int i = 0; i < nodeList.getLength(); i++){
			org.w3c.dom.Node domNode = nodeList.item(i);

			// get task id
			NamedNodeMap nodeMap = domNode.getAttributes();
			org.w3c.dom.Node idAttrNode = nodeMap.getNamedItem("id");
			String taskName = idAttrNode.getNodeValue();

			// read task attributes
			int weight = parseWeight(domNode);
			int startTime = parseStartTime(domNode);
			String procID = parseProc(domNode);
			// instantiate processor/find processor instance
			Proc proc = idToProcMap.get(procID);
			if (proc == null)
			{
				proc = new BasicProc(procID);
				idToProcMap.put(procID, proc);
			}

			// instantiate task/find task instance
			TaskVertex task = new BasicTaskVertex(taskName, i);
			task.setWeight(weight);
			idToTaskMap.put(taskName, task);

			procAllocations.put(task, proc);
			startTimes.put(task, startTime);

			graph.add(task);
			NodesByStartTime.add(task);
		}

		NodesByStartTime.sort(new Comparator<TaskVertex>() {

			@Override
			public int compare(TaskVertex o1, TaskVertex o2) {
				return startTimes.get(o1) - startTimes.get(o2);
			}
		});

		// read edges
		nodeList = document.getElementsByTagName("edge");
		for(int i = 0; i < nodeList.getLength(); i++){
			CommEdge<TaskVertex> edge = processEdgeElement(nodeList.item(i), idToTaskMap);
			graph.add(edge);
		}

		// create system object
		ClassicHomogeneousSystem system = new ClassicHomogeneousSystem();
		for (Proc proc : idToProcMap.values())
		{
			system.addProc(proc);
		}

		// create schedule
		OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule = new BasicSchedule<>(graph, system);
		for (TaskVertex task : NodesByStartTime)
		{
			schedule.put(task, procAllocations.get(task), startTimes.get(task), false);
		}

		return schedule;
	}

	/**
	 *
	 * @param domNode
	 * @param idToTaskMap
	 * @return	the Edge element processed
	 */
	private static CommEdge<TaskVertex> processEdgeElement(org.w3c.dom.Node domNode, Map<String, TaskVertex> idToTaskMap) {

		NamedNodeMap nodeMap = domNode.getAttributes();
		org.w3c.dom.Node attrNode;

		attrNode = nodeMap.getNamedItem("from");
		if (attrNode == null)
		{
			attrNode = nodeMap.getNamedItem("tail");
		}
		String fromName = attrNode.getNodeValue();
		attrNode = nodeMap.getNamedItem("to");
		if (attrNode == null)
		{
			attrNode = nodeMap.getNamedItem("head");
			// alternate names for gxls generated in python where 'from' and 'to' are reserved words
		}
		String toName = attrNode.getNodeValue();
		// Get corresponding vertices from map
		TaskVertex from = (TaskVertex) idToTaskMap.get(fromName);
		TaskVertex to = (TaskVertex) idToTaskMap.get(toName);

		int weight = parseWeight(domNode);

		CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(from.name() + "-" + to.name(), from, to, true);
		edge.setWeight(weight);
		return edge;
	}

	private static int parseWeight(org.w3c.dom.Node domNode) {
		int weight = 0;

		NodeList childList = domNode.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			org.w3c.dom.Node childNode = childList.item(i);
			if (childNode.getNodeName().equalsIgnoreCase("attr")) {
				NamedNodeMap nodeMap = childNode.getAttributes();
				org.w3c.dom.Node nameAttrNode = nodeMap.getNamedItem("name");
				if ((nameAttrNode.getNodeValue()).equalsIgnoreCase("weight")) {
					NodeList attrChildsList = childNode.getChildNodes();
					org.w3c.dom.Node attrChildNode = attrChildsList.item(1);
					String valueString = attrChildNode.getTextContent();
					weight = Integer.parseInt(valueString);
				}
			}
		}
		return weight;
	}

	private static int parseStartTime(org.w3c.dom.Node domNode) {
		int startTime = 0;

		NodeList childList = domNode.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			org.w3c.dom.Node childNode = childList.item(i);
			if (childNode.getNodeName().equalsIgnoreCase("attr")) {
				NamedNodeMap nodeMap = childNode.getAttributes();
				org.w3c.dom.Node nameAttrNode = nodeMap.getNamedItem("name");
				if ((nameAttrNode.getNodeValue()).equalsIgnoreCase("Start time")) {
					NodeList attrChildsList = childNode.getChildNodes();
					org.w3c.dom.Node attrChildNode = attrChildsList.item(1);
					String valueString = attrChildNode.getTextContent();
					startTime = Integer.parseInt(valueString);
				}
			}
		}
		return startTime;
	}

	private static String parseProc(org.w3c.dom.Node domNode) {
		String procID = null;

		NodeList childList = domNode.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			org.w3c.dom.Node childNode = childList.item(i);
			if (childNode.getNodeName().equalsIgnoreCase("attr")) {
				NamedNodeMap nodeMap = childNode.getAttributes();
				org.w3c.dom.Node nameAttrNode = nodeMap.getNamedItem("name");
				if ((nameAttrNode.getNodeValue()).equalsIgnoreCase("Processor")) {
					NodeList attrChildsList = childNode.getChildNodes();
					org.w3c.dom.Node attrChildNode = attrChildsList.item(1);
					procID = attrChildNode.getTextContent();
				}
			}
		}
		return procID;
	}

	public static OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> readScheduleFromDot(File fileName){
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph = null;
		Map<TaskVertex, Proc> procAllocations = new HashMap<>();
		Map<TaskVertex, Integer> startTimes = new HashMap<>();
		Map<String, Proc> idToProcMap = new HashMap<>();

		Map<String, TaskVertex> idToTaskMap = new HashMap<>();

		if(!fileName.canRead()){
			System.out.println("File "+fileName.getName()+" cannot be read");
			System.exit(1);		//Abnormal exit
		}

		org.graphstream.graph.Graph gsGraph = new DefaultGraph("temp graph");
		FileSource fs = new FileSourceDOT();

		fs.addSink(gsGraph);

		try {
			fs.readAll(fileName.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			fs.removeSink(gsGraph);
		}

		List<TaskVertex> NodesByStartTime = new ArrayList<>();

		graph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(
				fileName.getName().substring(0,fileName.getName().length()-4), gsGraph.getNodeCount());

		// read tasks
		for(int i = 0; i < gsGraph.getNodeSet().size(); i++){

			Node n = gsGraph.getNode(i);
			String procID = Double.toString(n.getAttribute("Processor"));
			Integer taskWeight = ((Double)n.getAttribute("Weight")).intValue();
			Integer taskStart  = ((Double)n.getAttribute("Start")).intValue();
			if(procID == null || taskWeight == null || taskStart == null){
				throw new IllegalStateException("Schedule attributes missing.");
			}



			Proc proc = idToProcMap.get(procID);
			if (proc == null)
			{
				proc = new BasicProc(procID);
				idToProcMap.put(procID, proc);
			}

			// instantiate task/find task instance
			TaskVertex task = new BasicTaskVertex(n.getId(), i);
			task.setWeight(taskWeight);
			idToTaskMap.put(n.getId(), task);

			procAllocations.put(task, proc);
			startTimes.put(task, taskStart);

			graph.add(task);
			NodesByStartTime.add(task);
		}

		NodesByStartTime.sort(new Comparator<TaskVertex>() {

			@Override
			public int compare(TaskVertex o1, TaskVertex o2) {
				return startTimes.get(o1) - startTimes.get(o2);
			}
		});

		// read edges
		for(int i = 0; i < gsGraph.getEdgeSet().size(); i++){
			Edge e = gsGraph.getEdge(i);
			Integer weight = ((Double)e.getAttribute("Weight")).intValue();
			CommEdge<TaskVertex> commEdge = new BasicCommEdge<>(e.getId(), graph.task(e.getSourceNode().getIndex()),
					graph.task(e.getTargetNode().getIndex()), true);
			commEdge.setWeight(weight);
			graph.add(commEdge);
		}

		// create system object
		ClassicHomogeneousSystem system = new ClassicHomogeneousSystem();
		for (Proc proc : idToProcMap.values())
		{
			system.addProc(proc);
		}

		// create schedule
		OnePerTaskSchedule<TaskVertex, CommEdge<TaskVertex>, Proc> schedule = new BasicSchedule<>(graph, system);
		for (TaskVertex task : NodesByStartTime)
		{
			schedule.put(task, procAllocations.get(task), startTimes.get(task), false);
		}

		return schedule;
	}
}
