package at.logic.gapt.provers.inductionProver

import at.logic.gapt.expr._
import at.logic.gapt.expr.fol.{ FOLSubstitution, Utils }
import at.logic.gapt.expr.hol.CNFp
import at.logic.gapt.grammars.findMinimalSipGrammar
import at.logic.gapt.proofs.expansionTrees._
import at.logic.gapt.proofs.lk.LKToExpansionProof
import at.logic.gapt.proofs.lk.base.{ LKProof, HOLSequent }
import at.logic.gapt.provers.Prover
import at.logic.gapt.provers.maxsat.QMaxSAT
import at.logic.gapt.provers.prover9.Prover9Prover
import at.logic.gapt.provers.veriT.VeriTProver
import at.logic.gapt.utils.logging.Logger

trait SolutionFinder {
  def findSolution( schematicSIP: SimpleInductionProof ): Option[FOLFormula]
}

class SipProver(
  solutionFinder:            SolutionFinder = new HeuristicSolutionFinder( 1 ),
  instanceProver:            Prover         = new Prover9Prover(),
  instances:                 Seq[Int]       = 0 until 3,
  testInstances:             Seq[Int]       = 0 until 15,
  minimizeInstanceLanguages: Boolean        = false,
  quasiTautProver:           Prover         = new VeriTProver()
)
    extends Prover with Logger {

  override def getLKProof( endSequent: HOLSequent ): Option[LKProof] =
    getSimpleInductionProof( endSequent ).map( _.toLKProof )

  def getSimpleInductionProof( endSequent: HOLSequent ): Option[SimpleInductionProof] = {
    val inductionVariable = freeVariables( endSequent.formulas.toList.map( _.asInstanceOf[FOLExpression] ) ) match {
      case singleton if singleton.size == 1 => singleton.head
    }
    require( inductionVariable == SimpleInductionProof.alpha ) // TODO: maybe relax this restriction

    getSimpleInductionProof( endSequent, generateInstanceProofs( endSequent, inductionVariable ) )

  }

  def getSimpleInductionProof( endSequent: HOLSequent, instanceProofs: Seq[( Int, ExpansionSequent )] ): Option[SimpleInductionProof] = {

    val inductionVariable = freeVariables( endSequent.formulas.toList.map( _.asInstanceOf[FOLExpression] ) ) match {
      case singleton if singleton.size == 1 => singleton.head
    }
    require( inductionVariable == SimpleInductionProof.alpha ) // TODO: maybe relax this restriction

    val termEncoding = InstanceTermEncoding( endSequent )
    var instanceLanguages = instanceProofs map {
      case ( n, expSeq ) =>
        n -> termEncoding.encode( expSeq )
    }

    // Ground the instance languages.
    instanceLanguages = instanceLanguages map {
      case ( n, lang ) => n -> groundTerms( lang )
    }

    instanceLanguages foreach {
      case ( n, l ) =>
        debug( s"Instance language for n=$n:\n${l.map( _.toString ).sorted.mkString( "\n" )}" )
    }

    debug( "Finding grammar..." )
    val grammar = findMinimalSipGrammar( instanceLanguages, new QMaxSAT )
    debug( s"Grammar:\n$grammar" )

    if ( testInstances.forall { n =>
      val generatedInstanceSequent = FOLSubstitution( inductionVariable -> Utils.numeral( n ) )(
        termEncoding.decodeToFSequent( grammar.instanceGrammar( n ).language )
      )
      val isQuasiTaut = quasiTautProver.isValid( generatedInstanceSequent )
      debug( s"[n=$n] Instance language is quasi-tautological: $isQuasiTaut" )
      isQuasiTaut
    } ) {
      val schematicSip = decodeSipGrammar( termEncoding, grammar )
      debug( s"Gamma0 = ${schematicSip.Gamma0}" )
      debug( s"Gamma1 = ${schematicSip.Gamma1}" )
      debug( s"Gamma2 = ${schematicSip.Gamma2}" )
      debug( s"Sequent0 = ${schematicSip.Sequent0}" )
      debug( s"Sequent1 = ${schematicSip.Sequent1}" )
      debug( s"Sequent2 = ${schematicSip.Sequent2}" )
      debug( s"t = ${schematicSip.t}" )
      debug( s"u = ${schematicSip.u}" )

      ( 0 until 3 ) foreach { i =>
        lazy val C_i = canonicalSolution( schematicSip, i )
        debug( s"C_$i =\n${CNFp( C_i ).map( _.mkString( ", " ) ).mkString( "\n" )}" )
      }

      solutionFinder.findSolution( schematicSip ).map( schematicSip.solve )
    } else {
      None // sip grammar does not generate a quasi-tautology for a concrete instance, hence unsolvable
    }
  }

  def generateInstanceProofs( endSequent: HOLSequent, inductionVariable: FOLVar ): Seq[( Int, ExpansionSequent )] = {
    var instanceProofs = instances map { n =>
      val instanceSequent = FOLSubstitution( inductionVariable -> Utils.numeral( n ) )( endSequent )
      debug( s"[n=$n] Proving $instanceSequent" )
      n -> instanceProver.getExpansionSequent( instanceSequent ).get
    }
    if ( minimizeInstanceLanguages ) {
      instanceProofs = instanceProofs map {
        case ( n, expProof ) =>
          val minExpProof = minimalExpansionSequent( expProof, new VeriTProver ).get
          debug {
            val removedInstances = extractInstances( expProof ) diff extractInstances( minExpProof )
            s"[n=$n] Removing unnecessary instances $removedInstances"
          }
          n -> minExpProof
      }
    }

    instanceProofs
  }
}
