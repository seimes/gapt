/* Description: Tests for the base prover
**/

package at.logic.provers.atp

import org.specs._
import org.specs.runner._
import org.specs.mock.Mockito
import org.mockito.Matchers._  // to use matchers like anyInt()
import at.logic.parsing.calculi.simple.SimpleResolutionParserFOL
import at.logic.parsing.readers.StringReader
import at.logic.calculi.resolution.base._
import at.logic.calculi.resolution.robinson._
import refinements.SimpleRefinement
import commands._
import ui._
import commandsParsers.RobinsonCommandsParser
import commandsParsers.robinson._

private class MyParser(str: String) extends StringReader(str) with SimpleResolutionParserFOL
private object MyProver extends Prover[Clause] {val panel = new UIPanel[Clause]{def getNextCommand(com:Command, elements: Option[Iterator[Clause]]): Command = ExitCom}}

class ProverTest extends SpecificationWithJUnit {
  "Prover" should {
    "in case it has only one clause return it if it is the empty clause" in {
      MyProver.refute(simpleAutoStream(".")).head must beLike {
        case a: ResolutionProof[_] if a.root setEquals theEmptyClause().root => true
      }
    }
    "in case it has an empty clause set return None" in {
      MyProver.refute(simpleAutoStream("")) must beEqual (Stream.empty)
    }
    "in case it has only one clause return None if it is not the empty clause" in {
      MyProver.refute(simpleAutoStream("P(x).")) must beEqual (Stream.empty)
    }
    "refute the following clauses" in {
      "p(a). -p(x) | p(f(x)). -p(f(f(a)))" in {
        MyProver.refute(simpleAutoStream("P(a). -P(x) | P(f(x)). -P(f(f(a))).")).head must beLike {
          case a: ResolutionProof[_] if a.root setEquals theEmptyClause().root => true
        }
      }
      "requiring factoring" in {
        "p(a). -p(y) | -p(x) | p(f(y)) | p(f(x)). -p(f(f(a)))" in {
          MyProver.refute(simpleAutoStream("P(a). -P(y) | -P(x) | P(f(y)) | P(f(x)). -P(f(f(a))).")).head must beLike {
            case a: ResolutionProof[_] if a.root setEquals theEmptyClause().root => true
          }
        }
      }
      "requiring non-terminal factoring" in {
        "P(a). -P(x) | P(f(x)) | P(f(y)). -P(f(f(a))). -P(f(f(b)))." in {
          MyProver.refute(simpleAutoStream("P(a). -P(x) | P(f(x)) | P(f(y)). -P(f(f(a))). -P(f(f(b))).")).head must beLike {
            case a: ResolutionProof[_] if a.root setEquals theEmptyClause().root => true
          }
        }
      }
     "requiring paramodulation" in {
        "P(a). -P(b). =(a,b)." in {
          MyProver.refute(simpleAutoStream("P(a). -P(b). =(a,b).")).head must beLike {
            case a: ResolutionProof[_] if a.root setEquals theEmptyClause().root => true
          }
        }
      }
    }
    /*"When there is a refutation the proof should be correct (clauses from the set as initials and using only the rules in a correct way" in {
      "ex1"
    }*/
    // test with a different target clause than the empty
  }
  " Prover with unit refinement" should {
    "refute the following clauses" in {
      "p(a). -p(x) | p(f(x)). -p(f(f(a)))" in {
        MyProver.refute(unitAutoStream("P(a). -P(x) | P(f(x)). -P(f(f(a))).")).head must beLike {
          case a: ResolutionProof[_] if a.root setEquals theEmptyClause().root => true
        }
      }
      // this should not work as it cannot resolve :-P(f(a)), P(f(a)) with P(f(a)), P(f(a)):-
      /*"p(a). -p(x) | -p(x) | p(f(x)) | p(f(x)). -p(f(f(a)))" in {
        MyProver.refute(unitAutoStream("P(a). -P(x) | -P(x) | P(f(x)) | P(f(x)). -P(f(f(a))).")).head must beLike {
          case a: ResolutionProof if a.root.formulaEquivalece(theEmptyClause().root) => true
        }
      }*/
    }
  }

  import at.logic.utils.ds.PublishingBuffer

  def createSubsum(pb: PublishingBuffer[Clause]): at.logic.algorithms.subsumption.managers.SubsumptionManager =
    new at.logic.algorithms.subsumption.managers.SimpleManager(pb.asInstanceOf[PublishingBuffer[at.logic.calculi.lk.base.Sequent]],
                        new at.logic.algorithms.subsumption.StillmanSubsumptionAlgorithm[at.logic.language.fol.FOLExpression] {val matchAlg = at.logic.algorithms.matching.fol.FOLMatchingAlgorithm})

  def autoStream(cl: String, createRef: (PublishingBuffer[Clause] => at.logic.provers.atp.refinements.Refinement[Clause])) = {
    val cls = new MyParser(cl).getClauseList
    AutomatedFOLStream(-1, cls, createRef, createSubsum)
  }
  
  def createSimple(pb: PublishingBuffer[Clause]) = new at.logic.provers.atp.refinements.SimpleRefinement(pb)
  def createUnit(pb: PublishingBuffer[Clause]) = new at.logic.provers.atp.refinements.UnitRefinement(pb)

  def simpleAutoStream(cl: String) = autoStream(cl, createSimple)

  def unitAutoStream(cl: String) = autoStream(cl, createUnit)
}