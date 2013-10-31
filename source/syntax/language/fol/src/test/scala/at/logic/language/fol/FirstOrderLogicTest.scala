/*
 * FirstOrderLogicTest.scala
 */

package at.logic.language.fol

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import at.logic.language.lambda.types._
import at.logic.language.hol

@RunWith(classOf[JUnitRunner])
class FirstOrderLogicTest extends SpecificationWithJUnit {
  "FirstOrderLogic" should {
    "construct correctly an atom formula P(x,f(y),c)" in {
      val List( p, x,y,f,c ) = List("P","x","y","f","c")
      val Pc = FOLConst( p, (Ti -> (Ti -> (Ti -> To))) )
      try {
      Atom( p, FOLVar(x)::Function(f,FOLVar(y)::Nil)::FOLConst(c)::Nil ) must beLike {
        case FOLApp( FOLApp( FOLApp( Pc, FOLVar(x) ), FOLApp( fc, FOLVar(y) ) ), FOLConst(c) ) => ok
      }
      } catch {
        case e : Exception =>
          println(e.getMessage)
          e.printStackTrace
          ko
      }
    }
    "constructs correctly an atom using the factory" in {
      val var1 = FOLVar("x1")
      val const1 = FOLConst("c1")
      val var2 = FOLVar("x2")
      val args = var1::var2::const1::Nil
      val atom1 = Atom("A", args)
      val var3 = Atom("X3")
      val and1 = And(atom1, var3)
      true
    }
    "constructs correctly a forall using the factory" in {
      val var1 = FOLVar("x1")
      val const1 = FOLConst("c1")
      val var2 = FOLVar("x2")
      val args = var1::var2::const1::Nil
      val atom1 = Atom("A",args)
      val all1 = AllVar(var1, atom1)
      true
    }

    "alpha equality as default provides that ∀x.∀x.P(x) is equal to ∀y.∀y.P(y)" in {
      val x = FOLVar("x")
      val y = FOLVar("y")
      val p = "P"
      val px = Atom(p,List(x))
      val py = Atom(p,List(y))
      val allall_px = AllVar(x, AllVar(x, px))
      val allall_py = AllVar(y, AllVar(y, py))

      allall_px must beEqualTo (allall_py)
    }
  }

  "First Order Formula matching" should {
    "not allow P and P match as an Atom " in {
      val ps = "P"
      val f = And(Atom(ps), Atom(ps))

      f must beLike {
        case Atom(_,_) => ko
        case AllVar(_,_) => ko
        case ExVar(_,_) => ko
        case Or(_,_) => ko
        case Imp(_,_) => ko
        case And(_,_) => ok
        case _ => ko
      }
    }
    "match Equation with Atom" in {
      val a = FOLConst("a").asInstanceOf[FOLTerm]
      val b = FOLConst("b").asInstanceOf[FOLTerm]
      val eq = Equation(a, b)

      eq must beLike {
        case Atom(_,_) => ok
        case _ => ko
      }
    }
  }

  "First order formulas matching against higher order contructors" should {
    "work for propositional logical operators" in {
      val List(x,y) = List("x","y") map (FOLVar(_))
      val p = "P"
      val pab = Atom(p, List(x,y))

      And(pab,pab) match {
        case hol.And(a,b) =>
          a mustEqual(pab)
          b mustEqual(pab)
        case _ => ko("FOL Conjunction did not match against HOL Conjunction!")
      }

      Or(pab,pab) match {
        case hol.Or(a,b) =>
          a mustEqual(pab)
          b mustEqual(pab)
        case _ => ko("FOL Disjunction did not match against HOL Conjunction!")
      }

      Neg(pab) match {
        case hol.Neg(a) =>
          a mustEqual(pab)
        case _ => ko("FOL Negation did not match against HOL Conjunction!")
      }
    }

    "work for quantifiers" in {
      val List(a,b) = List("a","b") map (FOLConst(_))
      val List(x,y) = List("x","y") map (FOLVar(_))
      val p = "P"
      val pab = Atom(p, List(a,b))

      AllVar(x,pab) match {
        case hol.AllVar(v,f) =>
          v mustEqual(x)
          f mustEqual(pab)
        case _ => ko("FOL AllVar did not match against HOL Conjunction!")
      }

      ExVar(x,pab) match {
        case hol.ExVar(v,f) =>
          v mustEqual(x)
          f mustEqual(pab)
        case hol.Ex(_,_) =>
          ko("!!!!!!!!!")
        case Ex(_) =>
          ko("+++++")
        case _ => ko("FOL ExVar did not match against HOL Conjunction!")
      }
    }

  }

}
