package at.logic.gapt.formats.tptp

import org.specs2.mutable._
import org.specs2.execute.Success
import at.logic.gapt.expr._
import at.logic.gapt.expr._
import at.logic.gapt.proofs.lk.base.HOLSequent

class TPTPHOLExporterTest extends Specification {
  "Export to TPTP thf" should {
    "handle atoms correctly" in {
      val x = Var( "x", Ti -> To )
      val y = Var( "y", To )
      val c = Const( "c", Ti )

      val ax = HOLAtom( x, List( c ) )
      val ay = HOLAtom( y )

      println( TPTPHOLExporter( List( HOLSequent( Nil, List( ax, ay ) ) ) ) )

      println( TPTPHOLExporter( List(
        HOLSequent( List( ax ), Nil ),
        HOLSequent( Nil, List( ay ) )
      ) ) )
      ok
    }
  }

}
