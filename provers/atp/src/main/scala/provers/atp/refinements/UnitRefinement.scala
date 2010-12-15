/*
 * UnitRefinement.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.provers.atp.refinements

import at.logic.calculi.resolution.base._
import at.logic.calculi.lk.base._
import scala.collection.mutable.{Queue,ListBuffer}
import at.logic.utils.ds.PublishingBuffer

class UnitRefinement[V <: Sequent](c: PublishingBuffer[V]) extends Refinement[V] {
  val clauses = c // all clauses
  init
  val pairs = new ListBuffer[Tuple2[ResolutionProof[V],ResolutionProof[V]]] // all pairs of possible two clauses
  val proofs = new ListBuffer[ResolutionProof[V]] // all clauses as proofs
  val units: ListBuffer[ResolutionProof[V]] = new ListBuffer[ResolutionProof[V]]()
  insertClauses

  def getNextClausesPair: Option[Tuple2[ResolutionProof[V], ResolutionProof[V]]] = if (pairs.isEmpty) None else Some(pairs.remove(0))

  def getClausesPair(c1: V, c2: V): Option[Tuple2[ResolutionProof[V], ResolutionProof[V]]] = {
    val pairInd = pairs.findIndexOf(x => (x._1.root == c1 && x._2.root == c2) || (x._1.root == c2 && x._2.root == c1))
    if (pairInd > -1) {val ret = pairs(pairInd); pairs.remove(pairInd); Some(ret)}
    else None
  }

  private def insertClauses = {
    proofs ++= clauses.map(createInitialProof)
    units ++= proofs.filter(x => isUnit(x.root))
    val tmp = proofs.toList
    pairs ++= (for (
        c1 <- units;
        c2 <- proofs;
        if (!(c1.root.antecedent.isEmpty && c2.root.antecedent.isEmpty) &&
          !(c1.root.succedent.isEmpty && c2.root.succedent.isEmpty) &&
          c1.root != c2.root)
      ) yield (c1,c2))
  }
  def insertProof(proof: ResolutionProof[V]) = {
    clauses.append(proof.root)
    pairs ++= {for (
        c1 <- (if (isUnit(proof.root)) proofs else units);
        if (!(c1.root.antecedent.isEmpty && proof.root.antecedent.isEmpty) &&
          !(c1.root.succedent.isEmpty && proof.root.succedent.isEmpty))
      ) yield (c1,proof)
    }
    proofs += proof
    if (isUnit(proof.root)) units += proof
  }

  protected def removeClause(s: V) = {
    proofs.filter(x => x.root == s).foreach(x => proofs -= x)
    pairs.filter(x => x._1.root == s || x._2.root == s).foreach(x => pairs -= x)
  }
  private def createInitialProof(c: V): ResolutionProof[V] = InitialSequent(c)
  private def isUnit(c: V): Boolean = (c.antecedent.size + c.succedent.size) == 1
}

  /*
trait UnitRefinement  extends Refinement {
  val queue: Queue[Tuple2[ResolutionProof,ResolutionProof]] = new Queue[Tuple2[ResolutionProof,ResolutionProof]]()
  val clauses:ListBuffer[ResolutionProof] = new ListBuffer[ResolutionProof]()
  val units: ListBuffer[ResolutionProof] = new ListBuffer[ResolutionProof]()
  
  def getClauses = try {
      Some(queue.dequeue)
    } catch {
      case ex: Predef.NoSuchElementException => None
  }
  def insertClauses(c: List[Clause]) = {
    clauses ++= c.map(x => Axiom(x))
    units ++= clauses.filter(x => isUnit(x.root))
    queue ++= {for (
        c1 <- units;
        c2 <- clauses;
        if (!(c1.root.antecedent.isEmpty && c2.root.antecedent.isEmpty) &&
          !(c1.root.succedent.isEmpty && c2.root.succedent.isEmpty) &&
          c1.root != c2.root)
      ) yield (c1,c2)
    }
  }
  def insertProof(proof: ResolutionProof) = {
    queue ++= {for (
        c1 <- units;
        if (!(c1.root.antecedent.isEmpty && proof.root.antecedent.isEmpty) &&
          !(c1.root.succedent.isEmpty && proof.root.succedent.isEmpty))
      ) yield (c1,proof)
    }
    clauses += proof
    if (isUnit(proof.root)) units += proof
  }
  private def isUnit(c: Clause): Boolean = (c.negative.size + c.positive.size) == 1
}
*/