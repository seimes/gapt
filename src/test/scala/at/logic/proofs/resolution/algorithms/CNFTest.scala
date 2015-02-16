package at.logic.proofs.resolution.algorithms

import at.logic.language.fol.{And, Atom, FOLConst, Imp, Neg, Or}
import at.logic.language.hol.{HOLAtom => HOLAtom}
import at.logic.proofs.resolution.FClause
import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CNFTest extends SpecificationWithJUnit {
  "the computation of CNFp(f)" should {
    "be {|- Pa,Qa, Qa|-} for f = (Pa ∨ Qa) ∧ ¬Qa" in {
      val Pa = Atom("P", FOLConst("a")::Nil)
      val Qa = Atom("Q", FOLConst("a")::Nil)
      val nQa = Neg(Qa)
      val PavQa = Or(Pa,Qa)
      val f = And(PavQa, nQa)
      CNFp(f).toSet must beEqualTo(Set(FClause(List(),List(Pa,Qa)),FClause(List(Qa),List())))
    }
  }

  "the computation of TseitinCNF(f)" should {
    "should be right, where f = ((P ∨ Q) ∧ R ) -> ¬S" in {
      val p = Atom("P", Nil)
      val q = Atom("Q", Nil)
      val r = Atom("R", Nil)
      val s = Atom("S", Nil)

      val f = Imp(And(Or(p, q), r), Neg(s))

      val x =  Atom("x", Nil)
      val x0 = Atom("x0", Nil)
      val x1 = Atom("x1", Nil)
      val x2 = Atom("x2", Nil)

      val cnf = TseitinCNF(f)
      val expected = Set(
        FClause(List(), List(x2)),
        FClause(List(x1), List(x2)),
        FClause(List(), List(x2, x0)),
        FClause(List(), List(x1, s)),
        FClause(List(x1, s), List()),
        FClause(List(x0), List(r)),
        FClause(List(x0), List(x)),
        FClause(List(q), List(x)),
        FClause(List(p), List(x)),
        FClause(List(x2, x0), List(x1)),
        FClause(List(x, r), List(x0)),
        FClause(List(x), List(p, q))
      )
      cnf._1.toSet must beEqualTo(expected)
    }
  }
}
