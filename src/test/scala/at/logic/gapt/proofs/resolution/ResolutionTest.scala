/*
 * ResolutionTest.scala
 *
 */

package at.logic.gapt.proofs.resolution

import at.logic.gapt.expr.fol.FOLSubstitution
import org.specs2.mutable._

import at.logic.gapt.proofs.resolution.robinson._
import at.logic.gapt.proofs.occurrences._
import at.logic.gapt.expr._
import at.logic.gapt.proofs.lk.base._

class ResolutionTest extends Specification {

  "Paramodulation rule in Robinson Resolution" should {
    "be created correctly" in {
      val cl1 = InitialClause( Nil, FOLAtom( "=", FOLFunction( "+", FOLVar( "x" ) :: FOLVar( "x" ) :: Nil ) :: FOLVar( "x" ) :: Nil ) :: Nil )
      val cl2 = InitialClause( Nil, FOLAtom( "=", FOLFunction( "+", FOLVar( "y" ) :: FOLVar( "y" ) :: Nil ) :: FOLVar( "y" ) :: Nil ) :: Nil )
      val param = Paramodulation( cl1, cl2, cl1.root.succedent( 0 ), cl2.root.succedent( 0 ), FOLAtom( "=", FOLVar( "y" ) :: FOLVar( "y" ) :: Nil ), FOLSubstitution( List( ( FOLVar( "x" ), FOLVar( "y" ) ) ) ) )
      val sq = Seq( FOLAtom( "=", FOLVar( "y" ) :: FOLVar( "y" ) :: Nil ) )

      param.root.positive.map( _.formula ) must beEqualTo( sq )
    }

    "be created correctly -- this test relies on the fact that sub is applied to the inferred formula" in {
      val cl1 = InitialClause( Nil, FOLAtom( "=", FOLFunction( "+", FOLVar( "x" ) :: FOLVar( "x" ) :: Nil ) :: FOLVar( "x" ) :: Nil ) :: Nil )
      val cl2 = InitialClause( Nil, FOLAtom( "=", FOLFunction( "+", FOLVar( "y" ) :: FOLVar( "y" ) :: Nil ) :: FOLVar( "y" ) :: Nil ) :: Nil )
      val param = Paramodulation( cl1, cl2, cl1.root.succedent( 0 ), cl2.root.succedent( 0 ), FOLAtom( "=", FOLVar( "y" ) :: FOLVar( "x" ) :: Nil ), FOLSubstitution( List( ( FOLVar( "x" ), FOLVar( "y" ) ) ) ) )
      val sq = Seq( FOLAtom( "=", FOLVar( "y" ) :: FOLVar( "y" ) :: Nil ) )

      param.root.positive.map( _.formula ) must beEqualTo( sq )
    }

    "correctly keep the context of demodulated formulas " in {
      val P = "P"
      val List( a, b, c, d, e, f ) = List( "a", "b", "c", "d", "e", "f" ) map ( x => FOLConst( x ).asInstanceOf[FOLTerm] )
      val List( e1, e2, e3, p, q ) = List( Eq( a, b ), Eq( c, d ), Eq( e, f ), FOLAtom( P, a :: Nil ), FOLAtom( P, b :: Nil ) )
      val p1 = InitialClause( Nil, List( e1, e2 ) )
      val p2 = InitialClause( Nil, List( e3, p ) )
      val p3 = Paramodulation( p1, p2, p1.root.succedent( 0 ), p2.root.succedent( 1 ), q, FOLSubstitution() )
      val expected_root = HOLSequent( Nil, List( e2, e3, q ) )

      p3.root.toHOLSequent must beSyntacticFSequentEqual( expected_root )

    }
  }
  "extrator on Resolution rule" should {
    "work properly" in {
      val x = FOLVar( "x" )
      val fa = FOLFunction( "f", List( FOLConst( "a" ) ) )
      val Pfa = FOLAtom( "P", List( fa ) )
      val Px = FOLAtom( "P", List( x ) )
      val cl1 = InitialClause( List(), List( Px ) )
      val cl2 = InitialClause( List( Pfa ), List() )
      val res = Resolution( cl1, cl2, cl1.root.succedent( 0 ), cl2.root.antecedent( 0 ), FOLSubstitution( List( ( x, fa ) ) ) )
      res must beLike { case Resolution( _, _, _, _, _, _ ) => ok }
    }
  }

  /* using deprecated data structure. Please update.
  "Andrews Resolution" should {
    "refute 'not (A or not A)'" in {
      val a = HOLAtom(Const("p", To))
      val f = Neg(Or(a, Neg(a))).asInstanceOf[FormulaOccurrence]
      val s = Sequent(Nil, f::Nil)
      val p0 = InitialSequent[SequentOccurrence](s)
      val p1 = NotT( p0, p0.root.succedent.head )
      val p2 = OrFL( p1, p1.root.antecedent.head )
      val p3 = OrFR( p1, p1.root.antecedent.head )
      val p4 = NotF( p3, p3.root.antecedent.head )
      val p5 = Cut( p4, p2, p4.root.succedent.head, p2.root.antecedent.head )
      p5.root.getSequent must beEqualTo(Sequent(Nil, Nil))
    }

    "handle strong quantifiers correctly" in {
      val x = Var("X", i -> o )
      val y = Var("y", i )
      val z = Var("Z", i -> o )
      val args = x::y::z::Nil
      val tp = FunctionType(To, args.map(a => a.exptype))
      val a = HOLAtom(Const("R", tp), args)
      val qa = All( x, a )

      qa.freeVariables must not contain( x )

      val sk = SkolemSymbolFactory.getSkolemSymbol

      // We do not care about the order of arguments. Do we?
      val skt1 = HOLFunction( sk, y::z::Nil, i -> o)
      val skt2 = HOLFunction( sk, z::y::Nil, i -> o)
      val tp1 = FunctionType(To, skt1.exptype::y.exptype::z.exptype::Nil)
      val tp2 = FunctionType(To, skt2.exptype::y.exptype::z.exptype::Nil)
      val ska1 = HOLAtom(Const("R", tp1), skt1::y::z::Nil )
      val ska2 = HOLAtom(Const("R", tp2), skt2::y::z::Nil )

      val p0 = InitialSequent[SequentOccurrence]( Sequent( qa::Nil, Nil ) )
      val p1 = ForallF( p0, p0.root.antecedent.head, sk )

      ska1::ska2::Nil must contain( p1.root.getSequent.antecedent.head )
    }

    "handle weak quantifiers and substitution correctly" in {
      val x = Var("X", i -> o )
      val f = Const("f", (i -> o) -> i )
      val xfx = App(x, App( f, x ) ).asInstanceOf[Formula]
      val m = All( x, xfx )

      val z = Var("z", i)
      val Pz = HOLAtom( Const("P", To -> z.exptype), z::Nil )
      val form = Or(Pz, Neg(Pz))
      val t = Abs( z, form )

      val p0 = InitialSequent[SequentOccurrence]( Sequent( Nil, m::Nil ) )
      val p1 = ForallT( p0, p0.root.succedent.head, x )
      val p2 = Sub( p1, Substitution( x, t ) )

      val newa = HOLAtom( ConstantStringSymbol("P"), App( f, t )::Nil )
      p2.root.getSequent.succedent.head must beEqualTo( 
        Or( newa, Neg( newa ) ) )
    }
  }
*/
}
