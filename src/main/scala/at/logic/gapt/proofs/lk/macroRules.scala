/*
 * macroRules.scala
 */

package at.logic.gapt.proofs.lk

import at.logic.gapt.expr._
import at.logic.gapt.expr.hol.{ isPrenex, instantiate, HOLPosition }
import at.logic.gapt.proofs.expansionTrees._
import at.logic.gapt.proofs.lk.base._
import at.logic.gapt.proofs.occurrences._
import at.logic.gapt.utils.ds.trees._
import at.logic.gapt.utils.logging.Logger
import scala.collection.mutable.ListBuffer

trait MacroRuleLogger extends Logger {
  override def loggerName = "MacroRuleLogger"
}

object AndLeftRule {
  /**
   * <pre>Replaces a formulas A, B (marked by term1oc & term2oc) with the conjunction
   * A ∧ B in the antecedent of a sequent.
   *
   * The rule:
   *     (rest of s1)
   *     sL, A, B |- sR
   * ------------------- (AndLeft)
   * sL, A ∧ B |- sR
   * </pre>
   *
   * @param s1 The top proof with (sL, A, B |- sR) as the bottommost sequent.
   * @param term1oc The occurrence of A in the antecedent of s1.
   * @param term2oc The occurrence of B in the antecedent of s2.
   * @return An LK Proof ending with the new inference.
   */
  def apply( s1: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence ) = {
    val p0 = AndLeft1Rule( s1, term1oc, term2oc.formula.asInstanceOf[HOLFormula] )
    val p1 = AndLeft2Rule( p0, term1oc.formula.asInstanceOf[HOLFormula], p0.getDescendantInLowerSequent( term2oc ).get )
    ContractionLeftRule( p1, p1.prin.head, p1.getDescendantInLowerSequent( p0.prin.head ).get )
  }

  /**
   * <pre>Replaces a formulas term1, term2 with the conjunction
   * term1 ∧ term2 in the antecedent of a sequent.
   *
   * The rule:
   *     (rest of s1)
   * sL, term1, term2 |- sR
   * ---------------------- (AndLeft)
   * sL, term1 ∧ term2 |- sR
   * </pre>
   *
   * @param s1 The top proof with (sL, term1, term2 |- sR) as the bottommost sequent.
   * @param term1 The first formula to be replaced in the antecedent of s1.
   * @param term2 The second formula to be replaced in the antecedent of s2.
   * @return An LK Proof ending with the new inference.
   */
  def apply( s1: LKProof, term1: HOLFormula, term2: HOLFormula ): UnaryTree[OccSequent] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
    val x1 = s1.root.antecedent.find( _.formula == term1 )
    if ( x1 == None )
      throw new LKRuleCreationException( "Not matching formula occurrences found for application of the rule with the given formula" )
    val x2 = s1.root.antecedent.find( x => x.formula == term2 && x != x1.get )
    if ( x2 == None )
      throw new LKRuleCreationException( "Not matching formula occurrences found for application of the rule with the given formula" )
    apply( s1, x1.get, x2.get )
  }
}

object OrRightRule {

  /**
   * <pre>Replaces a formulas A, B (marked by term1oc & term2oc) with the disjunction
   * A ∨ B in the succedent of a sequent.
   *
   * The rule:
   *     (rest of s1)
   *   sL|- sR, A, B
   * ------------------- (OrRight)
   * sL |- sR, A ∨ B
   * </pre>
   *
   * @param s1 The top proof with (sL |- sR, A, B) as the bottommost sequent.
   * @param term1oc The occurrence of A in the succedent of s1.
   * @param term2oc The occurrence of B in the succedent of s2.
   * @return An LK Proof ending with the new inference.
   */
  def apply( s1: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence ) = {
    val p0 = OrRight1Rule( s1, term1oc, term2oc.formula )
    val p1 = OrRight2Rule( p0, term1oc.formula, p0.getDescendantInLowerSequent( term2oc ).get )
    ContractionRightRule( p1, p1.prin.head, p1.getDescendantInLowerSequent( p0.prin.head ).get )
  }

  /**
   * <pre>Replaces a formulas term1, term2 with the disjunction
   * term1 ∨ term2 in the succedent of a sequent.
   *
   * The rule:
   *     (rest of s1)
   * sL |- sR, term1, term2
   * ---------------------- (OrRight)
   * sL |- sR, term1 ∨ term2
   * </pre>
   *
   * @param s1 The top proof with (sL |- sR, term1, term2) as the bottommost sequent.
   * @param term1 The first formula to be replaced in the succedent of s1.
   * @param term2 The second formula to be replaced in the succedent of s2.
   * @return An LK Proof ending with the new inference.
   */
  def apply( s1: LKProof, term1: HOLFormula, term2: HOLFormula ): UnaryTree[OccSequent] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
    val x1 = s1.root.succedent.find( _.formula == term1 )
    if ( x1 == None )
      throw new LKRuleCreationException( "Not matching formula occurrences found for application of the rule with the given formula" )
    val x2 = s1.root.succedent.find( x => x.formula == term2 && x != x1.get )
    if ( x2 == None )
      throw new LKRuleCreationException( "Not matching formula occurrences found for application of the rule with the given formula" )
    apply( s1, x1.get, x2.get )
  }
}

object TransRule {
  /**
   * <pre>Performs a proof employing transitivity.
   *
   * Takes a proof s2 with end-sequent of the form
   * (x=z), Trans, ... |- ...
   * and return one with end-sequent of the form
   * (x=y), (y=z), Trans, ... |- ...
   * where Trans is defined as Forall xyz.((x=y ∧ y=z) -> x=z)
   * </pre>
   * @param x X
   * @param y Y
   * @param z Z
   * @param s2 The proof which contains the (x=z) which is to be shown.
   * @return A proof wich s2 as a subtree and the formula (x=z) replaced by (x=y) and (y=z).
   */
  def apply( x: FOLTerm, y: FOLTerm, z: FOLTerm, s2: LKProof ): LKProof = {

    val xv = FOLVar( "x" )
    val yv = FOLVar( "y" )
    val zv = FOLVar( "z" )

    //Forall xyz.(x = y ^ y = z -> x = z)
    val Trans = All( xv, All( yv, All( zv, Imp( And( Eq( xv, yv ), Eq( yv, zv ) ), Eq( xv, zv ) ) ) ) )
    def TransX( x: FOLTerm ) = All( yv, All( zv, Imp( And( Eq( x, yv ), Eq( yv, zv ) ), Eq( x, zv ) ) ) )
    def TransXY( x: FOLTerm, y: FOLTerm ) = All( zv, Imp( And( Eq( x, y ), Eq( y, zv ) ), Eq( x, zv ) ) )
    def TransXYZ( x: FOLTerm, y: FOLTerm, z: FOLTerm ) = Imp( And( Eq( x, y ), Eq( y, z ) ), Eq( x, z ) )

    val xy = Eq( x, y )
    val yz = Eq( y, z )
    val xz = Eq( x, z )

    val ax_xy = Axiom( xy :: Nil, xy :: Nil )
    val ax_yz = Axiom( yz :: Nil, yz :: Nil )

    val s1 = AndRightRule( ax_xy, ax_yz, xy, yz )

    val imp = ImpLeftRule( s1, s2, And( xy, yz ), xz )

    val allQZ = ForallLeftRule( imp, TransXYZ( x, y, z ), TransXY( x, y ), z )
    val allQYZ = ForallLeftRule( allQZ, TransXY( x, y ), TransX( x ), y )
    val allQXYZ = ForallLeftRule( allQYZ, TransX( x ), Trans, x )

    ContractionLeftRule( allQXYZ, Trans )
  }
}

object ExistsRightBlock {
  /**
   * <pre>Applies the ExistsRight-rule n times.
   * This method expects a formula main with
   * a quantifier block, and a proof s1 which has a fully
   * instantiated version of main on the left side of its
   * bottommost sequent.
   *
   * The rule:
   *   (rest of s1)
   *  sL |- A[x1\term1,...,xN\termN], sR
   * ---------------------------------- (ExistsRight x n)
   *     sL |- Exists x1,..,xN.A, sR
   * </pre>
   *
   * @param s1 The top proof with (sL |- A[x1\term1,...,xN\termN], sR) as the bottommost sequent.
   * @param main A formula of the form (Exist x1,...,xN.A).
   * @param terms The list of terms with which to instantiate main. The caller of this
   * method has to ensure the correctness of these terms, and, specifically, that
   * A[x1\term1,...,xN\termN] indeed occurs at the bottom of the proof s1.
   */
  def apply( s1: LKProof, main: HOLFormula, terms: Seq[LambdaExpression] ): LKProof = {
    val partiallyInstantiatedMains = ( 0 to terms.length ).toList.reverse.map( n => instantiate( main, terms.take( n ) ) ).toList

    //partiallyInstantiatedMains.foreach(println)

    val series = terms.reverse.foldLeft( ( s1, partiallyInstantiatedMains ) ) { ( acc, ai ) =>
      /*println("MACRORULES|FORALLLEFTBLOCK|APPLYING FORALLEFT")
        println("s1: " + acc._1)
        println("aux: " + acc._2.head)
        println("main: " + acc._2.tail.head)
        println("term: " + ai)*/
      ( ExistsRightRule( acc._1, acc._2.head, acc._2.tail.head, ai ), acc._2.tail )
    }

    series._1
  }
}

object ForallLeftBlock {
  /**
   * <pre>Applies the ForallLeft-rule n times.
   * This method expects a formula main with
   * a quantifier block, and a proof s1 which has a fully
   * instantiated version of main on the left side of its
   * bottommost sequent.
   *
   * The rule:
   *   (rest of s1)
   *  sL, A[x1\term1,...,xN\termN] |- sR
   * ---------------------------------- (ForallLeft x n)
   *     sL, Forall x1,..,xN.A |- sR
   * </pre>
   *
   * @param s1 The top proof with (sL, A[x1\term1,...,xN\termN] |- sR) as the bottommost sequent.
   * @param main A formula of the form (Forall x1,...,xN.A).
   * @param terms The list of terms with which to instantiate main. The caller of this
   * method has to ensure the correctness of these terms, and, specifically, that
   * A[x1\term1,...,xN\termN] indeed occurs at the bottom of the proof s1.
   */
  def apply( s1: LKProof, main: HOLFormula, terms: Seq[LambdaExpression] ): LKProof = {
    val partiallyInstantiatedMains = ( 0 to terms.length ).toList.reverse.map( n => instantiate( main, terms.take( n ) ) ).toList

    //partiallyInstantiatedMains.foreach(println)

    val series = terms.reverse.foldLeft( ( s1, partiallyInstantiatedMains ) ) { ( acc, ai ) =>
      /*println("MACRORULES|FORALLLEFTBLOCK|APPLYING FORALLEFT")
        println("s1: " + acc._1)
        println("aux: " + acc._2.head)
        println("main: " + acc._2.tail.head)
        println("term: " + ai)*/
      ( ForallLeftRule( acc._1, acc._2.head, acc._2.tail.head, ai ), acc._2.tail )
    }

    series._1
  }
}

object ForallRightBlock {
  /**
   * <pre>Applies the ForallRight-rule n times.
   * This method expects a formula main with
   * a quantifier block, and a proof s1 which has a fully
   * instantiated version of main on the right side of its
   * bottommost sequent.
   *
   * The rule:
   *   (rest of s1)
   *  sL |- sR, A[x1\y1,...,xN\yN]
   * ---------------------------------- (ForallRight x n)
   *     sL |- sR, Forall x1,..,xN.A
   *
   * where y1,...,yN are eigenvariables.
   * </pre>
   *
   * @param s1 The top proof with (sL |- sR, A[x1\y1,...,xN\yN]) as the bocttommost sequent.
   * @param main A formula of the form (Forall x1,...,xN.A).
   * @param eigenvariables The list of eigenvariables with which to instantiate main. The caller of this
   * method has to ensure the correctness of these terms, and, specifically, that
   * A[x1\y1,...,xN\yN] indeed occurs at the bottom of the proof s1.
   */
  def apply( s1: LKProof, main: HOLFormula, eigenvariables: Seq[Var] ): LKProof = {
    val partiallyInstantiatedMains = ( 0 to eigenvariables.length ).toList.reverse.map( n => instantiate( main, eigenvariables.take( n ) ) ).toList

    //partiallyInstantiatedMains.foreach(println)

    val series = eigenvariables.reverse.foldLeft( ( s1, partiallyInstantiatedMains ) ) { ( acc, ai ) =>
      /*println("MACRORULES|FORALLRIGHTBLOCK|APPLYING FORALLEFT")
        println("s1: " + acc._1)
        println("aux: " + acc._2.head)
        println("main: " + acc._2.tail.head)
        println("term: " + ai)*/
      ( ForallRightRule( acc._1, acc._2.head, acc._2.tail.head, ai ), acc._2.tail )
    }

    series._1
  }
}

/**
 * This macro rule unifies [[EquationLeft1Rule]] and [[EquationLeft2Rule]] by automatically choosing the appropriate rule.
 *
 */
object EquationLeftRule extends EquationRuleLogger {

  /**
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the antecedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param pos A position such that A(pos) = s or A(pos) = t
   * @return A proof ending with either an [[EquationLeft1Rule]] or an [[EquationLeft2Rule]] according to which replacement is sensible.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, pos: HOLPosition ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas with TermPositions = {
    val ( eqocc, auxocc ) = getTerms( s1.root, s2.root, term1oc, term2oc )
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )
        val aux = auxocc.formula
        val term = aux.get( pos )

        term match {
          case Some( `s` ) => EquationLeft1Rule( s1, s2, term1oc, term2oc, pos )

          case Some( `t` ) => EquationLeft2Rule( s1, s2, term1oc, term2oc, pos )

          case Some( x ) =>
            throw new LKRuleCreationException( "Wrong term " + x + " in auxiliary formula " + aux + " at position " + pos + "." )

          case None =>
            throw new LKRuleCreationException( "Position " + pos + " is not well-defined for formula " + aux + "." )
        }

      case _ =>
        throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }

  /**
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the antecedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param main A formula A' such that A' is obtained by replacing one occurrence of s in A by t or vice versa.
   * @return A proof ending with either an [[EquationLeft1Rule]] or an [[EquationLeft2Rule]] according to which one leads from A to A'.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas with TermPositions = {
    val ( eqocc, auxocc ) = getTerms( s1.root, s2.root, term1oc, term2oc )
    val aux = auxocc.formula
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )

        if ( s == t && aux == main ) {
          debug( "Producing equation rule with trivial equation." )
          EquationLeft1Rule( s1, s2, term1oc, term2oc, main )
        } else if ( s == t && aux != main ) {
          throw new LKRuleCreationException( "Eq is trivial, but aux formula " + aux + " and main formula " + main + "differ." )
        } else if ( s != t && aux == main ) {
          throw new LKRuleCreationException( "Nontrivial equation, but aux and main formula are equal." )
        } else {
          val sAux = aux.find( s )
          val sMain = main.find( s )

          val tAux = aux.find( t )
          val tMain = main.find( t )

          if ( sAux.isEmpty && tAux.isEmpty )
            throw new LKRuleCreationException( "Neither " + s + " nor " + t + " found in formula " + aux + "." )

          trace( "Positions of s = " + s + " in aux = " + aux + ": " + sAux + "." )
          trace( "Positions of s = " + s + " in main = " + main + ": " + sMain + "." )

          trace( "Positions of t = " + t + " in aux = " + aux + ": " + tAux + "." )
          trace( "Positions of t = " + t + " in main = " + main + ": " + tMain + "." )

          val tToS = sMain intersect tAux
          val sToT = tMain intersect sAux
          trace( "tToS = " + tToS )
          trace( "sToT = " + sToT )

          if ( sToT.length == 1 && tToS.length == 0 ) {
            val p = sToT.head
            val mainNew = HOLPosition.replace( aux, p, t )
            if ( mainNew == main ) {
              EquationLeft1Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + t + ") should yield " + main + " but is " + mainNew + "." )
          } else if ( tToS.length == 1 && sToT.length == 0 ) {
            val p = tToS.head
            val mainNew = HOLPosition.replace( aux, p, s )
            if ( mainNew == main ) {
              EquationLeft2Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + s + ") should yield " + main + " but is " + mainNew + "." )
          } else throw new LKRuleCreationException( "Formulas " + aux + " and " + main + " don't differ in exactly one position.\n Eq: " + eqocc.formula )
        }
      case _ => throw new LKRuleCreationException( "Formula " + eq + " is not an equation." )
    }
  }

  /**
   *
   * @param s1 A sequent with term1oc in the succedent.
   * @param s2 A sequent with term2oc in the antecedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param main A formula A' such that A' is obtained by replacing one occurrence of s in A by t or vice versa.
   * @return A proof ending with either an [[EquationLeft1Rule]] or an [[EquationLeft2Rule]] according to which one leads from A to A'.
   */
  def apply( s1: OccSequent, s2: OccSequent, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, main: HOLFormula ): OccSequent = {
    val ( eqocc, auxocc ) = getTerms( s1, s2, term1oc, term2oc )
    val aux = auxocc.formula
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )

        if ( s == t && aux == main ) {
          debug( "Producing equation rule with trivial equation." )
          EquationLeft1Rule( s1, s2, term1oc, term2oc, main )
        } else if ( s == t && aux != main ) {
          throw new LKRuleCreationException( "Eq is trivial, but aux formula " + aux + " and main formula " + main + "differ." )
        } else if ( s != t && aux == main ) {
          throw new LKRuleCreationException( "Nontrivial equation, but aux and main formula are equal." )
        } else {
          val sAux = aux.find( s )
          val sMain = main.find( s )

          val tAux = aux.find( t )
          val tMain = main.find( t )

          if ( sAux.isEmpty && tAux.isEmpty )
            throw new LKRuleCreationException( "Neither " + s + " nor " + t + " found in formula " + aux + "." )

          trace( "Positions of s = " + s + " in aux = " + aux + ": " + sAux + "." )
          trace( "Positions of s = " + s + " in main = " + main + ": " + sMain + "." )

          trace( "Positions of t = " + t + " in aux = " + aux + ": " + tAux + "." )
          trace( "Positions of t = " + t + " in main = " + main + ": " + tMain + "." )

          val tToS = sMain intersect tAux
          val sToT = tMain intersect sAux
          trace( "tToS = " + tToS )
          trace( "sToT = " + sToT )

          if ( sToT.length == 1 && tToS.length == 0 ) {
            val p = sToT.head
            val mainNew = HOLPosition.replace( aux, p, t )
            if ( mainNew == main ) {
              EquationLeft1Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + t + ") should yield " + main + " but is " + mainNew + "." )
          } else if ( tToS.length == 1 && sToT.length == 0 ) {
            val p = tToS.head
            val mainNew = HOLPosition.replace( aux, p, s )
            if ( mainNew == main ) {
              EquationLeft2Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + s + ") should yield " + main + " but is " + mainNew + "." )
          } else throw new LKRuleCreationException( "Formulas " + aux + " and " + main + " don't differ in exactly one position.\n Eq: " + eqocc.formula )
        }

      case _ => throw new LKRuleCreationException( "Formula " + eq + " is not an equation." )
    }
  }

  /**
   *
   * @param s1 A sequent with term1oc in the succedent.
   * @param s2 A sequent with term2oc in the antecedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param pos A position such that A(pos) = s or A(pos) = t
   * @return A proof ending with either an [[EquationLeft1Rule]] or an [[EquationLeft2Rule]] according to which replacement is sensible.
   */
  def apply( s1: OccSequent, s2: OccSequent, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, pos: HOLPosition ): OccSequent = {
    val ( eqocc, auxocc ) = getTerms( s1, s2, term1oc, term2oc )
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )
        val aux = auxocc.formula
        val term = aux.get( pos )

        term match {
          case Some( `s` ) => EquationLeft1Rule( s1, s2, term1oc, term2oc, pos )

          case Some( `t` ) => EquationLeft2Rule( s1, s2, term1oc, term2oc, pos )

          case Some( x ) =>
            throw new LKRuleCreationException( "Wrong term " + x + " in auxiliary formula " + aux + " at position " + pos + "." )

          case None =>
            throw new LKRuleCreationException( "Position " + pos + " is not well-defined for formula " + aux + "." )
        }

      case _ =>
        throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }

  /**
   * This version of the rule operates on formulas instead of occurrences. It will attempt to find appropriate occurrences in the premises.
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the antecedent.
   * @param term1 An equation s = t.
   * @param term2 A formula A.
   * @param main A formula A' such that A' is obtained by replacing one occurrence of s in A by t or vice versa.
   * @return A proof ending with either an [[EquationLeft1Rule]] or an [[EquationLeft2Rule]] according to which one leads from A to A'.
   */
  def apply( s1: LKProof, s2: LKProof, term1: HOLFormula, term2: HOLFormula, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
    ( s1.root.succedent.filter( x => x.formula == term1 ).toList, s2.root.antecedent.filter( x => x.formula == term2 ).toList ) match {
      case ( ( x :: _ ), ( y :: _ ) ) => apply( s1, s2, x, y, main )
      case _                          => throw new LKRuleCreationException( "Not matching formula occurrences found for application of the rule with the given formula" )
    }
  }

  /**
   * This version creates an axiom for the equation.
   *
   */
  def apply( s1: LKProof, term1oc: FormulaOccurrence, eq: HOLFormula, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
    val leftSubproof = Axiom( eq )
    apply( leftSubproof, s1, leftSubproof.root.succedent( 0 ), term1oc, main )
  }

  private def getTerms( s1: OccSequent, s2: OccSequent, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence ) = {
    val term1op = s1.succedent.find( _ == term1oc )
    val term2op = s2.antecedent.find( _ == term2oc )
    if ( term1op == None || term2op == None ) throw new LKRuleCreationException( "Auxiliary formulas are not contained in the right part of the sequent" )
    else {
      val eqocc = term1op.get
      val auxocc = term2op.get
      ( eqocc, auxocc )
    }
  }
}

/**
 * This macro rule unifies [[EquationRight1Rule]] and [[EquationRight2Rule]] by automatically choosing the appropriate rule.
 *
 */
object EquationRightRule extends EquationRuleLogger {

  /**
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the succedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param pos A position such that A(pos) = s or A(pos) = t
   * @return A proof ending with either an [[EquationRight1Rule]] or an [[EquationRight2Rule]] according to which replacement is sensible.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, pos: HOLPosition ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas with TermPositions = {
    val ( eqocc, auxocc ) = getTerms( s1.root, s2.root, term1oc, term2oc )
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )
        val aux = auxocc.formula
        val term = aux.get( pos )

        term match {
          case Some( `s` ) => EquationRight1Rule( s1, s2, term1oc, term2oc, pos )

          case Some( `t` ) => EquationRight2Rule( s1, s2, term1oc, term2oc, pos )

          case Some( x ) =>
            throw new LKRuleCreationException( "Wrong term " + x + " in auxiliary formula " + aux + " at position " + pos + "." )

          case None =>
            throw new LKRuleCreationException( "Position " + pos + " is not well-defined for formula " + aux + "." )
        }

      case _ =>
        throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }

  /**
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the succedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param main A formula A' such that A' is obtained by replacing one occurrence of s in A by t or vice versa.
   * @return A proof ending with either an [[EquationRight1Rule]] or an [[EquationRight2Rule]] according to which one leads from A to A'.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas with TermPositions = {
    val ( eqocc, auxocc ) = getTerms( s1.root, s2.root, term1oc, term2oc )
    val aux = auxocc.formula
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )

        if ( s == t && aux == main ) {
          debug( "Producing equation rule with trivial equation." )
          EquationRight1Rule( s1, s2, term1oc, term2oc, main )
        } else if ( s == t && aux != main ) {
          throw new LKRuleCreationException( "Eq is trivial, but aux formula " + aux + " and main formula " + main + "differ." )
        } else if ( s != t && aux == main ) {
          throw new LKRuleCreationException( "Nontrivial equation, but aux and main formula are equal." )
        } else {
          val sAux = aux.find( s )
          val sMain = main.find( s )

          val tAux = aux.find( t )
          val tMain = main.find( t )

          if ( sAux.isEmpty && tAux.isEmpty )
            throw new LKRuleCreationException( "Neither " + s + " nor " + t + " found in formula " + aux + "." )

          trace( "Positions of s = " + s + " in aux = " + aux + ": " + sAux + "." )
          trace( "Positions of s = " + s + " in main = " + main + ": " + sMain + "." )

          trace( "Positions of t = " + t + " in aux = " + aux + ": " + tAux + "." )
          trace( "Positions of t = " + t + " in main = " + main + ": " + tMain + "." )

          val tToS = sMain intersect tAux
          val sToT = tMain intersect sAux
          trace( "tToS = " + tToS )
          trace( "sToT = " + sToT )

          if ( sToT.length == 1 && tToS.length == 0 ) {
            val p = sToT.head
            val mainNew = HOLPosition.replace( aux, p, t )
            if ( mainNew == main ) {
              EquationRight1Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + t + ") should yield " + main + " but is " + mainNew + "." )
          } else if ( tToS.length == 1 && sToT.length == 0 ) {
            val p = tToS.head
            val mainNew = HOLPosition.replace( aux, p, s )
            if ( mainNew == main ) {
              EquationRight2Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + s + ") should yield " + main + " but is " + mainNew + "." )
          } else throw new LKRuleCreationException( "Formulas " + aux + " and " + main + " don't differ in exactly one position.\n Eq: " + eqocc.formula )
        }
      case _ => throw new LKRuleCreationException( "Formula " + eq + " is not an equation." )
    }
  }

  /**
   *
   * @param s1 A sequent with term1oc in the succedent.
   * @param s2 A sequent with term2oc in the succedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param main A formula A' such that A' is obtained by replacing one occurrence of s in A by t or vice versa.
   * @return A proof ending with either an [[EquationRight1Rule]] or an [[EquationRight2Rule]] according to which one leads from A to A'.
   */
  def apply( s1: OccSequent, s2: OccSequent, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, main: HOLFormula ): OccSequent = {
    val ( eqocc, auxocc ) = getTerms( s1, s2, term1oc, term2oc )
    val aux = auxocc.formula
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )

        if ( s == t && aux == main ) {
          debug( "Producing equation rule with trivial equation." )
          EquationRight1Rule( s1, s2, term1oc, term2oc, main )
        } else if ( s == t && aux != main ) {
          throw new LKRuleCreationException( "Eq is trivial, but aux formula " + aux + " and main formula " + main + "differ." )
        } else if ( s != t && aux == main ) {
          throw new LKRuleCreationException( "Nontrivial equation, but aux and main formula are equal." )
        } else {
          val sAux = aux.find( s )
          val sMain = main.find( s )

          val tAux = aux.find( t )
          val tMain = main.find( t )

          if ( sAux.isEmpty && tAux.isEmpty )
            throw new LKRuleCreationException( "Neither " + s + " nor " + t + " found in formula " + aux + "." )

          trace( "Positions of s = " + s + " in aux = " + aux + ": " + sAux + "." )
          trace( "Positions of s = " + s + " in main = " + main + ": " + sMain + "." )

          trace( "Positions of t = " + t + " in aux = " + aux + ": " + tAux + "." )
          trace( "Positions of t = " + t + " in main = " + main + ": " + tMain + "." )

          val tToS = sMain intersect tAux
          val sToT = tMain intersect sAux
          trace( "tToS = " + tToS )
          trace( "sToT = " + sToT )

          if ( sToT.length == 1 && tToS.length == 0 ) {
            val p = sToT.head
            val mainNew = HOLPosition.replace( aux, p, t )
            if ( mainNew == main ) {
              EquationRight1Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + t + ") should yield " + main + " but is " + mainNew + "." )
          } else if ( tToS.length == 1 && sToT.length == 0 ) {
            val p = tToS.head
            val mainNew = HOLPosition.replace( aux, p, s )
            if ( mainNew == main ) {
              EquationRight2Rule( s1, s2, term1oc, term2oc, p )
            } else throw new LKRuleCreationException( "Replacement (" + aux + ", " + p + ", " + s + ") should yield " + main + " but is " + mainNew + "." )
          } else throw new LKRuleCreationException( "Formulas " + aux + " and " + main + " don't differ in exactly one position.\n Eq: " + eqocc.formula )
        }
      case _ => throw new LKRuleCreationException( "Formula " + eq + " is not an equation." )
    }
  }

  /**
   *
   * @param s1 A sequent with term1oc in the succedent.
   * @param s2 A sequent with term2oc in the succedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param pos A position such that A(pos) = s or A(pos) = t
   * @return A proof ending with either an [[EquationRight1Rule]] or an [[EquationRight2Rule]] according to which replacement is sensible.
   */
  def apply( s1: OccSequent, s2: OccSequent, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, pos: HOLPosition ): OccSequent = {
    val ( eqocc, auxocc ) = getTerms( s1, s2, term1oc, term2oc )
    val eq = eqocc.formula

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )
        val aux = auxocc.formula
        val term = aux.get( pos )

        term match {
          case Some( `s` ) => EquationRight1Rule( s1, s2, term1oc, term2oc, pos )

          case Some( `t` ) => EquationRight2Rule( s1, s2, term1oc, term2oc, pos )

          case Some( x ) =>
            throw new LKRuleCreationException( "Wrong term " + x + " in auxiliary formula " + aux + " at position " + pos + "." )

          case None =>
            throw new LKRuleCreationException( "Position " + pos + " is not well-defined for formula " + aux + "." )
        }

      case _ =>
        throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }

  /**
   * This version of the rule operates on formulas instead of occurrences. It will attempt to find appropriate occurrences in the premises.
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the succedent.
   * @param term1 An equation s = t.
   * @param term2 A formula A.
   * @param main A formula A' such that A' is obtained by replacing one occurrence of s in A by t or vice versa.
   * @return A proof ending with either an [[EquationRight1Rule]] or an [[EquationRight2Rule]] according to which one leads from A to A'.
   */
  def apply( s1: LKProof, s2: LKProof, term1: HOLFormula, term2: HOLFormula, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas =
    ( s1.root.succedent.filter( x => x.formula == term1 ).toList, s2.root.succedent.filter( x => x.formula == term2 ).toList ) match {
      case ( ( x :: _ ), ( y :: _ ) ) => apply( s1, s2, x, y, main )
      case _                          => throw new LKRuleCreationException( "Not matching formula occurrences found for application of the rule with the given formula" )
    }

  /**
   * This version creates an axiom for the equation.
   *
   */
  def apply( s1: LKProof, term1oc: FormulaOccurrence, eq: HOLFormula, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
    val leftSubproof = Axiom( eq )
    apply( leftSubproof, s1, leftSubproof.root.succedent( 0 ), term1oc, main )
  }

  private def getTerms( s1: OccSequent, s2: OccSequent, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence ) = {
    val term1op = s1.succedent.find( _ == term1oc )
    val term2op = s2.succedent.find( _ == term2oc )
    if ( term1op == None || term2op == None ) throw new LKRuleCreationException( "Auxiliary formulas are not contained in the right part of the sequent" )
    else {
      val eqocc = term1op.get
      val auxocc = term2op.get
      ( eqocc, auxocc )
    }
  }
}

/**
 * Macro rule that simulates several term replacements at once.
 *
 */
object EquationLeftMacroRule extends EquationRuleLogger {

  /**
   * Allows replacements at several positions in the auxiliary formula.
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the antecedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param sPos List of positions of terms that should be replaced by s.
   * @param tPos List of positions of terms that should be replaced by t.
   * @return A new proof whose main formula is A with every p in sPos replaced by s and every p in tPos replaced by t.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, sPos: Seq[HOLPosition], tPos: Seq[HOLPosition] ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas = {
    val ( eqocc, auxocc ) = ( s1.root.succedent.find( _ == term1oc ), s2.root.antecedent.find( _ == term2oc ) ) match {
      case ( Some( e ), Some( a ) ) => ( e, a )
      case _                        => throw new LKRuleCreationException( "Auxiliary formulas not found." )
    }
    val ( eq, aux ) = ( eqocc.formula, auxocc.formula )

    trace( "EquationLeftMacroRule called with equation " + term1oc + ", aux formula " + term2oc + ", s positions " + sPos + " and t positions " + tPos )

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )

        // Filter out those positions where no terms need to be replaced.
        val ( sPosActive, tPosActive ) = ( sPos filter { aux.get( _ ) match { case Some( `t` ) => true; case _ => false } },
          tPos filter { aux.get( _ ) match { case Some( `s` ) => true; case _ => false } } )
        val n = sPosActive.length + tPosActive.length

        trace( "" + n + " replacements to make." )

        n match {
          case 0 => throw new Exception( "This should never happen." )
          case 1 =>
            EquationLeftRule( s1, s2, term1oc, term2oc, ( sPosActive ++ tPosActive ).head )
          case _ =>

            // Initialize the proof currently being worked on and its auxiliary formula.
            var currentProofR = s2
            var currentAux = term2oc

            // Save newly created equations in a list so we can later contract them.
            val equations = new ListBuffer[FormulaOccurrence]

            // Iterate over the s-positions
            for ( p <- sPosActive ) aux.get( p ) match {
              case Some( `s` ) => trace( "s found at s-position " + p + ", nothing to do." )
              case Some( `t` ) =>

                // Generate a new instance of s = t :- s = t and save the formula in the antecedent in the equations list.
                val currentProofL = Axiom( List( eq ), List( eq ) )
                equations += currentProofL.root.antecedent.head
                val currentEq = currentProofL.root.succedent.head

                // Create a subproof that replaces the term at p.
                currentProofR = EquationLeftRule( currentProofL, currentProofR, currentEq, currentAux, p )

                // The new auxiliary formula is the principal formula of the previous step.
                currentAux = currentProofR.asInstanceOf[PrincipalFormulas].prin( 0 )

              case _ => throw new LKRuleCreationException( "Position " + p + " in formula " + aux + " does not contain term " + s + " or " + t + "." )
            }

            // Iterate over the t-positions. For comments see the previous loop.
            for ( p <- tPosActive ) aux.get( p ) match {
              case Some( `s` ) =>

                val currentProofL = Axiom( List( eq ), List( eq ) )
                equations += currentProofL.root.antecedent.head
                val currentEq = currentProofL.root.succedent.head

                currentProofR = EquationLeftRule( currentProofL, currentProofR, currentEq, currentAux, p )

                currentAux = currentProofR.asInstanceOf[PrincipalFormulas].prin( 0 )

              case Some( `t` ) => trace( "t found at t-position " + p + ", nothing to do." )
              case _           => throw new LKRuleCreationException( "Position " + p + " in formula " + aux + " does not contain term " + s + " or " + t + "." )
            }

            trace( "" + n + " replacements made." )

            // Find the descendants of the saved equations in the current end sequent.
            val equationDescendants = equations.toList map { currentProofR.getDescendantInLowerSequent } map { _.get }

            // Contract the equations.
            currentProofR = ContractionLeftMacroRule( currentProofR, equationDescendants )

            // Finally, remove the remaining occurrence of s = t with a cut.
            CutRule( s1, currentProofR, eqocc, currentProofR.asInstanceOf[PrincipalFormulas].prin( 0 ) )
        }
      case _ => throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }

  /**
   * Allows replacements at several positions in the auxiliary formula.
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the antecedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param main The proposed main formula.
   * @return A new proof with principal formula main. Eq rules will be used according to the replacements that need to be made.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas = {
    val ( eqocc, auxocc ) = ( s1.root.succedent.find( _ == term1oc ), s2.root.antecedent.find( _ == term2oc ) ) match {
      case ( Some( e ), Some( a ) ) => ( e, a )
      case _                        => throw new LKRuleCreationException( "Auxiliary formulas not found." )
    }
    val ( eq, aux ) = ( eqocc.formula, auxocc.formula )

    trace( "EquationLeftMacroRule called with equation " + term1oc + ", aux formula " + term2oc + " and main formula " + main )

    eq match {
      case Eq( s, t ) =>

        trace( "Eq: " + s + " = " + t + "." )

        if ( s == t && aux == main ) {
          debug( "Producing equation rule with trivial equation." )
          EquationLeft1Rule( s1, s2, term1oc, term2oc, main )
        } else if ( s == t && aux != main ) {
          throw new LKRuleCreationException( "Eq is trivial, but aux formula " + aux + " and main formula " + main + "differ." )
        } else if ( s != t && aux == main ) {
          throw new LKRuleCreationException( "Nontrivial equation, but aux and main formula are equal." )
        } else {
          // Find all positions of s and t in aux.
          val ( auxS, auxT ) = ( aux.find( s ), aux.find( t ) )

          // Find all positions of s and t in main.
          val ( mainS, mainT ) = ( main.find( s ), main.find( t ) )

          // Find the positions where actual replacements will happen.
          val ( tToS, sToT ) = ( mainS intersect auxT, mainT intersect auxS )

          // Call the previous apply method.
          apply( s1, s2, term1oc, term2oc, tToS, sToT )
        }

      case _ => throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }
}

/**
 * Macro rule that simulates several term replacements at once.
 *
 */
object EquationRightMacroRule extends EquationRuleLogger {

  /**
   * Allows replacements at several positions in the auxiliary formula.
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the succedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param sPos List of positions of terms that should be replaced by s.
   * @param tPos List of positions of terms that should be replaced by t.
   * @return A new proof whose main formula is A with every p in sPos replaced by s and every p in tPos replaced by t.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, sPos: Seq[HOLPosition], tPos: Seq[HOLPosition] ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas = {
    // Detailed comments can be found in the corresponding apply method for EquationLeftBulkRule!
    val ( eqocc, auxocc ) = ( s1.root.succedent.find( _ == term1oc ), s2.root.succedent.find( _ == term2oc ) ) match {
      case ( Some( e ), Some( a ) ) => ( e, a )
      case _                        => throw new LKRuleCreationException( "Auxiliary formulas not found." )
    }
    val ( eq, aux ) = ( eqocc.formula, auxocc.formula )

    trace( "EquationRightMacroRule called with equation " + term1oc + ", aux formula " + term2oc + ", s positions " + sPos + " and t positions " + tPos )

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )
        val ( sPosActive, tPosActive ) = ( sPos filter { aux.get( _ ) match { case Some( `t` ) => true; case _ => false } },
          tPos filter { aux.get( _ ) match { case Some( `s` ) => true; case _ => false } } )
        val n = sPosActive.length + tPosActive.length
        trace( "" + n + " replacements to make." )

        n match {
          case 0 => throw new Exception( "This should never happen." )
          case 1 =>
            EquationRightRule( s1, s2, term1oc, term2oc, ( sPosActive ++ tPosActive ).head )
          case _ =>

            var currentProofR = s2
            var currentAux = term2oc

            val equations = new ListBuffer[FormulaOccurrence]

            for ( p <- sPosActive ) aux.get( p ) match {
              case Some( `s` ) => trace( "s found at s-position " + p + ", nothing to do." )
              case Some( `t` ) =>
                val currentProofL = Axiom( List( eq ), List( eq ) )
                equations += currentProofL.root.antecedent.head
                val currentEq = currentProofL.root.succedent.head

                currentProofR = EquationRightRule( currentProofL, currentProofR, currentEq, currentAux, p )
                currentAux = currentProofR.asInstanceOf[PrincipalFormulas].prin( 0 )

              case _ => throw new LKRuleCreationException( "Position " + p + " in formula " + aux + " does not contain term " + s + " or " + t + "." )
            }

            for ( p <- tPosActive ) aux.get( p ) match {
              case Some( `s` ) =>
                val currentProofL = Axiom( List( eq ), List( eq ) )
                equations += currentProofL.root.antecedent.head
                val currentEq = currentProofL.root.succedent.head

                currentProofR = EquationRightRule( currentProofL, currentProofR, currentEq, currentAux, p )
                currentAux = currentProofR.asInstanceOf[PrincipalFormulas].prin( 0 )

              case Some( `t` ) => trace( "t found at t-position " + p + ", nothing to do." )
              case _           => throw new LKRuleCreationException( "Position " + p + " in formula " + aux + " does not contain term " + s + " or " + t + "." )
            }

            trace( "" + n + " replacements made." )

            val equationDescendants = equations.toList map { currentProofR.getDescendantInLowerSequent } map { _.get }
            currentProofR = ContractionLeftMacroRule( currentProofR, equationDescendants )

            CutRule( s1, currentProofR, eqocc, currentProofR.asInstanceOf[PrincipalFormulas].prin( 0 ) )
        }
      case _ => throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }

  /**
   * Allows replacements at several positions in the auxiliary formula.
   *
   * @param s1 A proof ending with term1oc in the succedent.
   * @param s2 A proof ending with term2oc in the succedent.
   * @param term1oc An equation s = t.
   * @param term2oc A formula A.
   * @param main The proposed main formula.
   * @return A new proof with principal formula main. Eq rules will be used according to the replacements that need to be made.
   */
  def apply( s1: LKProof, s2: LKProof, term1oc: FormulaOccurrence, term2oc: FormulaOccurrence, main: HOLFormula ): BinaryTree[OccSequent] with BinaryLKProof with AuxiliaryFormulas = {
    val ( eqocc, auxocc ) = ( s1.root.succedent.find( _ == term1oc ), s2.root.succedent.find( _ == term2oc ) ) match {
      case ( Some( e ), Some( a ) ) => ( e, a )
      case _                        => throw new LKRuleCreationException( "Auxiliary formulas not found." )
    }
    val ( eq, aux ) = ( eqocc.formula, auxocc.formula )

    trace( "EquationRightMacroRule called with equation " + term1oc + ", aux formula " + term2oc + " and main formula " + main )

    eq match {
      case Eq( s, t ) =>
        trace( "Eq: " + s + " = " + t + "." )

        if ( s == t && aux == main ) {
          debug( "Producing equation rule with trivial equation." )
          EquationRight1Rule( s1, s2, term1oc, term2oc, main )
        } else if ( s == t && aux != main ) {
          throw new LKRuleCreationException( "Eq is trivial, but aux formula " + aux + " and main formula " + main + "differ." )
        } else if ( s != t && aux == main ) {
          throw new LKRuleCreationException( "Nontrivial equation, but aux and main formula are equal." )
        } else {

          // Find all positions of s and t in aux.
          val ( auxS, auxT ) = ( aux.find( s ), aux.find( t ) )

          // Find all positions of s and t in main.
          val ( mainS, mainT ) = ( main.find( s ), main.find( t ) )

          // Find the positions where actual replacements will happen.
          val ( tToS, sToT ) = ( mainS intersect auxT, mainT intersect auxS )

          // Call the previous apply method.
          apply( s1, s2, term1oc, term2oc, tToS, sToT )

        }
      case _ => throw new LKRuleCreationException( "Formula occurrence " + eqocc + " is not an equation." )
    }
  }
}

/**
 * This macro rule simulates a series of contractions in the antecedent.
 *
 */
object ContractionLeftMacroRule extends MacroRuleLogger {

  /**
   *
   * @param s1 A proof.
   * @param occs A list of occurrences of a Formula in the antecedent of s1.
   * @return A proof ending with as many contraction rules as necessary to contract occs into a single occurrence.
   */
  def apply( s1: LKProof, occs: Seq[FormulaOccurrence] ): Tree[OccSequent] with LKProof = occs match {
    case Nil | _ :: Nil => s1
    case occ1 :: occ2 :: rest =>
      rest match {
        case Nil => ContractionLeftRule( s1, occ1, occ2 )

        case _ =>
          val subProof = ContractionLeftRule( s1, occ1, occ2 )
          val occ = subProof.prin( 0 )
          val restNew = rest map { subProof.getDescendantInLowerSequent }
          if ( restNew.forall( _.isDefined ) )
            ContractionLeftMacroRule( subProof, occ :: restNew.map( _.get ) )
          else
            throw new LKRuleCreationException( "Formula not found in sequent " + s1.root )
      }
  }

  /**
   * Contracts one formula in the antecedent down to n occurrences. Use with care!
   *
   * @param s1 A proof.
   * @param form A formula.
   * @param n Maximum number of occurrences of form in the antecedent of the end sequent. Defaults to 1, i.e. all occurrences are contracted.
   * @return
   */
  def apply( s1: LKProof, form: HOLFormula, n: Int = 1 ): Tree[OccSequent] with LKProof = {
    if ( n < 1 ) throw new IllegalArgumentException( "n must be >= 1." )
    val list = s1.root.antecedent.filter( _.formula == form ).drop( n - 1 )

    apply( s1, list )
  }
}

/**
 * This macro rule simulates a series of contractions in the succedent.
 *
 */
object ContractionRightMacroRule extends MacroRuleLogger {

  /**
   *
   * @param s1 A proof.
   * @param occs A list of occurrences of a Formula in the succedent of s1.
   * @return A proof ending with as many contraction rules as necessary to contract occs into a single occurrence.
   */
  def apply( s1: LKProof, occs: Seq[FormulaOccurrence] ): Tree[OccSequent] with LKProof = occs match {
    case Nil | _ :: Nil => s1
    case occ1 :: occ2 :: rest =>
      rest match {
        case Nil => ContractionRightRule( s1, occ1, occ2 )

        case _ =>
          val subProof = ContractionRightRule( s1, occ1, occ2 )
          val occ = subProof.prin( 0 )
          val restNew = rest map { o => subProof.getDescendantInLowerSequent( o ) }
          if ( restNew.forall( o => o.isDefined ) )
            ContractionRightMacroRule( subProof, occ :: restNew.map( _.get ) )
          else
            throw new LKRuleCreationException( "Formula not found in sequent " + s1.root )
      }
  }

  /**
   * Contracts one formula in the succedent down to n occurrences. Use with care!
   *
   * @param s1 A proof.
   * @param form A formula.
   * @param n Maximum number of occurrences of form in the succedent of the end sequent. Defaults to 1, i.e. all occurrences are contracted.
   * @return
   */
  def apply( s1: LKProof, form: HOLFormula, n: Int = 1 ): Tree[OccSequent] with LKProof = {
    if ( n < 1 ) throw new IllegalArgumentException( "n must be >= 1." )
    val list = s1.root.succedent.filter( _.formula == form ).drop( n - 1 )

    apply( s1, list )
  }
}

/**
 * This macro rule simulates a series of contractions in both cedents.
 *
 */
object ContractionMacroRule extends MacroRuleLogger {

  /**
   * Contracts the current proof down to a given FSequent.
   *
   * @param s1 An LKProof.
   * @param targetSequent The target sequent.
   * @param strict If true, the root of s1 must 1.) contain every formula at least as often as targetSequent
   *               and 2.) contain no formula that isn't contained at least once in targetSequent.
   * @return s1 with its end sequent contracted down to targetSequent.
   */
  def apply( s1: LKProof, targetSequent: HOLSequent, strict: Boolean = true ): Tree[OccSequent] with LKProof = {
    trace( "ContractionMacroRule called with subproof " + s1 + ", target sequent " + targetSequent + ", strict = " + strict )
    val currentSequent = s1.root.toHOLSequent
    val targetAnt = targetSequent.antecedent
    val targetSuc = targetSequent.succedent

    val assertion = ( ( targetSequent isSubMultisetOf currentSequent )
      && ( currentSequent isSubsetOf targetSequent ) )

    trace( "targetSequent diff currentSequent: " + targetSequent.diff( currentSequent ) )
    trace( "currentSequent.distinct diff targetSequent.distinct: " + currentSequent.distinct.diff( targetSequent.distinct ) )
    trace( "If called with strict this would " + { if ( assertion ) "succeed." else "fail." } )

    if ( strict & !assertion ) {
      throw new LKRuleCreationException( "Sequent " + targetSequent + " cannot be reached from " + currentSequent + " by contractions." )
    }

    val subProof = targetAnt.distinct.foldLeft( s1 )( ( acc, x ) => { trace( "Contracting formula " + x + " in antecedent." ); ContractionLeftMacroRule( acc, x, targetAnt.count( _ == x ) ) } )
    targetSuc.distinct.foldLeft( subProof )( ( acc, x ) => { trace( "Contracting formula " + x + " in succedent." ); ContractionRightMacroRule( acc, x, targetSuc.count( _ == x ) ) } )
  }

  /**
   * Performs all possible contractions. Use with care!
   *
   * @param s1 A proof.
   * @return A proof with all duplicate formulas in the end sequent contracted.
   */
  def apply( s1: LKProof ): Tree[OccSequent] with LKProof = {
    val targetSequent = s1.root.toHOLSequent.distinct
    apply( s1, targetSequent )
  }

}

/**
 * This macro rule simulates a series of weakenings in the antecedent.
 *
 */
object WeakeningLeftMacroRule extends MacroRuleLogger {

  /**
   *
   * @param s1 A Proof.
   * @param list A list of Formulas.
   * @return A new proof whose antecedent contains new occurrences of the formulas in list.
   */
  def apply( s1: LKProof, list: Seq[HOLFormula] ): Tree[OccSequent] with LKProof =
    list.foldLeft( s1 ) { ( acc, x ) => WeakeningLeftRule( acc, x ) }

  /**
   *
   * @param s1 An LKProof.
   * @param form A Formula.
   * @param n A natural number.
   * @return s1 extended with weakenings such that form occurs at least n times in the antecedent of the end sequent.
   */
  def apply( s1: LKProof, form: HOLFormula, n: Int ): Tree[OccSequent] with LKProof = {
    val nCurrent = s1.root.antecedent.count( _.formula == form )

    WeakeningLeftMacroRule( s1, Seq.fill( n - nCurrent )( form ) )
  }
}

/**
 * This macro rule simulates a series of weakenings in the succedent.
 *
 */
object WeakeningRightMacroRule extends MacroRuleLogger {

  /**
   *
   * @param s1 A Proof.
   * @param list A list of Formulas.
   * @return A new proof whose succedent contains new occurrences of the formulas in list.
   */
  def apply( s1: LKProof, list: Seq[HOLFormula] ): Tree[OccSequent] with LKProof =
    list.foldLeft( s1 ) { ( acc, x ) => WeakeningRightRule( acc, x ) }

  /**
   *
   * @param s1 An LKProof.
   * @param form A Formula.
   * @param n A natural number.
   * @return s1 extended with weakenings such that form occurs at least n times in the succedent of the end sequent.
   */
  def apply( s1: LKProof, form: HOLFormula, n: Int ): Tree[OccSequent] with LKProof = {
    val nCurrent = s1.root.succedent.count( _.formula == form )

    WeakeningRightMacroRule( s1, Seq.fill( n - nCurrent )( form ) )
  }
}

/**
 * This macro rule simulates a series of weakenings in both cedents.
 *
 */
object WeakeningMacroRule extends MacroRuleLogger {

  /**
   *
   * @param s1 A proof.
   * @param antList A list of formulas.
   * @param sucList A list of formulas.
   * @return A new proof whose antecedent and succedent contain new occurrences of the formulas in antList and sucList, respectively.
   */
  def apply( s1: LKProof, antList: Seq[HOLFormula], sucList: Seq[HOLFormula] ): Tree[OccSequent] with LKProof =
    WeakeningRightMacroRule( WeakeningLeftMacroRule( s1, antList ), sucList )

  /**
   *
   * @param s1 A proof.
   * @param targetSequent A sequent of formulas.
   * @param strict If true, will require that targetSequent contains the root of s1.
   * @return A proof whose end sequent is targetSequent.
   */
  def apply( s1: LKProof, targetSequent: HOLSequent, strict: Boolean = true ): Tree[OccSequent] with LKProof = {
    val currentSequent = s1.root.toHOLSequent

    if ( strict & !( currentSequent isSubMultisetOf targetSequent ) )
      throw new LKRuleCreationException( "Sequent " + targetSequent + " cannot be reached from " + currentSequent + " by weakenings." )

    val ( antDiff, sucDiff ) = ( targetSequent diff currentSequent ).toTuple

    WeakeningMacroRule( s1, antDiff, sucDiff )
  }
}

/**
 * This macro rule simulates multiple weakenings and contractions in both cedents.
 *
 */
object WeakeningContractionMacroRule extends MacroRuleLogger {

  /**
   *
   * @param s1 An LKProof.
   * @param antList List of pairs (f,n) of type (Formula, Int) that express “f should occur n times in the antecedent”.
   * @param sucList List of pairs (f,n) of type (Formula, Int) that express “f should occur n times in the succedent”.
   * @param strict If true: requires that for (f,n) in antList or sucList, if f occurs in the root of s1, then n > 0.
   * @return
   */
  def apply( s1: LKProof, antList: Seq[( HOLFormula, Int )], sucList: Seq[( HOLFormula, Int )], strict: Boolean ): Tree[OccSequent] with LKProof = {
    val currentAnt = s1.root.antecedent map { _.formula }
    val currentSuc = s1.root.succedent map { _.formula }

    val subProof = antList.foldLeft( s1 )( ( acc, p ) => {
      val ( f, n ) = p
      val nCurrent = currentAnt.count( _ == f )
      if ( n == 0 && nCurrent != 0 && strict )
        throw new LKRuleCreationException( "Cannot erase formula occurrences." )

      if ( n > nCurrent )
        WeakeningLeftMacroRule( acc, f, n - nCurrent )
      else if ( n == nCurrent )
        acc
      else // n < nCurrent
        ContractionLeftMacroRule( acc, f, n )
    } )

    sucList.foldLeft( subProof )( ( acc, p ) => {
      val ( f, n ) = p
      val nCurrent = currentSuc.count( _ == f )
      if ( n == 0 && nCurrent != 0 && strict )
        throw new LKRuleCreationException( "Cannot erase formula occurrences." )

      if ( n > nCurrent )
        WeakeningRightMacroRule( acc, f, n - nCurrent )
      else if ( n == nCurrent )
        acc
      else // n < nCurrent
        ContractionRightMacroRule( acc, f, n )
    } )
  }

  /**
   *
   * @param s1 An LKProof.
   * @param targetSequent The proposed end sequent.
   * @param strict If true, will require that the root of s1 contains no formula that doesn't appear at least once in targetSequent.
   * @return s1 with its end sequent modified to targetSequent by means of weakening and contraction.
   */
  def apply( s1: LKProof, targetSequent: HOLSequent, strict: Boolean = true ): Tree[OccSequent] with LKProof = {
    val currentSequent = s1.root.toHOLSequent
    val targetAnt = targetSequent.antecedent
    val targetSuc = targetSequent.succedent

    if ( strict && !( currentSequent isSubsetOf targetSequent ) )
      throw new LKRuleCreationException( "Sequent " + targetSequent + " cannot be reached from " + currentSequent + " by weakenings and contractions." )

    val antList = targetAnt.distinct map ( f => ( f, targetAnt.count( _ == f ) ) )
    val sucList = targetSuc.distinct map ( f => ( f, targetSuc.count( _ == f ) ) )

    apply( s1, antList, sucList, strict )
  }
}

/**
 * Computes a proof of F from a proof of some instances of F
 *
 */
object proofFromInstances {
  /**
   *
   * @param s1 An LKProof containing the instances in es in its end sequent.
   * @param es An ExpansionSequent in which all shallow formulas are prenex and which contains no strong or Skolem quantifiers.
   * @return A proof starting with s1 and ending with the deep sequent of es.
   */
  def apply( s1: LKProof, es: ExpansionSequent ): LKProof =
    ( es.antecedent ++ es.succedent ).foldLeft( s1 )( apply )

  /**
   *
   * @param s1 An LKProof containing the instances in et in its end sequent
   * @param et An ExpansionTree whose shallow formula is prenex and which contains no strong or Skolem quantifiers.
   * @return A proof starting with s1 and ending with the deep formula of et.
   */
  def apply( s1: LKProof, et: ExpansionTree ): LKProof = apply( s1, compressQuantifiers( et ) )

  /**
   *
   * @param s1 An LKProof containing the instances in mes in its end sequent.
   * @param mes A MultiExpansionSequent in which all shallow formulas are prenex and which contains no strong or Skolem quantifiers.
   * @return A proof starting with s1 and ending with the deep sequent of mes.
   */
  def apply( s1: LKProof, mes: MultiExpansionSequent )( implicit dummyImplicit: DummyImplicit ): LKProof = ( mes.antecedent ++ mes.succedent ).foldLeft( s1 )( apply )

  /**
   *
   * @param s1 An LKProof containing the instances in et in its end sequent
   * @param met A MultiExpansionTree whose shallow formula is prenex and which contains no strong or Skolem quantifiers.
   * @return A proof starting with s1 and ending with the deep formula of met.
   */
  def apply( s1: LKProof, met: MultiExpansionTree ): LKProof = {
    require( isPrenex( met.toShallow ), "Shallow formula of " + met + " is not prenex" )

    met match {
      case METWeakQuantifier( f @ All( _, _ ), instances ) =>
        val tmp = instances.foldLeft( s1 ) {
          ( acc, i ) => ForallLeftBlock( acc, f, i._2 )
        }

        ContractionLeftMacroRule( tmp, f )

      case METWeakQuantifier( f @ Ex( _, _ ), instances ) =>
        val tmp = instances.foldLeft( s1 ) {
          ( acc, i ) => ExistsRightBlock( acc, f, i._2 )
        }

        ContractionRightMacroRule( tmp, f )

      case METSkolemQuantifier( _, _, _ ) | METStrongQuantifier( _, _, _ ) =>
        throw new UnsupportedOperationException( "This case is not handled at this time." )
      case _ => s1
    }
  }
}

/**
 * Maybe there is a better place for this?
 *
 */
object applyRecursive {

  /**
   * Recursively applies a function f to a proof.
   *
   * In the case of an axiom p, the result is just f(p).
   *
   * In the case of a unary proof p with subproof u, this means that it recursively applies f to u, giving u', and then computes f(p(u')).
   * Binary proofs work analogously.
   *
   * Caveat: It might mess up the ancestor relation on formula occurrences, so be careful.
   *
   * @param f A function of type LKProof => LKProof
   * @param proof An LKProof
   * @return
   */
  def apply( f: LKProof => LKProof )( proof: LKProof ): LKProof = proof match {

    case Axiom( _ ) => f( proof )

    // Unary rules
    case WeakeningLeftRule( up, _, p1 ) =>
      f( WeakeningLeftRule( applyRecursive( f )( up ), p1.formula ) )

    case WeakeningRightRule( up, r, p1 ) =>
      f( WeakeningRightRule( applyRecursive( f )( up ), p1.formula ) )

    case ContractionLeftRule( up, r, a1, _, _ ) =>
      val subProof = applyRecursive( f )( up )
      f( ContractionLeftRule( subProof, a1.formula ) )

    case ContractionRightRule( up, r, a1, _, _ ) =>
      val subProof = applyRecursive( f )( up )
      f( ContractionRightRule( subProof, a1.formula ) )

    case AndLeft1Rule( up, _, a, p ) =>
      val subProof = applyRecursive( f )( up )
      f( AndLeft1Rule( subProof, a.formula, p.formula ) )

    case AndLeft2Rule( up, _, a, p ) =>
      val subProof = applyRecursive( f )( up )
      f( AndLeft2Rule( subProof, p.formula, a.formula ) )

    case OrRight1Rule( up, r, a, p ) =>
      val subProof = applyRecursive( f )( up )
      f( OrRight1Rule( subProof, a.formula, p.formula ) )

    case OrRight2Rule( up, r, a, p ) =>
      val subProof = applyRecursive( f )( up )
      f( OrRight2Rule( subProof, p.formula, a.formula ) )

    case ImpRightRule( up, _, a1, a2, _ ) =>
      val subProof = applyRecursive( f )( up )
      f( ImpRightRule( subProof, a1.formula, a2.formula ) )

    case NegLeftRule( up, _, a, _ ) =>
      val subProof = applyRecursive( f )( up )
      f( NegLeftRule( subProof, a.formula ) )

    case NegRightRule( up, _, a, _ ) =>
      val subProof = applyRecursive( f )( up )
      f( NegRightRule( subProof, a.formula ) )

    case ForallLeftRule( up, _, a, p, t ) =>
      val subProof = applyRecursive( f )( up )
      f( ForallLeftRule( subProof, a.formula, p.formula, t ) )

    case ExistsRightRule( up, _, a, p, t ) =>
      val subProof = applyRecursive( f )( up )
      f( ExistsRightRule( subProof, a.formula, p.formula, t ) )

    case ForallRightRule( up, _, a, p, v ) =>
      val subProof = applyRecursive( f )( up )
      f( ForallRightRule( subProof, a.formula, p.formula, v ) )

    case ExistsLeftRule( up, r, a, p, v ) =>
      val subProof = applyRecursive( f )( up )
      f( ExistsLeftRule( subProof, a.formula, p.formula, v ) )

    case DefinitionLeftRule( up, _, a, p ) =>
      val subProof = applyRecursive( f )( up )
      f( DefinitionLeftRule( subProof, a.formula, p.formula ) )

    case DefinitionRightRule( up, _, a, p ) =>
      val subProof = applyRecursive( f )( up )
      f( DefinitionRightRule( subProof, a.formula, p.formula ) )

    // Binary rules
    case CutRule( up1, up2, _, a1, a2 ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      f( CutRule( subProof1, subProof2, a1.formula ) )

    case AndRightRule( up1, up2, _, a1, a2, _ ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      f( AndRightRule( subProof1, subProof2, a1.formula, a2.formula ) )

    case OrLeftRule( up1, up2, r, a1, a2, _ ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      f( OrLeftRule( subProof1, subProof2, a1.formula, a2.formula ) )

    case ImpLeftRule( up1, up2, r, a1, a2, _ ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      f( ImpLeftRule( subProof1, subProof2, a1.formula, a2.formula ) )

    // TODO: change equation rules
    case EquationLeft1Rule( up1, up2, _, a1, a2, pos, _ ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      val ( a1New, a2New ) = ( subProof1.root.succedent.find( _ =^= a1 ), subProof2.root.antecedent.find( _ =^= a2 ) )
      if ( a1New.isEmpty || a2New.isEmpty )
        throw new LKRuleCreationException( "Couldn't find descendants of " + a1 + " and " + a2 + "." )
      f( EquationLeft1Rule( subProof1, subProof2, a1New.get, a2New.get, pos( 0 ) ) )

    case EquationLeft2Rule( up1, up2, _, a1, a2, pos, _ ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      val ( a1New, a2New ) = ( subProof1.root.succedent.find( _ =^= a1 ), subProof2.root.antecedent.find( _ =^= a2 ) )
      if ( a1New.isEmpty || a2New.isEmpty )
        throw new LKRuleCreationException( "Couldn't find descendants of " + a1 + " and " + a2 + "." )
      f( EquationLeft2Rule( subProof1, subProof2, a1New.get, a2New.get, pos( 0 ) ) )

    case EquationRight1Rule( up1, up2, _, a1, a2, pos, _ ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      val ( a1New, a2New ) = ( subProof1.root.succedent.find( _ =^= a1 ), subProof2.root.succedent.find( _ =^= a2 ) )
      if ( a1New.isEmpty || a2New.isEmpty )
        throw new LKRuleCreationException( "Couldn't find descendants of " + a1 + " and " + a2 + "." )
      f( EquationRight1Rule( subProof1, subProof2, a1New.get, a2New.get, pos( 0 ) ) )

    case EquationRight2Rule( up1, up2, _, a1, a2, pos, _ ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      val ( a1New, a2New ) = ( subProof1.root.succedent.find( _ =^= a1 ), subProof2.root.succedent.find( _ =^= a2 ) )
      if ( a1New.isEmpty || a2New.isEmpty )
        throw new LKRuleCreationException( "Couldn't find descendants of " + a1 + " and " + a2 + "." )
      f( EquationRight2Rule( subProof1, subProof2, a1New.get, a2New.get, pos( 0 ) ) )

    case InductionRule( up1, up2, _, a1, a2, a3, _, term ) =>
      val ( subProof1, subProof2 ) = ( apply( f )( up1 ), apply( f )( up2 ) )
      f( InductionRule( subProof1, subProof2, a1.formula.asInstanceOf[FOLFormula], a2.formula.asInstanceOf[FOLFormula], a3.formula.asInstanceOf[FOLFormula], term ) )

  }
}