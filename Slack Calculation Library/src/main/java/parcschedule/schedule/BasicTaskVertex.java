package parcschedule.schedule;

import parcgraph.graph.*;

public class BasicTaskVertex extends BasicVertex  implements TaskVertex {
	
  private final int index;
	
  public BasicTaskVertex(int index) {
    this(Integer.toString(index), index);
  }
	
  public BasicTaskVertex(String name, int index) {
    super(name);
    this.index = index;
  }

  public String toXML() {
		
    StringBuffer b = new StringBuffer();
    b.append("<node id=\"" + this.name() +"\">");
    b.append("<attr name=\" name\"><string>");
    b.append(this.name());
    b.append("</string></attr>");

    b.append("<attr name=\" weight\"><int>");
    b.append(this.weight());
    b.append("</int></attr>");

    b.append("</node>");
    return b.toString();

  }

  public int index() {
	  return index;
  }

}
