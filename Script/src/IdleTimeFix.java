import net.sourceforge.gxl.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class IdleTimeFix {
    public static void main(String[] args) throws IOException, SAXException {
        File[] outputFiles = new File("C:/Users/Prabh/Downloads/output/").listFiles();
        for (File file : outputFiles) {
            GXLDocument doc = new GXLDocument(file);
            String name = file.getName();
            GXLGraph graph = doc.getDocumentElement().getGraphAt(0);
//            int numProcs = doc.get;
            GXLInt procsElement = (GXLInt) (graph.getAttr("Number of Processors").getChildAt(0));
            int numProcs = procsElement.getIntValue();
            ArrayList<Integer> procsUsed = new ArrayList<Integer>();
            for (int i = 0; i < graph.getChildCount(); i++) {
                if (graph.getChildAt(i) instanceof GXLNode) {
                    GXLElement node = graph.getChildAt(i);
                    GXLInt procElement = (GXLInt) node.getChildAt(2).getChildAt(0);
                    Integer proc = procElement.getIntValue();
                    if (!procsUsed.contains(proc)) {
                        procsUsed.add(proc);
                    }
                    if (procsUsed.size() == numProcs) break;
                }
            }

            String[] nameSplit = name.split(".gxl");
            String parameter = nameSplit[1];

            StringBuilder sb = new StringBuilder();
            sb.append(nameSplit[0] + ".gxl,");

            if (parameter.contains("20")) sb.append(20 + ",");
            else if (parameter.contains("2")) sb.append(2 + ",");

            if (parameter.contains("4")) sb.append(4 + ",");
            if (parameter.contains("8")) sb.append(8 + ",");

            if (parameter.contains("blevel")) sb.append("blevel" + ",");
            if (parameter.contains("etf")) sb.append("etf" + ",");
            if (parameter.contains("est") & !parameter.contains("weighted-children-est")) sb.append("est" + ",");

            if (parameter.contains("weighted-children-est")) sb.append("weighted-children-est" + ",");
            else if (parameter.contains("norm")) sb.append("norm" + ",");
            else sb.append("null,");

            if (parameter.contains("dcp")) sb.append("dcp" + ",");
            else if (parameter.contains("dsc")) sb.append("dsc" + ",");
            else sb.append("null,");

            if (parameter.contains("glb")) sb.append("glb" + ",");
            else if (parameter.contains("list")) sb.append("list" + ",");
            else sb.append("null,");

            sb.append((numProcs - procsUsed.size()) + "\n");
            file.delete();
            File CSVFile = new File("idleProcessors.csv");
            try (FileWriter writer = new FileWriter(CSVFile, true)) {
                writer.append(sb.toString());
                writer.flush();
                writer.close();
            }
        }
    }
}
