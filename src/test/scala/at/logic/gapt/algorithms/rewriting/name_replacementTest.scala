package at.logic.gapt.algorithms.rewriting

import org.specs2.mutable._
import at.logic.gapt.expr._
import at.logic.gapt.expr.fol._
import at.logic.gapt.proofs.resolution.robinson._
import at.logic.gapt.proofs.resolution._
import at.logic.gapt.utils.ds.acyclicGraphs.{ BinaryAGraph, UnaryAGraph, LeafAGraph, AGraph }

/**
 * Test for renaming of constant symbols
 */
class name_replacementTest extends Specification {

  val c1 = FOLAtom( "P", FOLFunction( "g", FOLConst( "a" ) :: Nil ) :: Nil )
  val c2 = FOLAtom( "P", FOLFunction( "g", FOLVar( "x" ) :: Nil ) :: Nil )
  val c3 = FOLAtom( "Q", FOLFunction( "f", FOLConst( "ladr0" ) :: Nil ) :: Nil )
  val c4 = FOLAtom( "Q", FOLVar( "x" ) :: Nil )

  val x = FOLVar( "x" )
  val a = FOLConst( "a" )
  val fl = FOLFunction( "f", FOLConst( "ladr0" ) :: Nil )

  val d1 = FOLAtom( "R", FOLFunction( "f", FOLConst( "a" ) :: Nil ) :: Nil )
  val d2 = FOLAtom( "R", FOLFunction( "f", FOLVar( "x" ) :: Nil ) :: Nil )
  val d3 = FOLAtom( "Q", FOLFunction( "h", FOLConst( "c0" ) :: Nil ) :: Nil )
  val d4 = FOLAtom( "Q", FOLVar( "x" ) :: Nil )

  val hc = FOLFunction( "h", FOLConst( "c0" ) :: Nil )

  object proof1 {
    val s1 = FOLSubstitution( Map( x -> a ) )
    val s2 = FOLSubstitution( Map( x -> fl ) )
    val p1 = InitialClause( List( c1, c1 ), List( c3 ) )
    val p2 = InitialClause( Nil, List( c2 ) )
    val p3 = InitialClause( List( c4 ), Nil )
    val p5 = Resolution( p2, p1, p2.root.positive( 0 ), p1.root.negative( 1 ), s1 )
    val p6 = Resolution( p5, p3, p5.root.positive( 0 ), p3.root.negative( 0 ), s2 )
    val p7 = Resolution( p2, p6, p2.root.positive( 0 ), p6.root.negative( 0 ), s1 )
  }

  object proof2 {
    val r1 = FOLSubstitution( Map( x -> a ) )
    val r2 = FOLSubstitution( Map( x -> hc ) )
    val q1 = InitialClause( List( d1, d1 ), List( d3 ) )
    val q2 = InitialClause( Nil, List( d2 ) )
    val q3 = InitialClause( List( d4 ), Nil )
    val q5 = Resolution( q2, q1, q2.root.positive( 0 ), q1.root.negative( 1 ), r1 )
    val q6 = Resolution( q5, q3, q5.root.positive( 0 ), q3.root.negative( 0 ), r2 )
    val q7 = Resolution( q2, q6, q2.root.positive( 0 ), q6.root.negative( 0 ), r1 )
  }

  object proof3 {
    val s1 = FOLSubstitution( Map( x -> a ) )
    val s2 = FOLSubstitution( Map( x -> fl ) )
    val p0 = InitialClause( List( c1, c2 ), List( c3 ) )
    val p1 = Factor( p0, p0.root.negative( 1 ), p0.root.negative( 0 ) :: Nil, FOLSubstitution() )
    val p2 = InitialClause( Nil, List( c2 ) )
    val p3 = InitialClause( List( c4 ), Nil )
    val p5 = Resolution( p2, p1, p2.root.positive( 0 ), p1.root.negative( 0 ), s1 )
    val p6 = Resolution( p5, p3, p5.root.positive( 0 ), p3.root.negative( 0 ), s2 )
  }

  object proof4 {
    //this proof has errors: the factor rule needs a unification
    val r1 = FOLSubstitution( Map( x -> a ) )
    val r2 = FOLSubstitution( Map( x -> hc ) )
    val q0 = InitialClause( List( d1, d2 ), List( d3 ) )
    val q1 = Factor( q0, q0.root.negative( 1 ), q0.root.negative( 0 ) :: Nil, FOLSubstitution() )
    val q2 = InitialClause( Nil, List( d2 ) )
    val q3 = InitialClause( List( d4 ), Nil )
    val q5 = Resolution( q2, q1, q2.root.positive( 0 ), q1.root.negative( 0 ), r1 )
    val q6 = Resolution( q5, q3, q5.root.positive( 0 ), q3.root.negative( 0 ), r2 )

  }

  def checkClause( c: OccClause, d: OccClause ) = c.toHOLSequent multiSetEquals ( d.toHOLSequent )
  def checkTree( r: AGraph[OccClause], o: AGraph[OccClause] ): Option[( AGraph[OccClause], AGraph[OccClause] )] = {
    val pair: ( AGraph[OccClause], AGraph[OccClause] ) = ( r, o )
    pair match {
      case ( LeafAGraph( c ), LeafAGraph( d ) ) =>
        if ( checkClause( c.asInstanceOf[OccClause], d.asInstanceOf[OccClause] ) ) None else Some( ( r, o ) )
      case ( UnaryAGraph( c, p ), UnaryAGraph( d, q ) ) =>
        checkTree( p.asInstanceOf[AGraph[OccClause]], q.asInstanceOf[AGraph[OccClause]] ) match {
          case None =>
            if ( checkClause( c.asInstanceOf[OccClause], d.asInstanceOf[OccClause] ) ) None else Some( ( r, o ) )
          case e @ Some( _ ) => e
        }
      case ( BinaryAGraph( c, p1, p2 ), BinaryAGraph( d, q1, q2 ) ) =>
        checkTree( p1.asInstanceOf[AGraph[OccClause]], q1.asInstanceOf[AGraph[OccClause]] ) match {
          case None =>
            checkTree( p2.asInstanceOf[AGraph[OccClause]], q2.asInstanceOf[AGraph[OccClause]] ) match {
              case None =>
                if ( checkClause( c.asInstanceOf[OccClause], d.asInstanceOf[OccClause] ) ) None else Some( ( r, o ) )
              case Some( e ) => Some( e )
            }
          case Some( e ) => Some( e )
        }
      case _ => Some( ( r, o ) )
    }
  }

  val map: NameReplacement.SymbolMap = Map[String, ( Int, String )](
    "P" -> ( 2, "R" ),
    "f" -> ( 1, "h" ),
    "g" -> ( 2, "f" ),
    "ladr0" -> ( 0, "c0" )
  )

  "The renaming interface " should {
    "rewrite fol formulas" in {
      val p_ladr_fladr = FOLAtom( "P", FOLConst( "ladr0" ) :: FOLFunction( "f", FOLConst( "ladr0" ) :: Nil ) :: Nil )
      val p_a_ladr = FOLAtom( "P", FOLConst( "a" ) :: FOLConst( "ladr0" ) :: Nil )
      val q_gx = FOLAtom( "Q", FOLFunction( "g", FOLVar( "x" ) :: Nil ) :: Nil )

      val fol1 = And( p_ladr_fladr, Or( Neg( p_a_ladr ), q_gx ) )

      val r_c_hc = FOLAtom( "R", FOLConst( "c0" ) :: FOLFunction( "h", FOLConst( "c0" ) :: Nil ) :: Nil )
      val r_a_c = FOLAtom( "R", FOLConst( "a" ) :: FOLConst( "c0" ) :: Nil )

      val fol1_ = And( r_c_hc, Or( Neg( r_a_c ), q_gx ) )

      fol1_ must beEqualTo( fol1.renameSymbols( map ) )
    }

    "rewrite hol formulas" in {
      val P = Const( "P", Ti -> ( Ti -> To ) )
      val Q = Const( "Q", Ti -> To )
      val R = Const( "R", Ti -> ( Ti -> To ) )
      val f = Const( "f", Ti -> Ti )
      val g = Const( "g", Ti -> Ti )
      val h = Const( "h", Ti -> Ti )

      val ladr = Const( "ladr0", Ti )
      val c0 = Const( "c0", Ti )

      val p_ladr_fladr = HOLAtom( P, ladr :: HOLFunction( f, ladr :: Nil ) :: Nil )
      val p_a_ladr = HOLAtom( P, Const( "a", Ti ) :: ladr :: Nil )
      val q_gx = HOLAtom( Q, HOLFunction( g, Var( "x", Ti ) :: Nil ) :: Nil )

      val fol1 = And( p_ladr_fladr, Or( Neg( p_a_ladr ), q_gx ) ).asInstanceOf[FOLFormula]

      val r_c_hc = HOLAtom( R, c0 :: HOLFunction( h, c0 :: Nil ) :: Nil )
      val r_a_c = HOLAtom( R, Const( "a", Ti ) :: c0 :: Nil )

      val fol1_ = And( r_c_hc, Or( Neg( r_a_c ), q_gx ) )

      fol1_ must beEqualTo( fol1.renameSymbols( map ) )
    }

    "rewrite of resolution proofs must work" in {
      //      println(proof1.p7)
      //      println(proof2.q7)
      val map: NameReplacement.SymbolMap = Map[String, ( Int, String )](
        "P" -> ( 1, "R" ),
        "f" -> ( 1, "h" ),
        "g" -> ( 1, "f" ),
        "ladr0" -> ( 0, "c0" )
      )

      val ( _, proof ) = NameReplacement.rename_resproof( proof1.p7, map )
      //println
      //proof4.q0.root.negative map println
      //println
      //proof4.q1.root.negative map println
      //println
      //(proof4.q1.root.negative diff proof4.q0.root.negative) map println
      //println

      //def find_lost_ancestors()

      //proof4.q1 match { case Factor(c,p,aux,s) => aux map println}

      checkTree( proof, proof2.q7 ) must beEmpty

      val ( _, fproof ) = NameReplacement.rename_resproof( proof3.p6, map )
      //fproof.nodes map println
      checkTree( fproof, proof4.q6 ) must beEmpty
    }
  }
}
