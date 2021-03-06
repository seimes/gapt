package at.logic.gapt.testing

import java.io.File

import at.logic.gapt.cli.GAPScalaInteractiveShellLibrary.loadProver9LKProof
import at.logic.gapt.expr.fol.isFOLPrenexSigma1
import at.logic.gapt.formats.leanCoP.LeanCoPParser
import at.logic.gapt.formats.veriT.VeriTParser
import at.logic.gapt.proofs.expansionTrees.{ addSymmetry, toDeep, ExpansionProofToLK }
import at.logic.gapt.proofs.lk.{ solve, containsEqualityReasoning, ReductiveCutElim, LKToExpansionProof }
import at.logic.gapt.proofs.lk.cutIntroduction._
import at.logic.gapt.provers.minisat.MiniSATProver
import at.logic.gapt.provers.veriT.VeriTProver
import scala.concurrent.duration._
import scala.util.Random

import scala.xml.XML

class Prover9TestCase( f: File ) extends RegressionTestCase( f.getParentFile.getName ) {
  override def timeout = Some( 10 minutes )

  override def test( implicit testRun: TestRun ) = {
    val p = loadProver9LKProof( f.getAbsolutePath ) --- "import"

    val E = LKToExpansionProof( p ) --- "extractExpansionSequent"
    val deep = toDeep( E ) --- "toDeep"

    if ( !containsEqualityReasoning( p ) ) {
      new MiniSATProver().isValid( deep ) !-- "minisat validity"
      solve.solvePropositional( deep ).isDefined !-- "solvePropositional"
      ExpansionProofToLK( E ) --- "expansionProofToLKProof"
      ReductiveCutElim( p ) --? "cut-elim (input)"
    }

    new VeriTProver().isValid( deep ) !-- "verit validity"

    if ( isFOLPrenexSigma1( p.root.toHOLSequent ) ) {
      val qOption = CutIntroduction.one_cut_many_quantifiers( p, false ) --- "cut-introduction"

      qOption foreach { q =>
        if ( !containsEqualityReasoning( q ) )
          ReductiveCutElim( q ) --? "cut-elim (cut-intro)"
      }
    }
  }
}

class LeanCoPTestCase( f: File ) extends RegressionTestCase( f.getParentFile.getName ) {
  override def timeout = Some( 2 minutes )

  override def test( implicit testRun: TestRun ) = {
    val E = LeanCoPParser.getExpansionProof( f.getAbsolutePath ).get --- "import"

    val deep = toDeep( E ) --- "toDeep"
    new MiniSATProver().isValid( deep.toFormula ) !-- "minisat validity"
  }
}

class VeriTTestCase( f: File ) extends RegressionTestCase( f.getName ) {
  override def test( implicit testRun: TestRun ) = {
    val E = addSymmetry( VeriTParser.getExpansionProof( f.getAbsolutePath ).get ) --- "import"

    val deep = toDeep( E ) --- "toDeep"
    new MiniSATProver().isValid( deep.toFormula ) !-- "minisat validity"
  }
}

// Usage: RegressionTests [<test number limit>]
object RegressionTests extends App {
  def prover9Proofs = recursiveListFiles( "testing/TSTP/prover9" )
    .filter( _.getName.endsWith( ".out" ) )

  def leancopProofs = recursiveListFiles( "testing/TSTP/leanCoP" )
    .filter( _.getName.endsWith( ".out" ) )

  def veritProofs = recursiveListFiles( "testing/veriT-SMT-LIB" )
    .filter( _.getName.endsWith( ".proof_flat" ) )

  def prover9TestCases = prover9Proofs.map( new Prover9TestCase( _ ) )
  def leancopTestCases = leancopProofs.map( new LeanCoPTestCase( _ ) )
  def veritTestCases = veritProofs.map( new VeriTTestCase( _ ) )

  def allTestCases = prover9TestCases ++ leancopTestCases ++ veritTestCases

  def findTestCase( pat: String ) = allTestCases.find( _.toString.contains( pat ) ).get

  val testCases = args match {
    case Array( limit ) =>
      println( s"Only running $limit random tests." )
      Random.shuffle( allTestCases ).take( limit toInt )
    case _ => allTestCases
  }

  val total = testCases.length
  var started = 0
  val results = testCases.par map { tc =>
    started += 1
    println( s"[${( 100 * started ) / total}%] $tc" )
    try runOutOfProcess( Seq( "-Xmx1G", "-Xss30m" ) ) {
      tc.run().toJUnitXml
    } catch {
      case t: Throwable =>
        println( s"$tc failed:" )
        t.printStackTrace()
        <testsuite/>
    }
  }

  XML.save(
    "target/regression-test-results.xml",
    <testsuite> { results flatMap ( _.child ) toList } </testsuite>,
    "UTF-8"
  )
}
