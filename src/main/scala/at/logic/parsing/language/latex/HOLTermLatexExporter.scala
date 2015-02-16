/*
 * HOLExpressionLatexExporter.scala
 *
 */

package at.logic.parsing.language.latex

import at.logic.language.hol._
import at.logic.parsing.OutputExporter
import at.logic.language.lambda.types._
import at.logic.language.schema.indexedOmegaVar
import at.logic.parsing.language.HOLTermExporter

trait HOLTermLatexExporter extends OutputExporter with at.logic.parsing.language.HOLTermExporter {
  // it is LambdaExpression and require because of the stupid design chose not to have a common element for HOL
  def exportTerm( t: HOLExpression ): Unit = {
    require( t.isInstanceOf[HOLExpression] );
    t match {
      case indv: indexedOmegaVar        => getOutput.write( indv.name.toString + """_{""" + indv.index + """}""" )
      case HOLVar( name, _ )            => getOutput.write( name.toString )
      case HOLConst( name, _ )          => getOutput.write( name.toString )
      case HOLNeg( f )                  => { getOutput.write( """\neg """ ); exportTerm_( f ); }
      case HOLAnd( f1, f2 )             => { exportTerm_( f1 ); getOutput.write( """ \wedge """ ); exportTerm_( f2 ); }
      case HOLOr( f1, f2 )              => { exportTerm_( f1 ); getOutput.write( """ \vee """ ); exportTerm_( f2 ); }
      case HOLImp( f1, f2 )             => { exportTerm_( f1 ); getOutput.write( """ \rightarrow """ ); exportTerm_( f2 ); }
      case HOLExVar( v, f )             => { getOutput.write( """\exists """ ); exportTerm_( v.asInstanceOf[HOLVar] ); getOutput.write( """.""" ); exportTerm_( f ); }
      case HOLAllVar( v, f )            => { getOutput.write( """\forall """ ); exportTerm_( v.asInstanceOf[HOLVar] ); getOutput.write( """.""" ); exportTerm_( f ); }
      case HOLAbs( v, t )               => { getOutput.write( """\lambda """ ); exportTerm_( v ); getOutput.write( """.""" ); exportTerm_( t ); }
      case HOLAtom( name, args )        => exportFunction( t )
      case HOLFunction( name, args, _ ) => exportFunction( t )
    }
  }

  private def exportTerm_( t: HOLExpression ): Unit = {
    require( t.isInstanceOf[HOLExpression] ); t match {
      case indv: indexedOmegaVar        => getOutput.write( indv.name.toString + """_{""" + indv.index + """}""" )
      case HOLVar( name, _ )            => getOutput.write( name.toString )
      case HOLConst( name, _ )          => getOutput.write( name.toString )
      case HOLNeg( f )                  => { getOutput.write( "(" ); getOutput.write( """\neg """ ); exportTerm_( f ); getOutput.write( ")" ) }
      case HOLAnd( f1, f2 )             => { getOutput.write( "(" ); exportTerm_( f1 ); getOutput.write( """ \wedge """ ); exportTerm_( f2 ); getOutput.write( ")" ) }
      case HOLOr( f1, f2 )              => { getOutput.write( "(" ); exportTerm_( f1 ); getOutput.write( """ \vee """ ); exportTerm_( f2 ); getOutput.write( ")" ) }
      case HOLImp( f1, f2 )             => { getOutput.write( "(" ); exportTerm_( f1 ); getOutput.write( """ \rightarrow """ ); exportTerm_( f2 ); getOutput.write( ")" ) }
      case HOLExVar( v, f )             => { getOutput.write( "(" ); getOutput.write( """\exists """ ); exportTerm_( v.asInstanceOf[HOLVar] ); getOutput.write( """.""" ); exportTerm_( f ); getOutput.write( ")" ) }
      case HOLAllVar( v, f )            => { getOutput.write( "(" ); getOutput.write( """\forall """ ); exportTerm_( v.asInstanceOf[HOLVar] ); getOutput.write( """.""" ); exportTerm_( f ); getOutput.write( ")" ) }
      case HOLAbs( v, t )               => { getOutput.write( "(" ); getOutput.write( """\lambda """ ); exportTerm_( v ); getOutput.write( """.""" ); exportTerm_( t ); getOutput.write( ")" ) }
      case HOLAtom( name, args )        => exportFunction( t )
      case HOLFunction( name, args, _ ) => exportFunction( t )
    }
  }

  protected def latexType( ta: TA ): String = ta match {
    case Ti         => "i"
    case To         => "o"
    case ->( a, b ) => "(" + latexType( a ) + """ \rightarrow """ + latexType( b ) + ")"
  }
}

