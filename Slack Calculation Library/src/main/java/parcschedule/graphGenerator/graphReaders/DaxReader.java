package parcschedule.graphGenerator.graphReaders;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import parcschedule.schedule.BasicCommEdge;
import parcschedule.schedule.BasicTaskGraph;
import parcschedule.schedule.BasicTaskVertex;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

public class DaxReader 
{
	public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> read(File file) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(file);
		
		Map<String, TaskVertex> taskIdInstanceMap = new HashMap<>();
		
		Element dag = document.getDocumentElement();
		NodeList taskList = dag.getElementsByTagName("job");

		TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<>(file.getName().substring(0, file.getName().length() - 4), taskList.getLength());
		
		for (int i = 0; i < taskList.getLength(); i++)
		{
			Element job = (Element) taskList.item(i);
			TaskVertex taskVertex = new BasicTaskVertex(Integer.toString(i), i);
			int weight = (int) Math.round(Double.parseDouble(job.getAttribute("runtime")));
			if (weight == 0) weight = 1;
			taskVertex.setWeight(weight);
			taskIdInstanceMap.put(job.getAttribute("id"), taskVertex);
			taskGraph.add(taskVertex);
		}
		
		NodeList edgeHeads = dag.getElementsByTagName("child");
		
		int index = 0;
		for (int i = 0; i < edgeHeads.getLength(); i++)
		{
			Element vertexElement = (Element) edgeHeads.item(i);
			TaskVertex headVertex = taskIdInstanceMap.get(vertexElement.getAttribute("ref"));
			
			NodeList tails = vertexElement.getElementsByTagName("parent");
			for (int j = 0; j < tails.getLength(); j++)
			{
				CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(Integer.toString(index++), taskIdInstanceMap.get(((Element) tails.item(j)).getAttribute("ref")), headVertex, true);
				taskGraph.add(edge);
				edge.setWeight(0);
			}
		}
		
		return taskGraph;
		
	}
}
