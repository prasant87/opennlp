///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2003 Gann Bierner and Thomas Morton
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
package opennlp.common.util;

import java.util.*;

import opennlp.maxent.ContextGenerator;
import opennlp.maxent.MaxentModel;


/** Performs k-best search over sequence.  This is besed on the description in
  * Ratnaparkhi (1998), PhD diss, Univ. of Pennsylvania. 
 */
public class BeamSearch {

  protected MaxentModel model;
  protected ContextGenerator cg;
  protected int size;

  /** Creates new search object. 
   * @param size The size of the beam (k).
   * @param cg the context generator for the model. 
   * @param model the model for assigning probabilities to the sequence outcomes.
   */
  public BeamSearch(int size, ContextGenerator cg, MaxentModel model) {
    this.size = size;
    this.cg = cg;
    this.model = model;
  }

  /** Returns the best sequence of outcomes based on model for this object.
   * @param sequence The input sequence.
   * @param context An Object[] of additional context.  This is passed to the context generator blindly with the assumption that the context are appropiate.
   * @return The top ranked sequence of outcomes.
   */
  public Sequence bestSequence(List sequence, Object context) {
    int n = sequence.size();
    SortedSet prev = new TreeSet();
    SortedSet next = new TreeSet();
    SortedSet tmp;
    prev.add(new Sequence());
    Object[] additionalContext = (Object[]) context;
    if (additionalContext == null) {
      additionalContext = new Object[0];
    }
    for (int i = 0; i < n; i++) {
      int sz = Math.min(size, prev.size());
      for (int j = 1; j <= sz; j++) {
        Sequence top = (Sequence) prev.first();
        prev.remove(top);
        Object[] params = new Object[additionalContext.length + 3];
        params[0] = new Integer(i);
        params[1] = sequence;
        params[2] = top;
        for (int aci = 0, acl = additionalContext.length; aci < acl; aci++) {
          params[3 + aci] = additionalContext[aci];
        }
        double[] scores = model.eval(cg.getContext(params));
        double[] temp_scores = new double[scores.length];

        for (int c = 0; c < scores.length; c++) {
          if (!validSequence(i, sequence, top, model.getOutcome(c))) {
            scores[c] = 0;
          }
          temp_scores[c] = scores[c];
        }
        Arrays.sort(temp_scores);
        double min = temp_scores[temp_scores.length - size];

        for (int p = 0; p < scores.length; p++) {
          if (scores[p] < min)
            continue;
          Sequence newS = top.copy();
          newS.add(model.getOutcome(p), scores[p]);
          next.add(newS);
        }
      }
      //    make prev = next; and re-init next (we reuse existing prev set once we clear it)
      prev.clear();
      tmp = prev;
      prev = next;
      next = tmp;
    }
    return (Sequence) prev.first();
  }

  /** Determines wheter a particular continuation of a sequence is valid.  
   * This is used to restrict invalid sequences such as thoses used in start/continure tag-based chunking 
   * or could be used to implement tag dictionary restrictions.
   * @param i The index in the input sequence for which the new outcome is being proposed.
   * @param inputSequence The input sequnce.
   * @param outcomesSequence The outcomes so far in this sequence.
   * @param outcome The next proposed outcome for the outcomes sequence.
   * @return true is the sequence would still be valid with the new outcome, false otherwise.
   */
  protected boolean validSequence(int i, List inputSequence, Sequence outcomesSequence, String outcome) {
    return true;
  }

}