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
//GNU Lesser General Public License for more details.
//
//You should have received a copy of the GNU Lesser General Public
//License along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//////////////////////////////////////////////////////////////////////////////
package opennlp.tools.coref.mention;

import opennlp.tools.util.Span;

public class Extent implements Comparable {

  /** Represents the character offset for this extent. */
  private Span span;
  /** A string representing the type of this extent.  This is helpfull for determining
   * which piece of code created a particular extent.
   */
  private String type;
  /** The entity id indicating which entity this extent belongs to.  This is only
   * used when training a coreference classifier.
   */
  private int id;
  /** Represents the character offsets of the the head of this extent. */
  private Span headSpan;
  /** The parse node that this extent is based on. */
  private Parse baseParse;
  
  /** A string representing the name type for this extent. */
  private String nameType;
    
  public Extent(Span span, Span headSpan, int entityId, Parse baseParse, String extentType) {
    this.span=span;
    this.headSpan=headSpan;
    this.id=entityId;
    this.type=extentType;
    this.baseParse = baseParse;
    //System.err.println("Extent.init: span="+s+" entitiy_id="+i);
  }

  /**
   * Returns the character offsets for this extent.
   * @return The span representing the character offsets of this extent.
   */
  public Span getSpan() {
    return(span);
  }

  /**
   * Returns the character offsets for the head of this extent.
   * @return The span representing the character offsets for the head of this extent.
   */
  public Span getHeadSpan() {
    return(headSpan);
  }
  
  /** 
   * Returns the parse node that this extent is based on.
   * @return The parse node that this extent is based on or null if the extent is newly created.
   */
  public Parse getBaseParse() {
    return baseParse;
  }
  
  public String getType() {
    return(type);
  }

  public int compareTo(Object o) {
    Extent e = (Extent) o;
    return(span.compareTo(e.span));
  }
  
  public void setParse(Parse parse) {
    baseParse = parse;
  }

  public String getNameType() {
    return nameType;
  }
  
  void setNameType(String nameType) {
    this.nameType = nameType;
  }

  public void setId(int i) {
    id=i;
  }

  public int getId() {
    return(id);
  }

  public String toString() {
    return("extent(span="+span+",hs="+headSpan+", type="+type+", id="+id+")"); 
  }

}