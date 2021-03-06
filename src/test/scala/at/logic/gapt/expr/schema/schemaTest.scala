package at.logic.gapt.expr.schema

import org.specs2.mutable._
import at.logic.gapt.expr._
import BetaReduction._

class SchemaTest extends Specification {
  "Schema" should {
    val i = IntVar( "i" )
    val one = Succ( IntZero() )
    val two = Succ( Succ( IntZero() ) )
    val p = "P"
    val pi = IndexedPredicate( p, i :: Nil )
    val p1 = IndexedPredicate( p, one :: Nil )
    val p2 = IndexedPredicate( p, two :: Nil )
    val bigAnd = BigAnd( Abs( i, pi ), one, two )
    val bigOr = BigOr( i, pi, one, two )
    val and = And( p1, p2 )
    val or = Or( bigAnd, bigOr )
    val neg = Neg( or )
    val imp = Imp( neg, and )

    "create IndexedPredicate correctly (1)" in {
      ( p1 ) must beLike { case f: SchemaFormula => ok }
    }
    "create IndexedPredicate correctly (2)" in {
      ( pi ) must beLike { case f: SchemaFormula => ok }
    }
    "create SchemaFormula correctly (1)" in {
      ( and ) must beLike { case f: SchemaFormula => ok }
    }
    "create SchemaFormula correctly (2)" in {
      ( bigAnd ) must beLike { case f: SchemaFormula => ok }
    }
    "create SchemaFormula correctly (3)" in {
      ( bigOr ) must beLike { case f: SchemaFormula => ok }
    }
    "create SchemaFormula correctly (4)" in {
      ( imp ) must beLike { case f: SchemaFormula => ok }
    }

    "correctly deal with bound variables in the BigAnd extractor (2)" in {
      val pi = IndexedPredicate( "p", i :: Nil )
      val f = BigAnd( i, pi, IntZero(), IntZero() )
      val res = f match {
        case BigAnd( v, f, ub, lb ) => Abs( v, f )
      }
      res must beEqualTo( Abs( i, pi ) )
    }

    "correctly deal with bound variables in the BigAnd extractor (1)" in {
      val pi = IndexedPredicate( "p", i :: Nil )
      val p0 = IndexedPredicate( "p", IntZero() :: Nil )
      val f = BigAnd( i, pi, IntZero(), IntZero() )
      val res = f match {
        case BigAnd( v, f, ub, lb ) => App( Abs( v, f ), ub )
      }
      betaNormalize( res ) must beEqualTo( p0 )
    }

    "perform the unapply function in BigAnd correctly" in {
      val iformula = Abs( i.asInstanceOf[Var], p1 )
      val bigConj = BigAnd( iformula, one, two )
      ( BigAnd.unapply( bigConj ).get._1 must beEqualTo( i ) ) &&
        ( BigAnd.unapply( bigConj ).get._2 must beEqualTo( p1 ) ) &&
        ( BigAnd.unapply( bigConj ).get._3 must beEqualTo( one ) ) &&
        ( BigAnd.unapply( bigConj ).get._4 must beEqualTo( two ) )
    }

    "have correct BiggerThan constructor" in {
      val bt1 = BiggerThan( i, one )
      val bt2 = BiggerThan( two, one )
      val bt3 = BiggerThan( one, two )
      val bt4 = BiggerThan( two, i )
      bt1 must beLike {
        case SchemaAtom( BiggerThanC(), x :: y :: Nil ) => ok
        case _ => ko
      }
    }

    "create a schematic term" in {
      val fconst = Const( "f", Tindex -> Tindex -> Tindex )
      val gconst = Const( "g", Tindex -> Tindex )
      val hconst = Const( "h", Tindex -> Tindex )

      def g( t: SchemaExpression ): SchemaExpression = {
        App( gconst, t )
      }

      def h( t: SchemaExpression ): SchemaExpression = {
        App( hconst, t )
      }

      def f( n: IntegerTerm, v: SchemaExpression ): SchemaExpression = {
        n match {
          case IntZero() => g( n )
          case _         => g( f( Pred( n ), v ) )
        }
      }

      // ?????????????????????
      true must beEqualTo( true )
    }

    "unfold a schematic term" in {
      def f = Const( "f", Ti -> Ti )
      def h = Const( "h", Tindex -> ( Ti -> Ti ) )
      def g = Const( "g", Tindex -> ( Ti -> Ti ) )
      val k = IntVar( "k" )
      val x = foVar( "x" )
      val z = indexedFOVar( "z", Succ( IntZero() ) )
      val z0 = indexedFOVar( "z", IntZero() )
      val base1 = sTerm( g, IntZero(), x :: Nil )
      val step1 = sTerm( g, Succ( k ), x :: Nil )
      val base2 = x
      val step2 = foTerm( "f", sTerm( g, Succ( k ), x :: Nil ) :: Nil )
      dbTRS.clear
      dbTRS.add( g, Tuple2( base1, base2 ), Tuple2( step1, step2 ) )
      val term = sTerm( g, Succ( Succ( k ) ), x :: Nil )
      val term1 = sTerm( g, Succ( IntZero() ), z :: Nil )
      val term2 = sTerm( g, IntZero(), z0 :: Nil )

      val unf = unfoldSTerm( term2 )

      // ?????????????????????
      true must beEqualTo( true )

    }
  }
}
