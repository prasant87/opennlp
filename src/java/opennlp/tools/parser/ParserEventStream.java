///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2003 Thomas Morton
//
//This library is free software; you can redistribute it and/or
//modify it under the terms of the GNU Lesser General Public
//License as published by the Free Software Foundation; either
//version 2.1 of the License, or (at your option) any later version.
//
//This library is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU Lesser General Public
//License along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//////////////////////////////////////////////////////////////////////////////   
package opennlp.tools.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import opennlp.maxent.DataStream;
import opennlp.maxent.Event;
import opennlp.maxent.EventStream;
import opennlp.tools.chunker.ChunkerContextGenerator;
import opennlp.tools.postag.DefaultPOSContextGenerator;
import opennlp.tools.postag.POSContextGenerator;

/**
 * Wrapper class for one of four parser event streams.  The particular event stram is specified 
 * at construction.
 * @author Tom Morton
 *
 */
public class ParserEventStream implements EventStream {

  private BuildContextGenerator bcg;
  private CheckContextGenerator kcg;
  private ChunkerContextGenerator ccg;
  private POSContextGenerator tcg;
  private DataStream data;
  private Event[] events;
  private int ei;
  private HeadRules rules;
  private EventTypeEnum etype;

  /**
   * Create an event stream based on the specified data stream of the specified type using the specified head rules.
   * @param d A 1-parse-per-line Penn Treebank Style parse. 
   * @param rules The head rules.
   * @param etype The type of events desired (tag, chunk, build, or check).
   */
  public ParserEventStream(DataStream d, HeadRules rules, EventTypeEnum etype) {
    if (etype == EventTypeEnum.BUILD) {
      this.bcg = new BuildContextGenerator();
    }
    else if (etype == EventTypeEnum.CHECK) {
      this.kcg = new CheckContextGenerator();
    }
    else if (etype == EventTypeEnum.CHUNK) {
      this.ccg = new ChunkContextGenerator();
    }
    else if (etype == EventTypeEnum.TAG) {
      this.tcg = new DefaultPOSContextGenerator();
    }
    this.rules = rules;
    this.etype = etype;
    data = d;
    ei = 0;
    if (d.hasNext()) {
      addNewEvents();
    }
    else {
      events = new Event[0];
    }
  }

  public boolean hasNext() {
    return (ei < events.length || data.hasNext());
  }

  public Event nextEvent() {
    if (ei == events.length) {
      addNewEvents();
      ei = 0;
    }
    return ((Event) events[ei++]);
  }

  private static void getInitialChunks(Parse p, List ichunks) {
    if (p.isPosTag()) {
      ichunks.add(p);
    }
    else {
      List kids = p.getChildren();
      boolean allKidsAreTags = true;
      for (int ci = 0, cl = kids.size(); ci < cl; ci++) {
        if (!((Parse) kids.get(ci)).isPosTag()) {
          allKidsAreTags = false;
        }
      }
      if (allKidsAreTags) {
        ichunks.add(p);
      }
      else {
        for (int ci = 0, cl = kids.size(); ci < cl; ci++) {
          getInitialChunks((Parse) kids.get(ci), ichunks);
        }
      }
    }
  }

  private static List getInitialChunks(Parse p) {
    List chunks = new ArrayList();
    getInitialChunks(p, chunks);
    return chunks;
  }

  private static boolean firstChild(Parse c, Parse parent) {
    List kids = parent.getChildren();
    return (kids.get(0) == c);
  }

  private static boolean lastChild(Parse c, Parse parent) {
    List kids = parent.getChildren();
    return (kids.get(kids.size() - 1) == c);
  }

  private void addNewEvents() {
    String parseStr = (String) data.nextToken();
    List events = new ArrayList();
    Parse p = Parse.parseParse(parseStr, rules);
    List chunks = getInitialChunks(p);
    if (etype == EventTypeEnum.TAG) {
      addTagEvents(events, chunks);
    }
    else if (etype == EventTypeEnum.CHUNK) {
      addChunkEvents(events, chunks);
    }
    else {
      addParseEvents(events, chunks);
    }
    this.events = (Event[]) events.toArray(new Event[events.size()]);
  }

  private void addParseEvents(List events, List chunks) {
    int ci = 0;
    while (ci < chunks.size()) {
      Parse c = (Parse) chunks.get(ci);
      Parse parent = c.getParent();
      if (parent != null) {
        String type = parent.getType();
        String outcome;
        if (firstChild(c, parent)) {
          outcome = ParserME.START + type;
        }
        else {
          outcome = ParserME.CONT + type;
        }
        c.setLabel(outcome);
        if (etype == EventTypeEnum.BUILD) {
          events.add(new Event(outcome, bcg.getContext(chunks, ci)));
        }
        int start = ci - 1;
        while (start >= 0 && ((Parse) chunks.get(start)).getParent() == parent) {
          start--;
        }
        if (lastChild(c, parent)) {
          if (etype == EventTypeEnum.CHECK) {
            events.add(new Event(ParserME.COMPLETE, kcg.getContext( chunks, type, start + 1, ci)));
          }
          //perform reduce
          chunks.remove(ci);
          ci--;
          while (ci >= 0 && ((Parse) chunks.get(ci)).getParent() == parent) {
            chunks.remove(ci);
            ci--;
          }
          if (!type.equals(ParserME.TOP_NODE)) {
            chunks.add(ci + 1, parent);
          }
        }
        else {
          if (etype == EventTypeEnum.CHECK) {
            events.add(new Event(ParserME.INCOMPLETE, kcg.getContext(chunks, type, start + 1, ci)));
          }
        }
      }
      ci++;
    }
  }

  private void addChunkEvents(List events, List chunks) {
    List toks = new ArrayList();
    List tags = new ArrayList();
    List preds = new ArrayList();
    for (int ci = 0, cl = chunks.size(); ci < cl; ci++) {
      Parse c = (Parse) chunks.get(ci);
      if (c.isPosTag()) {
        toks.add(c.toString());
        tags.add(c.getType());
        preds.add(ParserME.OTHER);
      }
      else {
        boolean start = true;
        String ctype = c.getType();
        for (Iterator ti = c.getChildren().iterator(); ti.hasNext();) {
          Parse tok = (Parse) ti.next();
          toks.add(tok.toString());
          tags.add(tok.getType());
          if (start) {
            preds.add(ParserME.START + ctype);
            start = false;
          }
          else {
            preds.add(ParserME.CONT + ctype);
          }
        }
      }
    }
    for (int ti = 0, tl = toks.size(); ti < tl; ti++) {
      events.add(new Event((String) preds.get(ti), ccg.getContext(ti, toks.toArray(), (String[]) tags.toArray(new String[tags.size()]), (String[]) preds.toArray(new String[preds.size()]))));
    }
  }

  private void addTagEvents(List events, List chunks) {
    List toks = new ArrayList();
    List preds = new ArrayList();
    for (int ci = 0, cl = chunks.size(); ci < cl; ci++) {
      Parse c = (Parse) chunks.get(ci);
      if (c.isPosTag()) {
        toks.add(c.toString());
        preds.add(c.getType());
      }
      else {
        for (Iterator ti = c.getChildren().iterator(); ti.hasNext();) {
          Parse tok = (Parse) ti.next();
          toks.add(tok.toString());
          preds.add(tok.getType());
        }
      }
    }
    for (int ti = 0, tl = toks.size(); ti < tl; ti++) {
      events.add(new Event((String) preds.get(ti), tcg.getContext(ti, toks.toArray(), (String[]) preds.toArray(new String[preds.size()]), null)));
    }
  }

  public static void main(String[] args) throws java.io.IOException {
    if (args.length == 0) {
      System.err.println("Usage ParserEventStream -[tag|chunk|build|check] head_rules < parses");
      System.exit(1);
    }
    EventTypeEnum etype = null;
    int ai = 0;
    if (args[ai].equals("-build")) {
      etype = EventTypeEnum.BUILD;
    }
    else if (args[ai].equals("-check")) {
      etype = EventTypeEnum.CHECK;
    }
    else if (args[ai].equals("-chunk")) {
      etype = EventTypeEnum.CHUNK;
    }
    else if (args[ai].equals("-tag")) {
      etype = EventTypeEnum.TAG;
    }
    else {
      System.err.println("Invalid option " + args[ai]);
      System.exit(1);
    }
    ai++;
    EnglishHeadRules rules = new EnglishHeadRules(args[ai++]);
    opennlp.maxent.EventStream es = new ParserEventStream(new opennlp.maxent.PlainTextByLineDataStream(new java.io.InputStreamReader(System.in)), rules, etype);
    while (es.hasNext()) {
      System.out.println(es.nextEvent());
    }
  }
}

/**
 * Enumerated type of event types for the parser. 
 *
 */
class EventTypeEnum {

  private String name;

  public static final EventTypeEnum BUILD = new EventTypeEnum("build");
  public static final EventTypeEnum CHECK = new EventTypeEnum("check");
  public static final EventTypeEnum CHUNK = new EventTypeEnum("chunk");
  public static final EventTypeEnum TAG = new EventTypeEnum("tag");

  private EventTypeEnum(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
