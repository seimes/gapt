package at.logic.gapt.proofs.lk

import at.logic.gapt.expr._
import at.logic.gapt.expr.hol._
import at.logic.gapt.expr.schema._
import at.logic.gapt.expr.hol.isAtom
import at.logic.gapt.proofs.expansionTrees.{ BinaryExpansionTree, ExpansionSequent, ExpansionTree, ETStrongQuantifier, UnaryExpansionTree, ETWeakQuantifier, getETOfFormula, toShallow, ETAtom => AtomET, ETWeakening }
import at.logic.gapt.proofs.lk._
import at.logic.gapt.proofs.lk.base._
import at.logic.gapt.proofs.shlk._
import at.logic.gapt.provers.Prover

/**
 * Bottom-up construction of sequent calculus proofs.
 *
 * Currently supports propositional logic as well as proof construction using expansion trees.
 */
object solve extends at.logic.gapt.utils.logging.Logger {

  /**
   * Main method for solving propositional sequents
   * @param seq: sequent to prove
   * @param throwOnError: throw Exception if there is no proof
   * @return a proof if there is one
   */
  def solvePropositional( seq: HOLSequent, throwOnError: Boolean = false ): Option[LKProof] = {
    debug( "running solvePropositional" )

    if ( SolveUtils.noCommonAtoms( seq ) ) {
      trace( "no common atoms: " + seq )
      //      None
    }

    startProving( seq, new PropositionalProofStrategy, throwOnError )
  }

  /**
   * Transform expansion proof to LK proof (assumes that deep formula of expansionSequent is a tautology)
   */
  def expansionProofToLKProof( expansionSequent: ExpansionSequent ): Option[LKProof] = {
    expansionProofToLKProof( toShallow( expansionSequent ), expansionSequent )
  }

  /**
   * "Solving" for FOL: Use instances from expansion sequent to create LK proof for a sequent
   */
  def expansionProofToLKProof( seq: HOLSequent, expansionSequent: ExpansionSequent, throwOnError: Boolean = false ): Option[LKProof] = {
    debug( "\nrunning expansionProofToLKProof" )
    startProving( seq, new ExpansionTreeProofStrategy( expansionSequent ), throwOnError )
  }

  // internal interface method
  private def startProving( seq: HOLSequent, strategy: ProofStrategy, throwOnError: Boolean ): Option[LKProof] = {
    val seq_norm = HOLSequent( seq.antecedent.toSet.toList, seq.succedent.toSet.toList )

    prove( seq_norm, strategy ) match {
      case Some( p ) => {
        debug( "finished proof successfully" )
        Some( WeakeningMacroRule( p, seq ) )
      }
      case None =>
        if ( throwOnError ) throw new Exception( "Sequent is not provable." ) else None
    }
  }

  private def prove( seq: HOLSequent, strategy: ProofStrategy ): Option[LKProof] = {
    // we are only proving set-normalized sequents
    val ant_set = seq.antecedent.toSet
    val suc_set = seq.succedent.toSet
    assert( ant_set.size == seq.antecedent.size && suc_set.size == seq.succedent.size )

    trace( "proving: " + seq )
    trace( "with strat: " + strategy )

    // TODO: this should be refactored: the first case is for atomic axioms, the second
    // for sequents A :- A for arbitrary A. The first should be treated as special case
    // of the second.
    if ( SolveUtils.isAxiom( seq ) ) {
      val ( f, rest ) = SolveUtils.getAxiomfromSeq( seq )
      Some( Axiom( f :: Nil, f :: Nil ) )
    } else if ( SolveUtils.findNonschematicAxiom( seq ).isDefined ) {
      val Some( ( f, g ) ) = SolveUtils.findNonschematicAxiom( seq )
      Some( AtomicExpansion( HOLSequent( f :: Nil, g :: Nil ) ) )
    } else {

      trace( "no axiom, calc next step" )

      // main step: ask strategy what to do
      strategy.calcNextStep( seq ) match {
        case Some( action ) => {
          trace( "strategy has selected: " + action + " action.form: " + action.formula + "\n" )

          // apply whatever rule matches to this formula
          action.loc match {
            case ProofStrategy.FormulaLocation.Antecedent =>
              assert( seq.antecedent.contains( action.formula ) )
              applyActionAntecedent( action, seq )

            case ProofStrategy.FormulaLocation.Succedent =>
              assert( seq.succedent.contains( action.formula ) )
              applyActionSuccedent( action, seq )
          }
        }

        case None => None
      }
    }
  }

  private def applyActionAntecedent( action: ProofStrategy.Action, seq: HOLSequent ): Option[LKProof] = {
    // sequent without principal sequent to help building upper goal sequent
    val rest = HOLSequent( seq.antecedent.diff( action.formula :: Nil ), seq.succedent )
    // proof strategies for children (with expansion sequents according to children or no changes in the propositional case)
    val nextProofStrategies = action.getNextStrategies()

    val rv = action.formula match {

      // Quantifier Rules

      case All( v, f ) => {
        val quantifiedTerm = action.getQuantifiedTerm().get // must be defined in this case
        val auxFormula = Substitution( v, quantifiedTerm )( f )

        val p_ant = if ( seq.antecedent.contains( auxFormula ) ) seq.antecedent else auxFormula +: seq.antecedent
        val p_suc = seq.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof => {
          if ( proof.root.toHOLSequent.antecedent.contains( auxFormula ) && !rest.antecedent.contains( auxFormula ) ) {
            val proof1 = ForallLeftRule( proof, auxFormula, action.formula, quantifiedTerm )
            if ( proof.root.toHOLSequent.antecedent.contains( action.formula ) ) // main formula already appears in upper proof
              ContractionLeftRule( proof1, action.formula )
            else
              proof1
          } else
            proof
        } )
      }

      case Ex( v, f ) => {
        val eigenVar = action.getQuantifiedTerm().get.asInstanceOf[Var]
        val auxFormula = Substitution( v, eigenVar )( f )

        val p_ant = if ( seq.antecedent.contains( auxFormula ) ) rest.antecedent else auxFormula +: rest.antecedent
        val p_suc = seq.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof =>
          if ( proof.root.toHOLSequent.antecedent.contains( auxFormula ) && !rest.antecedent.contains( auxFormula ) )
            ExistsLeftRule( proof, auxFormula, action.formula, eigenVar )
          else
            proof )
      }

      // Nullary rules
      case Bottom() => Some( Axiom( seq ) ) // FIXME: add rules for top/bottom?

      // Unary Rules

      case Neg( f1 ) => {
        val p_ant = rest.antecedent
        val p_suc = if ( seq.succedent.contains( f1 ) ) seq.succedent else f1 +: rest.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof =>
          if ( proof.root.toHOLSequent.succedent.contains( f1 ) && !rest.succedent.contains( f1 ) )
            NegLeftRule( proof, f1 )
          else
            proof )
      }

      case And( f1, f2 ) => {
        val f1_opt = if ( rest.antecedent.contains( f1 ) ) Nil else f1 :: Nil
        val f2_opt = if ( ( f1_opt ++ rest.antecedent ).contains( f2 ) ) Nil else f2 :: Nil
        val p_ant = f1_opt ++ f2_opt ++ rest.antecedent
        val p_suc = rest.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof => {
          val infer_on_f1 = proof.root.toHOLSequent.antecedent.contains( f1 ) && !rest.antecedent.contains( f1 )
          val infer_on_f2 = proof.root.toHOLSequent.antecedent.contains( f2 ) && !( f1_opt ++ rest.antecedent ).contains( f2 )

          val proof1 = if ( infer_on_f1 ) AndLeft1Rule( proof, f1, f2 ) else proof
          val proof2 = if ( infer_on_f2 ) AndLeft2Rule( proof1, f1, f2 ) else proof1
          if ( infer_on_f1 && infer_on_f2 ) ContractionLeftRule( proof2, action.formula ) else proof2
        } )
      }

      // Binary Rules

      case Or( f1, f2 ) => {
        val p_ant1 = if ( rest.antecedent.contains( f1 ) ) rest.antecedent else f1 +: rest.antecedent
        val p_suc1 = rest.succedent
        val premise1 = HOLSequent( p_ant1, p_suc1 )

        prove( premise1, nextProofStrategies( 0 ) ) match {
          case Some( proof1 ) =>
            if ( proof1.root.toHOLSequent.antecedent.contains( f1 ) && !rest.antecedent.contains( f1 ) ) {
              val p_ant2 = if ( rest.antecedent.contains( f2 ) ) rest.antecedent else f2 +: rest.antecedent
              val p_suc2 = rest.succedent
              val premise2 = HOLSequent( p_ant2, p_suc2 )

              prove( premise2, nextProofStrategies( 1 ) ).map( proof2 =>
                if ( proof2.root.toHOLSequent.antecedent.contains( f2 ) && !rest.antecedent.contains( f2 ) )
                  ContractionMacroRule( OrLeftRule( proof1, proof2, f1, f2 ) )
                else
                  proof2 )
            } else {
              Some( proof1 )
            }
          case None => None
        }
      }

      case Imp( f1, f2 ) => {
        val p_ant1 = rest.antecedent
        val p_suc1 = if ( rest.succedent.contains( f1 ) ) rest.succedent else f1 +: rest.succedent
        val premise1 = HOLSequent( p_ant1, p_suc1 )

        prove( premise1, nextProofStrategies( 0 ) ) match {
          case Some( proof1 ) =>
            if ( proof1.root.toHOLSequent.succedent.contains( f1 ) && !rest.succedent.contains( f1 ) ) {
              val p_ant2 = if ( rest.antecedent.contains( f2 ) ) rest.antecedent else f2 +: rest.antecedent
              val p_suc2 = rest.succedent
              val premise2 = HOLSequent( p_ant2, p_suc2 )

              prove( premise2, nextProofStrategies( 1 ) ).map( proof2 =>
                if ( proof2.root.toHOLSequent.antecedent.contains( f2 ) && !rest.antecedent.contains( f2 ) )
                  ContractionMacroRule( ImpLeftRule( proof1, proof2, f1, f2 ) )
                else
                  proof2 )
            } else {
              Some( proof1 )
            }
          case None => None
        }
      }

      // Schematic Rules

      case BigAnd( i, iter, from, to ) =>
        val i = IntVar( "i" )
        if ( from == to ) {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val sf = subst( iter )
          val p_ant = sf +: rest.antecedent
          val p_suc = rest.succedent
          val premise = HOLSequent( p_ant, p_suc )
          prove( premise, nextProofStrategies( 0 ) ) match {
            case Some( proof ) =>
              val proof2 = AndLeftEquivalenceRule3( proof, sf, action.formula.asInstanceOf[SchemaFormula] )
              Some( proof2 )
            case None => None
          }
        } else {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val sf1 = BigAnd( i, iter, from, Pred( to ) )
          val sf2 = subst( iter )
          val p_ant = sf1 +: sf2 +: rest.antecedent
          val p_suc = rest.succedent
          val premise = HOLSequent( p_ant, p_suc )
          prove( premise, nextProofStrategies( 0 ) ) match {
            case Some( proof ) =>
              val proof1 = AndLeftRule( proof, sf1, sf2 )
              val and = And( BigAnd( i, iter, from, Pred( to ) ), subst( iter ) )
              val proof2 = AndLeftEquivalenceRule1( proof1, and, BigAnd( i, iter, from, to ) )
              Some( proof2 )
            case None => None
          }
        } // end of BigAnd

      case BigOr( i, iter, from, to ) =>
        val i = IntVar( "i" )
        if ( from == to ) {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val sf = subst( iter )
          val p_ant = sf +: rest.antecedent
          val p_suc = rest.succedent
          val premise = HOLSequent( p_ant, p_suc )
          prove( premise, nextProofStrategies( 0 ) ) match {
            case Some( proof ) =>
              val proof1 = OrLeftEquivalenceRule3( proof, sf, action.formula.asInstanceOf[SchemaFormula] )
              Some( proof1 )
            case None => None
          }
        } else {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val p_ant1 = BigOr( i, iter, from, Pred( to ) ) +: rest.antecedent
          val p_suc1 = rest.succedent
          val p_ant2 = subst( iter ) +: rest.antecedent
          val p_suc2 = rest.succedent
          val premise1 = HOLSequent( p_ant1, p_suc1 )
          val premise2 = HOLSequent( p_ant2, p_suc2 )
          prove( premise1, nextProofStrategies( 0 ) ) match {
            case Some( proof1 ) => prove( premise2, nextProofStrategies( 1 ) ) match {
              case Some( proof2 ) =>
                val proof3 = OrLeftRule( proof1, proof2, BigOr( i, iter, from, Pred( to ) ), subst( iter ) )
                val or = Or( BigOr( i, iter, from, Pred( to ) ), subst( iter ) )
                val proof4 = OrLeftEquivalenceRule1( proof3, or, BigOr( i, iter, from, to ) )
                val proof5 = ContractionMacroRule( proof4, seq, strict = false )
                Some( proof5 )
              case None => None
            }
            case None => None
          }
        } // end of BigOr

      case _ => throw new IllegalArgumentException( "Invalid formula in prove: " + action.formula )

    } // end of match formula

    // invariant: we have constructed a proof of a subsequent of seq
    if ( rv.isDefined ) assert( rv.get.root.toHOLSequent.isSubsetOf( seq ) )

    rv
  }

  private def applyActionSuccedent( action: ProofStrategy.Action, seq: HOLSequent ): Option[LKProof] = {
    val rest = HOLSequent( seq.antecedent, seq.succedent.diff( action.formula :: Nil ) )
    val nextProofStrategies = action.getNextStrategies()

    val rv = action.formula match {

      // Quantifier Rules

      case All( v, f ) => {
        val eigenVar = action.getQuantifiedTerm().get.asInstanceOf[Var]
        val auxFormula = Substitution( v, eigenVar )( f )

        val p_ant = rest.antecedent
        val p_suc = if ( rest.succedent.contains( auxFormula ) ) rest.succedent else auxFormula +: rest.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof =>
          if ( proof.root.toHOLSequent.succedent.contains( auxFormula ) && !rest.succedent.contains( auxFormula ) )
            ForallRightRule( proof, auxFormula, action.formula, eigenVar )
          else
            proof )
      }

      case Ex( v, f ) => {
        val quantifiedTerm = action.getQuantifiedTerm().get
        val auxFormula = Substitution( v, quantifiedTerm )( f )

        val p_ant = rest.antecedent
        val p_suc = if ( seq.succedent.contains( auxFormula ) ) seq.succedent else auxFormula +: seq.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof => {
          if ( proof.root.toHOLSequent.succedent.contains( auxFormula ) && !rest.succedent.contains( auxFormula ) ) {
            val proof1 = ExistsRightRule( proof, auxFormula, action.formula, quantifiedTerm )
            if ( proof.root.toHOLSequent.succedent.contains( action.formula ) )
              ContractionRightRule( proof1, action.formula )
            else
              proof1
          } else
            proof
        } )
      }

      // Nullary rules
      case Top() => Some( Axiom( seq ) ) // FIXME: add rules for top/bottom?

      // Unary Rules

      case Neg( f1 ) => {
        val p_ant = if ( rest.antecedent.contains( f1 ) ) rest.antecedent else f1 +: rest.antecedent
        val p_suc = rest.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof =>
          if ( proof.root.toHOLSequent.antecedent.contains( f1 ) && !rest.antecedent.contains( f1 ) )
            NegRightRule( proof, f1 )
          else
            proof )
      }

      case Imp( f1, f2 ) => {
        val p_ant = if ( rest.antecedent.contains( f1 ) ) rest.antecedent else f1 +: rest.antecedent
        val p_suc = if ( rest.succedent.contains( f2 ) ) rest.succedent else f2 +: rest.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof => {
          val infer_on_f1 = proof.root.toHOLSequent.antecedent.contains( f1 ) && !rest.antecedent.contains( f1 )
          val infer_on_f2 = proof.root.toHOLSequent.succedent.contains( f2 ) && !rest.succedent.contains( f2 )

          if ( infer_on_f1 || infer_on_f2 ) { // need to infer main formula
            val proof1 = if ( !infer_on_f1 ) WeakeningLeftRule( proof, f1 ) else proof
            val proof2 = if ( !infer_on_f2 ) WeakeningRightRule( proof1, f2 ) else proof1
            ImpRightRule( proof2, f1, f2 )
          } else {
            proof
          }
        } )
      }

      case Or( f1, f2 ) => {
        val f1_opt = if ( rest.succedent.contains( f1 ) ) Nil else f1 :: Nil
        val f2_opt = if ( ( f1_opt ++ rest.succedent ).contains( f2 ) ) Nil else f2 :: Nil
        val p_ant = rest.antecedent
        val p_suc = f1_opt ++ f2_opt ++ rest.succedent
        val premise = HOLSequent( p_ant, p_suc )

        prove( premise, nextProofStrategies( 0 ) ).map( proof => {
          val infer_on_f1 = proof.root.toHOLSequent.succedent.contains( f1 ) && !rest.succedent.contains( f1 )
          val infer_on_f2 = proof.root.toHOLSequent.succedent.contains( f2 ) && !( f1_opt ++ rest.succedent ).contains( f2 )

          val proof1 = if ( infer_on_f1 ) OrRight1Rule( proof, f1, f2 ) else proof
          val proof2 = if ( infer_on_f2 ) OrRight2Rule( proof1, f1, f2 ) else proof1
          if ( infer_on_f1 && infer_on_f2 ) ContractionRightRule( proof2, action.formula ) else proof2
        } )
      }

      // Binary Rules

      case And( f1, f2 ) => {
        val p_ant1 = rest.antecedent
        val p_suc1 = if ( rest.succedent.contains( f1 ) ) rest.succedent else f1 +: rest.succedent
        val premise1 = HOLSequent( p_ant1, p_suc1 )

        prove( premise1, nextProofStrategies( 0 ) ) match {
          case Some( proof1 ) =>
            if ( proof1.root.toHOLSequent.succedent.contains( f1 ) && !rest.succedent.contains( f1 ) ) {
              val p_ant2 = rest.antecedent
              val p_suc2 = if ( rest.succedent.contains( f2 ) ) rest.succedent else f2 +: rest.succedent
              val premise2 = HOLSequent( p_ant2, p_suc2 )

              prove( premise2, nextProofStrategies( 1 ) ).map( proof2 =>
                if ( proof2.root.toHOLSequent.succedent.contains( f2 ) && !rest.succedent.contains( f2 ) )
                  ContractionMacroRule( AndRightRule( proof1, proof2, f1, f2 ) )
                else
                  proof2 )
            } else {
              Some( proof1 )
            }
          case None => None
        }
      }

      // Schematic Rules

      case BigOr( i, iter, from, to ) =>
        val i = IntVar( "i" )
        if ( from == to ) {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val p_ant = subst( iter ) +: rest.antecedent
          val p_suc = rest.succedent
          val premise = HOLSequent( p_ant, p_suc )
          prove( premise, nextProofStrategies( 0 ) ) match {
            case Some( proof ) =>
              val proof1 = OrRightEquivalenceRule3( proof, subst( iter ), action.formula.asInstanceOf[SchemaFormula] )
              Some( proof1 )
            case None => None
          }
        } else {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val p_ant = rest.antecedent
          val p_suc = BigOr( i, iter, from, Pred( to ) ) +: subst( iter ) +: rest.succedent
          val premise = HOLSequent( p_ant, p_suc )
          prove( premise, nextProofStrategies( 0 ) ) match {
            case Some( proof ) =>
              val proof1 = OrRightRule( proof, BigOr( i, iter, from, Pred( to ) ), subst( iter ) )
              val or = Or( BigOr( i, iter, from, Pred( to ) ), subst( iter ) )
              val proof2 = OrRightEquivalenceRule1( proof1, or, BigOr( i, iter, from, to ) )
              Some( proof2 )
            case None => None
          }
        }

      case BigAnd( i, iter, from, to ) =>
        val i = IntVar( "i" )
        if ( from == to ) {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val p_ant = rest.antecedent
          val p_suc = subst( iter ) +: rest.succedent
          val premise = HOLSequent( p_ant, p_suc )
          prove( premise, nextProofStrategies( 0 ) ) match {
            case Some( proof ) =>
              val proof1 = AndRightEquivalenceRule3( proof, subst( iter ), action.formula.asInstanceOf[SchemaFormula] )
              Some( proof1 )
            case None => None
          }
        } else {
          val new_map = Map[Var, SchemaExpression]() + Tuple2( i, to )
          val subst = new SchemaSubstitution( new_map )
          val p_ant1 = rest.antecedent
          val p_suc1 = BigAnd( i, iter, from, Pred( to ) ) +: rest.succedent
          val p_ant2 = rest.antecedent
          val p_suc2 = subst( iter ) +: rest.succedent
          val premise1 = HOLSequent( p_ant1, p_suc1 )
          val premise2 = HOLSequent( p_ant2, p_suc2 )
          prove( premise1, nextProofStrategies( 0 ) ) match {
            case Some( proof1 ) => prove( premise2, nextProofStrategies( 1 ) ) match {
              case Some( proof2 ) =>
                val proof3 = AndRightRule( proof1, proof2, BigAnd( i, iter, from, Pred( to ) ), subst( iter ) )
                val and = And( BigAnd( i, iter, from, Pred( to ) ), subst( iter ) )
                val proof4 = AndRightEquivalenceRule1( proof3, and, BigAnd( i, iter, from, to ) )
                val proof5 = ContractionMacroRule( proof4, seq, strict = false )
                Some( proof5 )
              case None => None
            }
            case None => None
          }
        }

      case _ => throw new IllegalArgumentException( "Invalid formula in prove: " + action.formula )

    } // end of match formula

    // invariant: we have constructed a proof of a subsequent of seq
    if ( rv.isDefined ) assert( rv.get.root.toHOLSequent.isSubsetOf( seq ) )

    rv
  }
}

/**
 * Strategy to tell prove procedure which rules to apply
 *
 * A strategy selects a next action to execute. An action is represented by
 * a formula and the information whether this formula is in the antecedent
 * or the succedent. The action is to apply a rule to this formula.
 */
abstract class ProofStrategy {
  def calcNextStep( seq: HOLSequent ): Option[ProofStrategy.Action]
}
object ProofStrategy {
  object FormulaLocation extends Enumeration { val Succedent, Antecedent = Value }

  class Action( val formula: HOLFormula, val loc: FormulaLocation.Value, private val oldStrategy: Option[ProofStrategy] ) {
    override def toString() = "ProofStrategy.Action(" + formula + ", " + loc + ")"

    /**
     * Returns strategy to use for proving children
     */
    def getNextStrategies(): Seq[ProofStrategy] = {
      formula match { // either one or two branches
        case ( Or( _, _ ) | BigOr( _, _, _, _ ) | Imp( _, _ ) ) if loc == FormulaLocation.Antecedent => List( oldStrategy.get, oldStrategy.get )
        case ( And( _, _ ) | BigAnd( _, _, _, _ ) ) if loc == FormulaLocation.Succedent => List( oldStrategy.get, oldStrategy.get )
        case _ => List( oldStrategy.get )
      }
    }

    def getQuantifiedTerm(): Option[LambdaExpression] = None
  }
}

/**
 * Strategy for proving propositional sequents.
 */
class PropositionalProofStrategy extends ProofStrategy with at.logic.gapt.utils.logging.Logger {
  val FormulaLocation = ProofStrategy.FormulaLocation // shortcut

  override def calcNextStep( seq: HOLSequent ): Option[ProofStrategy.Action] = {

    if ( SolveUtils.isAxiom( seq ) || SolveUtils.findNonschematicAxiom( seq ).isDefined ) {
      throw new RuntimeException( "Prove strategy called on axiom: " + seq )
    } else {

      // rule preference:
      // NOTE: getOrElse uses call by name, i.e. functions below are only evaluated if really needed
      findNullaryLeft( seq ).orElse(
        findNullaryRight( seq ).orElse(
          findUnaryLeft( seq ).orElse(
            findUnaryRight( seq ).orElse(
              findBinaryLeft( seq ).orElse(
                findBinaryRight( seq ).orElse(
                  {
                    debug( "PropositionalProofStrategy is unable to find a rule to apply on: " + seq )
                    None
                  }
                )
              )
            )
          )
        )
      )
    }
  }

  def findNullaryLeft( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.antecedent.find( f => f match {
      case Bottom() => true
      case _        => false
    } ).map( new ProofStrategy.Action( _, FormulaLocation.Antecedent, Some( this ) ) )
  def findNullaryRight( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.succedent.find( f => f match {
      case Top() => true
      case _     => false
    } ).map( new ProofStrategy.Action( _, FormulaLocation.Succedent, Some( this ) ) )

  // Tries to find a formula on the left or on the right such that its
  // introduction rule is unary.
  def findUnaryLeft( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.antecedent.find( f => f match {
      case Neg( _ ) | And( _, _ ) | BigAnd( _, _, _, _ ) => true
      case _ => false
    } ).map( new ProofStrategy.Action( _, FormulaLocation.Antecedent, Some( this ) ) )
  def findUnaryRight( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.succedent.find( f => f match {
      case Neg( _ ) | Imp( _, _ ) | Or( _, _ ) | BigOr( _, _, _, _ ) => true
      case _ => false
    } ).map( new ProofStrategy.Action( _, FormulaLocation.Succedent, Some( this ) ) )

  // Tries to find a formula on the left or on the right such that its
  // introduction rule is binary.
  def findBinaryLeft( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.antecedent.find( f => f match {
      case Imp( _, _ ) | Or( _, _ ) | BigOr( _, _, _, _ ) => true
      case _ => false
    } ).map( new ProofStrategy.Action( _, FormulaLocation.Antecedent, Some( this ) ) )
  def findBinaryRight( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.succedent.find( f => f match {
      case And( _, _ ) | BigAnd( _, _, _, _ ) => true
      case _                                  => false
    } ).map( new ProofStrategy.Action( _, FormulaLocation.Succedent, Some( this ) ) )

}

/**
 * Strategy for constructing a proof from an ExpansionSequent.
 *
 * The internal state of this strategy is an ExpansionSequent. The action is
 * a formula on a side of the sequent plus a witness term or eigenvariable
 * respectively in case this formula starts with a quantifier.
 */
class ExpansionTreeProofStrategy( val expansionSequent: ExpansionSequent ) extends PropositionalProofStrategy with at.logic.gapt.utils.logging.Logger {

  override def toString(): String = "ExpansionTreeProofStrategy(" + expansionSequent + ")"

  override def calcNextStep( seq: HOLSequent ): Option[ProofStrategy.Action] = {
    if ( SolveUtils.isAxiom( seq ) || SolveUtils.findNonschematicAxiom( seq ).isDefined ) {
      throw new RuntimeException( "Prove strategy called on axiom: " + seq )
    } else {
      // every possible action (i.e. formula in toShallow( expansionSequent )) must be realizable (in seq)
      assert( toShallow( expansionSequent ).isSubsetOf( seq ) )

      val goal_pruned = removeWeakFormulas( seq )

      // rule preference:
      // NOTE: getOrElse uses call by name, i.e. functions below are only evaluated if really needed
      findUnaryLeft( goal_pruned ).orElse(
        findUnaryRight( goal_pruned ).orElse(

          findStrongQuantifier( goal_pruned ).orElse( // can always apply strong quantifier
            findWeakQuantifier( goal_pruned ).orElse( // weak before binary rules since it's unary

              findBinaryLeft( goal_pruned ).orElse(
                findBinaryRight( goal_pruned ).orElse(
                  {
                    debug( "ExpansionTreeProofStrategy is unable to find a rule to apply on: " + seq )
                    None
                  }
                )
              )
            )
          )
        )
      )
    }
  }

  /**
   * Remove all formulas from seq which correspond to top-level ETWeakening-nodes in expansionSequent
   *
   * This assumes that Shallow( expansionSequent ) is a subset of seq and that there are no duplicate
   * formulas in seq.
   */
  private def removeWeakFormulas( seq: HOLSequent ) = {
    val w_ant = expansionSequent.antecedent.filter( e => e match {
      case ETWeakening( _ ) => true
      case _                => false
    } ).map( toShallow( _ ) )
    val w_suc = expansionSequent.succedent.filter( e => e match {
      case ETWeakening( _ ) => true
      case _                => false
    } ).map( toShallow( _ ) )

    HOLSequent( seq.antecedent.filterNot( w_ant.contains( _ ) ), seq.succedent.filterNot( w_suc.contains( _ ) ) )
  }

  // TODO:  why do find... operate on seq, would it not make more sense to have them work on expansionSequent?
  //        in particular since we have assert( toShallow( expansionSequent ).isSubsetOf( seq ) )

  /**
   * need to override find-methods as we keep track of the state of the expansion sequent here
   */
  override def findUnaryLeft( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.antecedent.find( f => f match {
      case Neg( _ ) | And( _, _ ) => true
      case BigAnd( _, _, _, _ )   => throw new IllegalArgumentException( "Found BigAnd in expansionProofToLKProof (Schema formulas are not supported by expansion trees)" )
      case _                      => false
    } ).map( formula => formula match {
      case Neg( f1 ) =>
        trace( "found neg left; exp seq: " + expansionSequent + "; formula: " + formula )
        val et = getETOfFormula( expansionSequent, formula, isAntecedent = true ).get
        val etSeq1 = expansionSequent.removeFromAntecedent( et ).addToSuccedent( et.asInstanceOf[UnaryExpansionTree].children( 0 )._1.asInstanceOf[ExpansionTree] )
        val ps1 = new ExpansionTreeProofStrategy( etSeq1 )
        new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Antecedent, None, List[ProofStrategy]( ps1 ) )
      case And( f1, f2 ) =>
        val et = getETOfFormula( expansionSequent, formula, isAntecedent = true ).get
        val etSeq =
          expansionSequent
            .removeFromAntecedent( et )
            .addToAntecedent( et.asInstanceOf[BinaryExpansionTree].children( 0 )._1.asInstanceOf[ExpansionTree] )
            .addToAntecedent( et.asInstanceOf[BinaryExpansionTree].children( 1 )._1.asInstanceOf[ExpansionTree] )
        val ps1 = new ExpansionTreeProofStrategy( etSeq )
        new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Antecedent, None, List[ProofStrategy]( ps1 ) )
    } )

  override def findUnaryRight( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.succedent.find( f => f match {
      case Neg( _ ) | Imp( _, _ ) | Or( _, _ ) => true
      case BigOr( _, _, _, _ )                 => throw new IllegalArgumentException( "Found BigOr in expansionProofToLKProof (Schema formulas are not supported by expansion trees)" )
      case _                                   => false
    } ).map( formula => formula match {
      case Neg( f1 ) =>
        val et = getETOfFormula( expansionSequent, formula, isAntecedent = false ).get
        val etSeq1 = expansionSequent.removeFromSuccedent( et ).addToAntecedent( et.asInstanceOf[UnaryExpansionTree].children( 0 )._1.asInstanceOf[ExpansionTree] )
        val ps1 = new ExpansionTreeProofStrategy( etSeq1 )
        new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Succedent, None, List[ProofStrategy]( ps1 ) )
      case Imp( f1, f2 ) =>
        val et = getETOfFormula( expansionSequent, formula, isAntecedent = false ).get
        val etSeq = expansionSequent
          .replaceInSuccedent( et, et.asInstanceOf[BinaryExpansionTree].children( 1 )._1.asInstanceOf[ExpansionTree] )
          .addToAntecedent( et.asInstanceOf[BinaryExpansionTree].children( 0 )._1.asInstanceOf[ExpansionTree] )
          .asInstanceOf[ExpansionSequent]
        val ps1 = new ExpansionTreeProofStrategy( etSeq )
        new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Succedent, None, List[ProofStrategy]( ps1 ) )
      case Or( f1, f2 ) =>
        val et = getETOfFormula( expansionSequent, formula, isAntecedent = false ).get
        val etSeq = expansionSequent
          .replaceInSuccedent( et, et.asInstanceOf[BinaryExpansionTree].children( 1 )._1.asInstanceOf[ExpansionTree] )
          .addToSuccedent( et.asInstanceOf[BinaryExpansionTree].children( 0 )._1.asInstanceOf[ExpansionTree] )
          .asInstanceOf[ExpansionSequent]
        val ps1 = new ExpansionTreeProofStrategy( etSeq )
        new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Succedent, None, List[ProofStrategy]( ps1 ) )
    } )

  override def findBinaryRight( seq: HOLSequent ): Option[ProofStrategy.Action] =
    seq.succedent.find( f => f match {
      case And( _, _ )          => true
      case BigAnd( _, _, _, _ ) => throw new IllegalArgumentException( "Found BigAnd in expansionProofToLKProof (Schema formulas are not supported by expansion trees)" )
      case _                    => false
    } ).map( formula => {
      // prepare new proof strategies for children
      val et = getETOfFormula( expansionSequent, formula, isAntecedent = false ).get
      val etSeq1 = expansionSequent.replaceInSuccedent( et, et.asInstanceOf[BinaryExpansionTree].children( 0 )._1.asInstanceOf[ExpansionTree] ).asInstanceOf[ExpansionSequent]
      val etSeq2 = expansionSequent.replaceInSuccedent( et, et.asInstanceOf[BinaryExpansionTree].children( 1 )._1.asInstanceOf[ExpansionTree] ).asInstanceOf[ExpansionSequent]
      val ps1 = new ExpansionTreeProofStrategy( etSeq1 )
      val ps2 = new ExpansionTreeProofStrategy( etSeq2 )
      new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Succedent, None, List[ProofStrategy]( ps1, ps2 ) )
    } )

  override def findBinaryLeft( seq: HOLSequent ): Option[ProofStrategy.Action] = {
    seq.antecedent.find( f => f match {
      case Imp( _, _ ) | Or( _, _ ) => true
      case BigOr( _, _, _, _ )      => throw new IllegalArgumentException( "Found BigOr in expansionProofToLKProof (Schema formulas are not supported by expansion trees)" )
      case _                        => false
    } ).map( formula => formula match {
      // differentiate again between Imp and Or as formulas appear in different locations when proving
      case Imp( _, _ ) => {
        trace( "found imp left; exp seq: " + expansionSequent + "; formula: " + formula )
        val et = getETOfFormula( expansionSequent, formula, isAntecedent = true ).get
        val children = et.asInstanceOf[BinaryExpansionTree].children // children are Tuple2(ET, Option[Formula])
        val etSeqPurged = expansionSequent.removeFromAntecedent( et )
        val etSeq1 = new ExpansionSequent( etSeqPurged.antecedent, children( 0 )._1.asInstanceOf[ExpansionTree] +: etSeqPurged.succedent )
        val etSeq2 = new ExpansionSequent( children( 1 )._1.asInstanceOf[ExpansionTree] +: etSeqPurged.antecedent, etSeqPurged.succedent )
        val ps1 = new ExpansionTreeProofStrategy( etSeq1 )
        val ps2 = new ExpansionTreeProofStrategy( etSeq2 )
        new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Antecedent, None, List[ProofStrategy]( ps1, ps2 ) )
      }
      case Or( _, _ ) => {
        val et = getETOfFormula( expansionSequent, formula, isAntecedent = true ).get
        val etSeq1 = expansionSequent.replaceInAntecedent( et, et.asInstanceOf[BinaryExpansionTree].children( 0 )._1.asInstanceOf[ExpansionTree] ).asInstanceOf[ExpansionSequent]
        val etSeq2 = expansionSequent.replaceInAntecedent( et, et.asInstanceOf[BinaryExpansionTree].children( 1 )._1.asInstanceOf[ExpansionTree] ).asInstanceOf[ExpansionSequent]
        val ps1 = new ExpansionTreeProofStrategy( etSeq1 )
        val ps2 = new ExpansionTreeProofStrategy( etSeq2 )
        new ExpansionTreeProofStrategy.ExpansionTreeAction( formula, FormulaLocation.Antecedent, None, List[ProofStrategy]( ps1, ps2 ) )
      }
    } )
  }

  def findStrongQuantifier( seq: HOLSequent ): Option[ExpansionTreeProofStrategy.ExpansionTreeAction] = {
    // find one instance, can just use it
    // return etseq (in strategy) with formula removed, but keep instance

    val anteResult = expansionSequent.antecedent.collectFirst( {
      case et @ ETStrongQuantifier( formula, variable, selection ) =>
        val newEtSeq = expansionSequent.replaceInAntecedent( et, selection.asInstanceOf[ExpansionTree] ).asInstanceOf[ExpansionSequent]
        new ExpansionTreeProofStrategy.ExpansionTreeAction( toShallow( et ), FormulaLocation.Antecedent, Some( variable ),
          List( new ExpansionTreeProofStrategy( newEtSeq ) ) )
    } )

    anteResult.orElse(
      expansionSequent.succedent.collectFirst( {
        case et @ ETStrongQuantifier( formula, variable, selection ) =>
          val newEtSeq = expansionSequent.replaceInSuccedent( et, selection.asInstanceOf[ExpansionTree] ).asInstanceOf[ExpansionSequent]
          new ExpansionTreeProofStrategy.ExpansionTreeAction( toShallow( et ), FormulaLocation.Succedent, Some( variable ),
            List( new ExpansionTreeProofStrategy( newEtSeq ) ) )
      } )
    )
  }

  /**
   * Check if *any* of vars appears as strong quantifier somewhere in et
   * Naive approach: always check everything.
   * This data does not really change (except on et seq changes), so it could be cached/precalculated for efficiency in the future
   */
  private def doVariablesAppearInStrongQuantifier( vars: Set[Var], et: ExpansionTree ): Boolean = {
    et match {
      case ETStrongQuantifier( formula, v, sel ) =>
        vars.contains( v ) || doVariablesAppearInStrongQuantifier( vars, sel )
      case ETWeakQuantifier( formula, instances ) =>
        instances.exists( entry => doVariablesAppearInStrongQuantifier( vars, entry._1 ) )
      case BinaryExpansionTree( child1, child2 ) =>
        doVariablesAppearInStrongQuantifier( vars, child1 ) || doVariablesAppearInStrongQuantifier( vars, child2 )
      case UnaryExpansionTree( child1 ) => doVariablesAppearInStrongQuantifier( vars, child1 )
      case AtomET( _ )                  => false
      case ETWeakening( _ )             => false
    }
  }

  def findWeakQuantifier( seq: HOLSequent ): Option[ExpansionTreeProofStrategy.ExpansionTreeAction] = {
    // check which of the terms to use (just ones that aren't there yet)
    // return etseq (in strat) with instance removed
    // -> check for:
    // any of set of variables of term used as expansion term in weak quantifier expansion is used as strong quantifier elsewhere (and hasn't been reduced so far, i.e. appears in current expansion sequent)
    // also in cyclicity condition: expand outer instantiations before inner (can't magically make inner part of formula appear, only rule by rule). this is done automatically if only outermost occurences of weak
    // quantifier instances are checked here

    def getFirstApplicableInstanceOfWeakQuantifier( instances: Seq[( ExpansionTree, LambdaExpression )] ) = {
      val firstApplicable = instances.find( inst => inst match {
        case ( et: ExpansionTree, term: LambdaExpression ) =>
          // check if free variables of term appear in any strong quantifier
          val vars = freeVariables( term )
          val doVarsAppear = doVariablesAppearInStrongQuantifier( vars, _: ExpansionTree )
          val canUseInstance = expansionSequent.succedent.forall( !doVarsAppear( _ ) ) && expansionSequent.antecedent.forall( !doVarsAppear( _ ) )
          canUseInstance
      } )
      firstApplicable
    }

    val anteResult: Option[ExpansionTreeProofStrategy.ExpansionTreeAction] = expansionSequent.antecedent.foldLeft( None: Option[ExpansionTreeProofStrategy.ExpansionTreeAction] )( ( old, et ) =>
      // want to return first match, so return old if defined or check next
      old.orElse( {
        et match {
          case ETWeakQuantifier( formula, instances ) =>
            getFirstApplicableInstanceOfWeakQuantifier( instances ).map( instancePicked => {
              val newInstances = instances.filterNot( _ eq instancePicked )
              // drop et as soon as all instances have been picked (from etseq, will stick in actual sequent for simplicity but never be chosen)
              val newEtSeq0 =
                if ( newInstances.isEmpty ) { expansionSequent.removeFromAntecedent( et ) }
                else { expansionSequent.replaceInAntecedent( et, ETWeakQuantifier.applyWithoutMerge( formula, newInstances ) ) }
              val newEtSeq = newEtSeq0.addToAntecedent( instancePicked._1 ).asInstanceOf[ExpansionSequent]
              new ExpansionTreeProofStrategy.ExpansionTreeAction( toShallow( et ), FormulaLocation.Antecedent, Some( instancePicked._2 ),
                List( new ExpansionTreeProofStrategy( newEtSeq ) ) )
            } )
          case _ => None
        }
      } ) )

    if ( anteResult.isDefined ) { // this should be anteResult.getOrElse (as anywhere else), but the scala compiler tries really hard to prevent this, so who am i to force it..
      anteResult
    } else {
      val succResult: Option[ExpansionTreeProofStrategy.ExpansionTreeAction] =
        expansionSequent.succedent.foldLeft( None: Option[ExpansionTreeProofStrategy.ExpansionTreeAction] )( ( old, et ) =>
          // want to return first match, so return old if defined or check next
          old.orElse( {
            et match {
              case ETWeakQuantifier( formula, instances ) =>
                getFirstApplicableInstanceOfWeakQuantifier( instances ).map( instancePicked => {
                  val newInstances = instances.filterNot( _ eq instancePicked )
                  // drop et as soon as all instances have been picked
                  val newEtSeq0 =
                    if ( newInstances.isEmpty ) { expansionSequent.removeFromSuccedent( et ) }
                    else { expansionSequent.replaceInSuccedent( et, ETWeakQuantifier.applyWithoutMerge( formula, newInstances ) ) }
                  val newEtSeq = newEtSeq0.addToSuccedent( instancePicked._1 ).asInstanceOf[ExpansionSequent]
                  new ExpansionTreeProofStrategy.ExpansionTreeAction( toShallow( et ), FormulaLocation.Succedent, Some( instancePicked._2 ),
                    List( new ExpansionTreeProofStrategy( newEtSeq ) ) )
                } )
              case _ => None
            }
          } ) )
      succResult
    }
  }
}

object ExpansionTreeProofStrategy {
  class ExpansionTreeAction( override val formula: HOLFormula, override val loc: ProofStrategy.FormulaLocation.Value,
                             val quantifiedTerm: Option[LambdaExpression], val subStrategy: Seq[ProofStrategy] )
      extends ProofStrategy.Action( formula, loc, None ) {
    override def toString() = "ExpansionTreeAction(" + formula + ", " + loc + ", " + quantifiedTerm + "," + subStrategy + ")"
    override def getNextStrategies(): Seq[ProofStrategy] = subStrategy
    override def getQuantifiedTerm(): Option[LambdaExpression] = quantifiedTerm
  }
}

private object SolveUtils extends at.logic.gapt.utils.logging.Logger {
  // Checks if the sequent is of the form A, \Gamma |- A, \Delta
  def isAxiom( seq: HOLSequent ): Boolean = {
    seq.antecedent.exists( f =>
      isAtom( f ) &&
        seq.succedent.exists( f2 =>
          f.syntaxEquals( f2 ) ) )
  }

  def findNonschematicAxiom( seq: HOLSequent ): Option[( HOLFormula, HOLFormula )] = {
    val axs = for (
      f <- seq.antecedent.toList;
      g <- seq.succedent.toList;
      if f == g && isNotSchematic( f )
    ) yield { ( f, g ) }

    axs match {
      case Nil           => None
      case ( f, g ) :: _ => Some( ( f, g ) )
    }
  }

  private def isNotSchematic( f: HOLFormula ): Boolean = f match {
    case Neg( l )             => isNotSchematic( l.asInstanceOf[HOLFormula] )
    case And( l, r )          => isNotSchematic( l.asInstanceOf[HOLFormula] ) && isNotSchematic( r.asInstanceOf[HOLFormula] )
    case Or( l, r )           => isNotSchematic( l.asInstanceOf[HOLFormula] ) && isNotSchematic( r.asInstanceOf[HOLFormula] )
    case Imp( l, r )          => isNotSchematic( l.asInstanceOf[HOLFormula] ) && isNotSchematic( r.asInstanceOf[HOLFormula] )
    case All( _, l )          => isNotSchematic( l.asInstanceOf[HOLFormula] )
    case Ex( _, l )           => isNotSchematic( l.asInstanceOf[HOLFormula] )
    case BigAnd( _, _, _, _ ) => false
    case BigOr( _, _, _, _ )  => false
    case HOLAtom( _, _ )      => true
    case Bottom() | Top()     => true
    case _                    => warn( "WARNING: Unexpected operator in test for schematic formula " + f ); false
  }

  def getAxiomfromSeq( seq: HOLSequent ): ( HOLFormula, HOLSequent ) = {
    if ( isAxiom( seq ) ) {
      seq.antecedent.foreach( f => if ( seq.succedent.contains( f ) ) {
        return ( f, removeFfromSeqAnt( removeFfromSeqSucc( seq, f ), f ) )
      } )
      throw new Exception( "\nError in if-autoprop.getAxiomfromSeq !\n" )
    } else throw new Exception( "\nError in else-autoprop.getAxiomfromSeq !\n" )
  }

  def removeFfromSeqAnt( seq: HOLSequent, f: HOLFormula ): HOLSequent = {
    HOLSequent( seq.antecedent.filter( x => x != f ), seq.succedent )
  }

  def removeFfromSeqSucc( seq: HOLSequent, f: HOLFormula ): HOLSequent = {
    HOLSequent( seq.antecedent, seq.succedent.filter( x => x != f ) )
  }

  def removeFfromSeqAnt( seq: HOLSequent, flist: List[HOLFormula] ): HOLSequent = {
    HOLSequent( seq.antecedent.filter( x => !flist.contains( x ) ), seq.succedent )
  }

  def removeFfromSeqSucc( seq: HOLSequent, flist: List[HOLFormula] ): HOLSequent = {
    HOLSequent( seq.antecedent, seq.succedent.filter( x => !flist.contains( x ) ) )
  }

  def removefromExpSeqAnt( seq: ExpansionSequent, f: HOLFormula ): ExpansionSequent = {
    getETOfFormula( seq, f, /*isAntecedent*/ true ) match {
      case Some( et ) => seq.removeFromAntecedent( et )
      case None       => throw new IllegalArgumentException( "Formula " + f + " not contained in expansion sequent " + seq )
    }
  }

  def removefromExpSeqSucc( seq: ExpansionSequent, f: HOLFormula ): ExpansionSequent = {
    getETOfFormula( seq, f, /*isAntecedent*/ false ) match {
      case Some( et ) => seq.removeFromSuccedent( et )
      case None       => throw new IllegalArgumentException( "Formula " + f + " not contained in expansion sequent " + seq )
    }
  }

  // Checks if the atoms occurring in seq are all different (if so, the sequent
  // is not provable.
  def noCommonAtoms( seq: HOLSequent ): Boolean = {
    val ats = atoms( seq )
    ats.size == ats.toSet.size
  }
}

class LKProver extends Prover {
  def getLKProof( seq: HOLSequent ): Option[LKProof] = solve.solvePropositional( seq )
}

object AtomicExpansion {

  /*  === implements algorithm from Lemma 4.1.1 in Methods of Cut-Elimination === */
  /* given a sequent S = F :- F for an arbitrary formula F, derive a proof of S from atomic axioms
   * CAUTION: Does not work on schematic formulas! Reason: No match for BigAnd/BigOr, schema substitution is special. */
  def apply( fs: HOLSequent ): LKProof = {
    //find a formula occurring on both sides
    SolveUtils.findNonschematicAxiom( fs ) match {
      case ( Some( ( f, g ) ) ) =>
        apply( fs, f, g )
      case None =>
        throw new Exception( "Could not find a (non-schematic) formula in " + fs + " which occurs on both sides!" )
    }
  }

  def apply( p: LKProof ): LKProof = expandProof( p )

  /* Same as apply(fs:FSequent) but you can specify the formula on the lhs (f1) and rhs (f2) */
  def apply( fs: HOLSequent, f1: HOLFormula, f2: HOLFormula ) = {

    val atomic_proof = atomicExpansion_( f1, f2 )

    WeakeningMacroRule( atomic_proof, fs )
  }

  // assumes f1 == f2
  private def atomicExpansion_( f1: HOLFormula, f2: HOLFormula ): LKProof = {
    try {
      ( f1, f2 ) match {
        case ( Bottom(), Bottom() ) => Axiom( HOLSequent( Seq( Bottom() ), Seq( Bottom() ) ) )
        case ( Top(), Top() )       => Axiom( HOLSequent( Seq( Top() ), Seq( Top() ) ) )

        case ( Neg( l1 ), Neg( l2 ) ) =>
          val parent = atomicExpansion_( l1, l2 )
          NegLeftRule( NegRightRule( parent, l1 ), l2 )

        case ( And( l1, r1 ), And( l2, r2 ) ) =>
          val parent1 = atomicExpansion_( l1, l2 )
          val parent2 = atomicExpansion_( r1, r2 )
          val i1 = AndLeft1Rule( parent1, l1, r1 )
          val i2 = AndLeft2Rule( parent2, l2, r2 )
          val i3 = AndRightRule( i1, i2, l1, r1 )
          ContractionLeftRule( i3, f1 )

        case ( Or( l1, r1 ), Or( l2, r2 ) ) =>
          val parent1 = atomicExpansion_( l1, l2 )
          val parent2 = atomicExpansion_( r1, r2 )
          val i1 = OrRight1Rule( parent1, l1, r1 )
          val i2 = OrRight2Rule( parent2, l2, r2 )
          val i3 = OrLeftRule( i1, i2, l1, r1 )
          ContractionRightRule( i3, f1 )

        case ( Imp( l1, r1 ), Imp( l2, r2 ) ) =>
          val parent1 = atomicExpansion_( l1, l2 )
          val parent2 = atomicExpansion_( r1, r2 )
          val i1 = ImpLeftRule( parent1, parent2, l1, r1 )
          ImpRightRule( i1, l2, r2 )

        case ( All( x1: Var, l1 ), All( x2: Var, l2 ) ) =>
          val eigenvar = rename( x1, freeVariables( l1 ).toList ++ freeVariables( l2 ).toList )
          val sub1 = Substitution( List( ( x1, eigenvar ) ) )
          val sub2 = Substitution( List( ( x2, eigenvar ) ) )
          val aux1 = sub1( l1 )
          val aux2 = sub2( l2 )

          val parent = atomicExpansion_( aux1, aux2 )
          val i1 = ForallLeftRule( parent, aux1, f1, eigenvar )
          ForallRightRule( i1, aux2, f2, eigenvar )

        case ( Ex( x1: Var, l1 ), Ex( x2: Var, l2 ) ) =>
          val eigenvar = rename( x1, freeVariables( l1 ).toList ++ freeVariables( l2 ).toList )
          val sub1 = Substitution( List( ( x1, eigenvar ) ) )
          val sub2 = Substitution( List( ( x2, eigenvar ) ) )
          val aux1 = sub1( l1 )
          val aux2 = sub2( l2 )

          val parent = atomicExpansion_( aux1, aux2 )
          val i1 = ExistsRightRule( parent, aux2, f2, eigenvar )
          ExistsLeftRule( i1, aux1, f1, eigenvar )

        case ( a1, a2 ) if isAtom( a1 ) && isAtom( a2 ) =>
          Axiom( a1 :: Nil, a2 :: Nil )

        case _ =>
          throw new Exception( "" + f1 + " and "
            + f2 + " do not have the same outermost operator or operator unhandled!" )

      }
    } catch {
      case e: Exception =>
        throw new Exception( "Error in non-atomic axiom expansion handling " + f1 + " and " + f2 + ": " + e.getMessage, e )
    }
  }

  def expandProof( p: LKProof ): LKProof = p match {
    case Axiom( seq @ OccSequent( antd, succd ) ) =>
      val tautology_formulas = for ( a <- antd; s <- succd; if a.formula == s.formula && !isAtom( a.formula ) ) yield { a.formula }
      if ( tautology_formulas.nonEmpty ) {
        val tf = tautology_formulas( 0 )
        //println("Expanding "+tf)
        AtomicExpansion( seq.toHOLSequent, tf, tf )
      } else {
        p
      }

    //structural rules
    case ContractionLeftRule( uproof, root, aux1, aux2, _ ) =>
      val duproof = expandProof( uproof )
      ContractionLeftRule( duproof, aux1.formula )
    case ContractionRightRule( uproof, root, aux1, aux2, _ ) =>
      val duproof = expandProof( uproof )
      ContractionRightRule( duproof, aux1.formula )
    case WeakeningLeftRule( uproof, root, aux1 ) =>
      val duproof = expandProof( uproof )
      WeakeningLeftRule( duproof, aux1.formula )
    case WeakeningRightRule( uproof, root, aux1 ) =>
      val duproof = expandProof( uproof )
      WeakeningRightRule( duproof, aux1.formula )
    case CutRule( uproof1, uproof2, root, aux1, aux2 ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      CutRule( duproof1, duproof2, aux1.formula )

    //Unary Logical rules
    case NegLeftRule( uproof, root, aux1, _ ) =>
      val duproof = expandProof( uproof )
      NegLeftRule( duproof, aux1.formula )
    case NegRightRule( uproof, root, aux1, _ ) =>
      val duproof = expandProof( uproof )
      NegRightRule( duproof, aux1.formula )
    case ImpRightRule( uproof, root, aux1, aux2, _ ) =>
      val duproof = expandProof( uproof )
      ImpRightRule( duproof, aux1.formula, aux2.formula )
    case OrRight1Rule( uproof, root, aux1, prin ) =>
      val duproof = expandProof( uproof )
      val f = prin.formula match { case Or( _, x ) => x }
      OrRight1Rule( duproof, aux1.formula, f )
    case OrRight2Rule( uproof, root, aux1, prin ) =>
      val duproof = expandProof( uproof )
      val f = prin.formula match { case Or( x, _ ) => x }
      OrRight2Rule( duproof, f, aux1.formula )
    case AndLeft1Rule( uproof, root, aux1, prin ) =>
      val duproof = expandProof( uproof )
      val f = prin.formula match { case And( _, x ) => x }
      AndLeft1Rule( duproof, aux1.formula, f )
    case AndLeft2Rule( uproof, root, aux1, prin ) =>
      val duproof = expandProof( uproof )
      val f = prin.formula match { case And( x, _ ) => x }
      AndLeft2Rule( duproof, f, aux1.formula )

    //Binary Logical Rules
    case ImpLeftRule( uproof1, uproof2, root, aux1, aux2, prin ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      ImpLeftRule( duproof1, duproof2, aux1.formula, aux2.formula )
    case OrLeftRule( uproof1, uproof2, root, aux1, aux2, prin ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      OrLeftRule( duproof1, duproof2, aux1.formula, aux2.formula )
    case AndRightRule( uproof1, uproof2, root, aux1, aux2, prin ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      AndRightRule( duproof1, duproof2, aux1.formula, aux2.formula )

    //Quantifier Rules
    case ForallLeftRule( uproof, root, aux, prin, sub ) =>
      val duproof = expandProof( uproof )
      ForallLeftRule( duproof, aux.formula, prin.formula, sub )
    case ForallRightRule( uproof, root, aux, prin, sub ) =>
      val duproof = expandProof( uproof )
      ForallRightRule( duproof, aux.formula, prin.formula, sub )
    case ExistsLeftRule( uproof, root, aux, prin, sub ) =>
      val duproof = expandProof( uproof )
      ExistsLeftRule( duproof, aux.formula, prin.formula, sub )
    case ExistsRightRule( uproof, root, aux, prin, sub ) =>
      val duproof = expandProof( uproof )
      ExistsRightRule( duproof, aux.formula, prin.formula, sub )

    //equality and definitions
    case EquationLeft1Rule( uproof1, uproof2, root, aux1, aux2, _, prin ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      EquationLeftRule( duproof1, duproof2, aux1.formula, aux2.formula, prin.formula )
    case EquationLeft2Rule( uproof1, uproof2, root, aux1, aux2, _, prin ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      EquationLeft2Rule( duproof1, duproof2, aux1.formula, aux2.formula, prin.formula )
    case EquationRight1Rule( uproof1, uproof2, root, aux1, aux2, _, prin ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      EquationRightRule( duproof1, duproof2, aux1.formula, aux2.formula, prin.formula )
    case EquationRight2Rule( uproof1, uproof2, root, aux1, aux2, _, prin ) =>
      val duproof1 = expandProof( uproof1 )
      val duproof2 = expandProof( uproof2 )
      EquationRightRule( duproof1, duproof2, aux1.formula, aux2.formula, prin.formula )

    case DefinitionLeftRule( uproof, root, aux, prin ) =>
      val duproof = expandProof( uproof )
      DefinitionLeftRule( duproof, aux.formula, prin.formula )
    case DefinitionRightRule( uproof, root, aux, prin ) =>
      val duproof = expandProof( uproof )
      DefinitionRightRule( duproof, aux.formula, prin.formula )

  }

}
