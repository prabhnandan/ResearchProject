package parcschedule.graphGenerator.graphReaders;

import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceDOT;
import parcschedule.schedule.*;

import java.io.File;
import java.io.IOException;

public class DotReader {

    public static TaskGraph<TaskVertex, CommEdge<TaskVertex>> read(File file)
    {

        org.graphstream.graph.Graph gsGraph = new DefaultGraph("temp graph");
        FileSource fs = new FileSourceDOT();

        fs.addSink(gsGraph);

        try {
            fs.readAll(file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            fs.removeSink(gsGraph);
        }

        TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = new BasicTaskGraph<>(file.getName().substring(0, file.getName().length() - 4), gsGraph.getNodeCount());

        for (int i = 0; i < gsGraph.getNodeCount(); i++)
        {
            TaskVertex taskVertex = new BasicTaskVertex(gsGraph.getNode(i).getId(), i);
            Integer weight = ((Double)gsGraph.getNode(i).getAttribute("Weight")).intValue();
            taskVertex.setWeight(weight);
            taskGraph.add(taskVertex);
        }

        int index = 0;
        for (Edge e : gsGraph.getEdgeSet())
        {
            String id = e.getId();
            TaskVertex n1 = taskGraph.task(e.getNode0().getIndex());
            TaskVertex n2 = taskGraph.task(e.getNode1().getIndex());
            CommEdge<TaskVertex> edge = new BasicCommEdge<TaskVertex>(id, n1, n2, true);
            Integer weight = ((Double)e.getAttribute("Weight")).intValue();
            edge.setWeight(weight);
            taskGraph.add(edge);
        }

        return taskGraph;

    }

    public static void main(String[] args) {

        TaskGraph<TaskVertex, CommEdge<TaskVertex>> taskGraph = DotReader.read(new File("DAGS/Nodes_7_OutTree.dot"));

        System.out.println(taskGraph.sizeTasks());
        System.out.println(taskGraph.sizeEdges());

    }

}
