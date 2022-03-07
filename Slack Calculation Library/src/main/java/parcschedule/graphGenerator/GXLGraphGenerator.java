package parcschedule.graphGenerator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import parcschedule.schedule.BasicCommEdge;
import parcschedule.schedule.BasicTaskGraph;
import parcschedule.schedule.BasicTaskVertex;
import parcschedule.schedule.CommEdge;
import parcschedule.schedule.TaskGraph;
import parcschedule.schedule.TaskVertex;

/**
 * Responsible for generating a task graph object from 
 * a GXL file representing a task graph.
 */
public class GXLGraphGenerator {
	
	/**
	 * Returns a task graph object by parsing a given GXL file.
	 * 
	 * @param fileName    the GXL file containing the task graph
	 * @param sb
	 * @return			the TaskGraph object
	 */
	public static TaskGraph generate(File fileName, StringBuilder sb){
		TaskGraph<TaskVertex, CommEdge<TaskVertex>> graph = null;

		sb.append(fileName.getParentFile().getName()+",");
		sb.append(fileName.getName()+",");


		Document document = null;
	    Map idToTaskMap = new HashMap();
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
		
	    //Adding all tasks
	    ArrayList taskList = new ArrayList();
	    NodeList nodeList = document.getElementsByTagName("node");

	    for(int i = 0; i < nodeList.getLength(); i++){
	    	taskList.add(processTaskElement(nodeList.item(i), idToTaskMap, i));
	    }
	    
	    graph = new BasicTaskGraph<TaskVertex, CommEdge<TaskVertex>>(
	    		fileName.getName().substring(0,fileName.getName().length()-4), taskList.size());
	    
	    for(Iterator it=taskList.iterator();it.hasNext();){
	    	graph.add((TaskVertex)it.next());
	    }

		nodeList = document.getElementsByTagName("edge");
	    for(int i = 0; i < nodeList.getLength(); i++){
	    	CommEdge<TaskVertex> edge = processEdgeElement(nodeList.item(i), idToTaskMap);
	    	graph.add(edge);
	    }

		return graph;
	}
	
	/**
	 * 
	 * @param domNode
	 * @param idToTaskMap
	 * @param index
	 * @return		the Task object
	 */
	private static TaskVertex processTaskElement(org.w3c.dom.Node domNode, Map idToTaskMap, int index) {
		NamedNodeMap nodeMap = domNode.getAttributes();
		org.w3c.dom.Node idAttrNode = nodeMap.getNamedItem("id");
	    String taskName = idAttrNode.getNodeValue();
	    
	    int weight = parseWeight(domNode);
	    TaskVertex task = new BasicTaskVertex(taskName,index);
	    task.setWeight( weight);
	     
	    idToTaskMap.put(taskName, task);
	    return task;
	}
	
	/**
	 * 
	 * @param domNode
	 * @param idToTaskMap
	 * @return	the Edge element processed
	 */
	private static CommEdge<TaskVertex> processEdgeElement(org.w3c.dom.Node domNode, Map idToTaskMap) {
		
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
}
