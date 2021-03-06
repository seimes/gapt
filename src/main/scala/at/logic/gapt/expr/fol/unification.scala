package at.logic.gapt.expr.fol

import at.logic.gapt.expr._
import at.logic.gapt.proofs.lk.base.HOLSequent

/**
 * The interface for an unification algorithm of finitary type, i.e.
 * one where the complete set of most general unifiers is finite.
 */
trait FinitaryUnification {
  /**
   * Calculates the complete set of most general unifiers of the terms term1 and term2.
   * @param term1 one of the terms to unify. formulas are also allowed, so we accept FOL expressions
   * @param term2 one of the terms to unify. formulas are also allowed, so we accept FOL expressions
   * @return a list of mgus, the empty list means that term1 and term2 are not unifiable.
   */
  def unify( term1: FOLExpression, term2: FOLExpression ): List[FOLSubstitution]
}

trait UnificationAlgorithm extends FinitaryUnification

class UnificationException( msg: String ) extends Exception( msg )

/**
 * Created by sebastian on 2/9/15.
 */
object FOLUnificationAlgorithm extends UnificationAlgorithm {

  def unify( seq1: HOLSequent, seq2: HOLSequent ): List[FOLSubstitution] = {
    require( ( seq1.antecedent ++ seq1.succedent ++ seq2.antecedent ++ seq2.succedent ).forall( _.isInstanceOf[FOLFormula] ) )

    val formseq1 = Or( ( seq1.antecedent.map( x => Neg( x.asInstanceOf[FOLFormula] ) ) ++ seq1.succedent.map( x => x.asInstanceOf[FOLFormula] ) ).toList )
    val formseq2 = Or( ( seq2.antecedent.map( x => Neg( x.asInstanceOf[FOLFormula] ) ) ++ seq2.succedent.map( x => x.asInstanceOf[FOLFormula] ) ).toList )

    unify( formseq1, formseq2 )
  }

  def unify( term1: FOLExpression, term2: FOLExpression ): List[FOLSubstitution] =
    unifySetOfTuples( Tuple2( term1.asInstanceOf[FOLExpression], term2.asInstanceOf[FOLExpression] ) :: Nil, Nil ) match {
      case Some( ( Nil, ls ) ) => List( FOLSubstitution( ls.map( x => ( x._1.asInstanceOf[FOLVar], x._2.asInstanceOf[FOLTerm] ) ) ) )
      case _                   => Nil
    }

  def applySubToListOfPairs( l: List[Tuple2[FOLExpression, FOLExpression]], s: FOLSubstitution ): List[Tuple2[FOLExpression, FOLExpression]] = {
    return l.map( a => ( s.apply( a._1 ), s.apply( a._2 ) ) )
  }

  def isSolvedVarIn( x: FOLVar, l: List[Tuple2[FOLExpression, FOLExpression]] ): Boolean = {
    for ( term <- ( ( l.map( ( a ) => a._1 ) ) ::: ( l.map( ( a ) => a._2 ) ) ) )
      if ( getVars( term ).contains( x ) )
        false
    true
  }

  def getVars( f: FOLExpression ): List[FOLVar] = f match {
    case FOLConst( _ ) | Top() | Bottom() => Nil
    case FOLVar( x )                      => f.asInstanceOf[FOLVar] :: Nil
    case FOLFunction( _, args )           => args.flatMap( a => getVars( a ) )
    case FOLAtom( _, args )               => args.flatMap( a => getVars( a ) )
    case Neg( f )                         => getVars( f )
    case And( l, r )                      => getVars( l ) ++ getVars( r )
    case Or( l, r )                       => getVars( l ) ++ getVars( r )
    case Imp( l, r )                      => getVars( l ) ++ getVars( r )
  }

  def unifySetOfTuples( s1: List[( FOLExpression, FOLExpression )], s2: List[( FOLExpression, FOLExpression )] ): Option[( List[( FOLExpression, FOLExpression )], List[( FOLExpression, FOLExpression )] )] =
    ( s1, s2 ) match {
      case ( ( ( a1, a2 ) :: s ), s2 ) if a1 == a2 => unifySetOfTuples( s, s2 )

      case ( ( FOLConst( name1 ), FOLConst( name2 ) ) :: s, s2 ) if name1 != name2 => None

      case ( ( ( FOLFunction( f1, args1 ), FOLFunction( f2, args2 ) ) :: s ), s2 ) if args1.length == args2.length && f1 == f2 => unifySetOfTuples( args1.zip( args2 ) ::: s, s2 )

      case ( ( ( FOLAtom( f1, args1 ), FOLAtom( f2, args2 ) ) :: s ), s2 ) if args1.length == args2.length && f1 == f2 => unifySetOfTuples( args1.zip( args2 ) ::: s, s2 )

      case ( ( ( Neg( f1 ), Neg( f2 ) ) :: s ), s2 ) => unifySetOfTuples( ( f1, f2 ) :: s, s2 )

      case ( ( ( x: FOLVar, v: FOLTerm ) :: s ), s2 ) if !getVars( v ).contains( x ) => {
        val sub = FOLSubstitution( x, v )
        unifySetOfTuples( applySubToListOfPairs( s, sub ), ( x, v ) :: applySubToListOfPairs( s2, sub ) )
      }

      case ( ( ( v: FOLTerm, x: FOLVar ) :: s ), s2 ) if !getVars( v ).contains( x ) => {
        val sub = FOLSubstitution( x, v )
        unifySetOfTuples( applySubToListOfPairs( s, sub ), ( x, v ) :: applySubToListOfPairs( s2, sub ) )
      }

      case ( Nil, s2 ) => Some( ( Nil, s2 ) )

      case ( ( And( l1, r1 ), And( l2, r2 ) ) :: s, set2 ) => unifySetOfTuples( ( l1, l2 ) :: ( r1, r2 ) :: s, set2 )

      case ( ( Or( l1, r1 ), Or( l2, r2 ) ) :: s, set2 ) => unifySetOfTuples( ( l1, l2 ) :: ( r1, r2 ) :: s, set2 )

      case ( ( Imp( l1, r1 ), Imp( l2, r2 ) ) :: s, set2 ) => unifySetOfTuples( ( l1, l2 ) :: ( r1, r2 ) :: s, set2 )

      case _ => None
    }
}
