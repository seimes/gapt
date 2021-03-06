package at.logic.gapt.proofs.expansionTrees

import at.logic.gapt.expr._
import at.logic.gapt.expr.fol.{ isFOLPrenexSigma1, FOLSubstitution, FOLMatchingAlgorithm }
import at.logic.gapt.expr.hol.{ containsQuantifier, instantiate, isPrenex }
import at.logic.gapt.proofs.lk.base._

/**
 * Extracts the instance terms used in a prenex FOL Pi_1 expansion tree / Sigma_1 expansion sequent.
 *
 * Each expansion tree is transformed into a list of pairs of its shallow formula and a tuple containing the terms
 * used to instantiate the outermost weak quantifier block.
 */
object extractInstanceTerms {
  def apply( expansionTree: ExpansionTree ): Seq[( FOLFormula, Seq[FOLTerm] )] =
    compressQuantifiers( expansionTree ) match {
      case METWeakQuantifier( formula, instanceLists ) =>
        instanceLists map {
          case ( child, instanceTerms ) if !child.containsWeakQuantifiers =>
            ( formula.asInstanceOf[FOLFormula], instanceTerms.map( _.asInstanceOf[FOLTerm] ) )
        }
      case et if !et.containsWeakQuantifiers => Seq( et.toShallow.asInstanceOf[FOLFormula] -> List() )
    }

  def apply( expansionSequent: ExpansionSequent ): Seq[( ( FOLFormula, Seq[FOLTerm] ), Boolean )] =
    expansionSequent.polarizedTrees flatMap { case ( et, pol ) => apply( et ).map( _ -> pol ) }
}

/**
 * Extracts the instances used in a prenex FOL Pi_1 expansion tree / Sigma_1 expansion sequent.
 *
 * Each expansion tree is transformed into a list of instances of its shallow formula.
 *
 * In contrast to [[toDeep]], this function doesn't produce conjunctions of instances, but instead increases the
 * number of formulas in the antecedent/succedent.
 */
object extractInstances {
  def apply( expansionTree: ExpansionTree ): Seq[FOLFormula] =
    if ( !containsQuantifier( toShallow( expansionTree ) ) )
      Seq( toShallow( expansionTree ).asInstanceOf[FOLFormula] )
    else expansionTree match {
      case ETWeakening( _ ) => Seq()
      case ETWeakQuantifier( _, instances ) =>
        instances flatMap { i => extractInstances( i._1 ) }
      case ETStrongQuantifier( _, _, t ) => extractInstances( t )
      case ETSkolemQuantifier( _, _, t ) => extractInstances( t )
    }

  def apply( expansionSequent: ExpansionSequent ): HOLSequent =
    HOLSequent(
      expansionSequent.antecedent flatMap apply,
      expansionSequent.succedent flatMap apply
    )

}

object groundTerms {
  def apply( term: FOLTerm ): FOLTerm =
    FOLSubstitution( freeVariables( term ).
      map { c => FOLVar( c.name ) -> FOLConst( c.name ) }.toSeq )( term )

  def apply( lang: Seq[FOLTerm] ): Seq[FOLTerm] = lang map apply
}

/**
 * Encodes instances of a prenex FOL Sigma_1 end-sequent as FOL terms.
 *
 * In the case of cut-introduction, the end-sequent has no free variables and we're encoding a Herbrand sequent as a
 * set of terms.  A term r_i(t_1,...,t_n) encodes an instance of the formula "forall x_1 ... x_n, phi(x_1,...,x_n)"
 * using the instances (t_1,...,t_n).
 *
 * In the case of simple inductive proofs, the end-sequent contains one free variable (alpha).  Here, we consider
 * proofs of instance sequents, which are obtained by substituting a numeral for alpha.  Hence the formulas occuring
 * in the end-sequents of instance proofs are substitution instances of [[endSequent]]; the encoded terms still only
 * capture the instances used in the instance proofs--i.e. not alpha.
 */
case class InstanceTermEncoding( endSequent: HOLSequent ) {

  require( isFOLPrenexSigma1( endSequent ), s"$endSequent is not a prenex FOL Sigma_1 sequent" )

  /**
   * Formula together with its polarity in a sequent, which is true if it is in the antecedent.
   */
  type PolarizedFormula = ( FOLFormula, Boolean )

  /**
   * Assigns each formula in the end-sequent a fresh function symbol used to encode its instances.
   */
  protected def mkSym( esFormula: PolarizedFormula ) = esFormula match {
    case ( f, true )  => s"{$f}"
    case ( f, false ) => s"-{$f}"
  }

  private def instanceTerms( instance: PolarizedFormula, esFormula: PolarizedFormula ) = ( instance, esFormula ) match {
    case ( ( instf: FOLFormula, true ), ( All.Block( vars, esf ), true ) ) =>
      FOLMatchingAlgorithm.matchTerms( esf, instf ).map { subst => vars.map( subst.apply ) }
    case ( ( instf: FOLFormula, false ), ( Ex.Block( vars, esf ), false ) ) =>
      FOLMatchingAlgorithm.matchTerms( esf, instf ).map { subst => vars.map( subst.apply ) }
    case _ => None
  }

  private def findESFormula( instance: PolarizedFormula ): Option[( PolarizedFormula, List[FOLTerm] )] =
    endSequent.polarizedFormulas.map( _.asInstanceOf[PolarizedFormula] ).
      flatMap { u => instanceTerms( instance, u ).map( u -> _ ) }.headOption

  def encodeOption( instance: PolarizedFormula ): Option[FOLTerm] =
    findESFormula( instance ) map {
      case ( esFormula, terms ) => FOLFunction( mkSym( esFormula ), terms )
    }

  def encode( instance: PolarizedFormula ): FOLTerm = encodeOption( instance ).getOrElse {
    throw new IllegalArgumentException( s"Cannot find $instance in $endSequent" )
  }

  /**
   * Encodes a sequent consisting of instances of an instance sequent.
   */
  def encode( instance: HOLSequent ): Seq[FOLTerm] =
    instance.polarizedFormulas.map { instf => encode( instf.asInstanceOf[PolarizedFormula] ) }

  /**
   * Encodes an expansion sequent (of an instance proof).
   *
   * The shallow formulas of the expansion sequents should be subsumed by formulas in the end-sequent.
   */
  // TODO: actually try to match the shallow formulas, and not the instances.
  def encode( instance: ExpansionSequent )( implicit dummyImplicit: DummyImplicit ): Seq[FOLTerm] =
    encode( extractInstances( instance ) )

  /**
   * Maps a function symbol to its corresponding formula in the end-sequent.
   */
  def findESFormula( sym: String ): Option[PolarizedFormula] =
    endSequent.polarizedFormulas.map( _.asInstanceOf[PolarizedFormula] ).
      find { u => mkSym( u ) == sym }

  /**
   * Decodes a term into its corresponding instance.
   *
   * The resulting instance can contain alpha in the inductive case.
   */
  def decodeOption( term: FOLTerm ): Option[PolarizedFormula] = term match {
    case FOLFunction( f, args ) =>
      findESFormula( f ).map {
        case ( esFormula, pol ) =>
          instantiate( esFormula, args ) -> pol
      }
  }

  def decode( term: FOLTerm ): PolarizedFormula = decodeOption( term ).get

  def decode( terms: Iterable[FOLTerm] ): Set[PolarizedFormula] =
    terms map decode toSet

  def decodeToFSequent( terms: Iterable[FOLTerm] ): HOLSequent = HOLSequent( decode( terms ).toSeq )

  def decodeToExpansionSequent( terms: Iterable[FOLTerm] ): ExpansionSequent = {
    val polExpTrees = terms.map { case FOLFunction( f, args ) => ( findESFormula( f ).get, args ) }.
      groupBy( _._1 ).toSeq.map {
        case ( ( esFormula, pol ), instances ) =>
          val vars = ( esFormula, pol ) match {
            case ( All.Block( vs, _ ), true ) => vs
            case ( Ex.Block( vs, _ ), false ) => vs
          }
          formulaToExpansionTree( esFormula, instances.map( _._2 ).map { terms => FOLSubstitution( ( vars, terms ).zipped.toList ) }.toList, !pol ) -> pol
      }
    ExpansionSequent( polExpTrees.filter( _._2 == true ).map( _._1 ).toList, polExpTrees.filter( _._2 == false ).map( _._1 ).toList )
  }
}
