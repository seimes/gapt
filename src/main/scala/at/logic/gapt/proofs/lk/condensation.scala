package at.logic.gapt.proofs.lk

import at.logic.gapt.expr._
import at.logic.gapt.proofs.lk.base.HOLSequent

/**
 * Condensation implements the redundancy optimization technique of the same name, see also
 * Leitsch: The Resolution Calculus chapter 3.2
 */
object condensation extends condensation
class condensation {
  //TODO: implement

}

/**
 * Factoring removes duplicate literals from fsequents
 */
object factoring extends factoring
class factoring {
  def apply( fs: HOLSequent ): HOLSequent = {
    val ant = fs.antecedent.foldLeft( List[HOLFormula]() )( ( a_, f ) => if ( a_.contains( f ) ) a_ else f :: a_ )
    val suc = fs.succedent.foldLeft( List[HOLFormula]() )( ( a_, f ) => if ( a_.contains( f ) ) a_ else f :: a_ )
    HOLSequent( ant.reverse, suc.reverse )
  }

  def apply( l: List[HOLSequent] ): List[HOLSequent] = l.map( factoring.apply )
}
