/*
 * fol.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package at.logic.language.fol

import at.logic.language.lambda.typedLambdaCalculus._
import at.logic.language.hol.{Neg => HOLNeg, Or => HOLOr, And => HOLAnd, Imp => HOLImp}
import at.logic.language.hol.{HOLExpression, HOL, HOLFormula, HOLVar, HOLConst, HOLApp, HOLAbs, HOLConstFormula, HOLFactory, HOLAppFormula}
import at.logic.language.hol.{AllQ => HOLAllQ, ExQ => HOLExQ}
import at.logic.language.lambda.symbols._
import at.logic.language.lambda.types._
import at.logic.language.hol.logicSymbols._
import at.logic.language.lambda.types.ImplicitConverters._

object Definitions { def fol = FOLFactory }

trait FOL extends HOL
{
  override def factory : LambdaFactoryA = FOLFactory
}

trait FOLExpression extends HOLExpression with FOL {
    override def toString = this match {
      case Var(x,_) => x.toString
      case Atom(x, args) => x + "(" +
        (if (args.size > 1) args.head.toString + args.tail.foldLeft("")((s,a) => s+", "+a.toString)
        else args.foldLeft("")((s,a) => s+a.toString)) + ")"
      case Function(x, args) => x + "(" +
        (if (args.size > 1) args.head.toString + args.tail.foldLeft("")((s,a) => s+", "+a.toString)
        else args.foldLeft("")((s,a) => s+a.toString)) + ")"
      case And(x,y) => "(" + x.toString + AndSymbol + y.toString + ")"
      case Or(x,y) => "(" + x.toString + OrSymbol + y.toString + ")"
      case Imp(x,y) => "(" + x.toString + ImpSymbol + y.toString + ")"
      case Neg(x) => NegSymbol + x.toString
      case ExVar(x,f) => ExistsSymbol + x.toString + "." + f.toString
      case AllVar(x,f) => ForallSymbol + x.toString + "." + f.toString
    }

    // this function outputs the string which creates
    // an object like this. can be used to create
    // tests based on bugs.
    def toCode : String = this match {
      case FOLVar(x) => "FOLVar( " + x.toCode + " )"
      case FOLConst(x) => "FOLConst( " + x.toCode + " )"
      case Atom(x, args) => "Atom( " + x.toCode + ", " + args.foldLeft( "Nil" )( (s, a) => a.toCode + "::" + s ) + ")"
      case Function(x, args) => "Function( " + x.toCode + ", " + args.foldLeft( "Nil" )( (s, a) => a.toCode + "::" + s ) + ")"
      case And(x,y) => "And(" + x.toCode + ", " + y.toCode + ")"
      case Or(x,y) => "Or(" + x.toCode + ", " + y.toCode + ")"
      case Imp(x,y) => "Imp(" + x.toCode + ", " + y.toCode + ")"
      case Neg(x) => "Neg(" + x.toCode + ")"
      case ExVar(x,f) => "ExVar(" + x.toCode + ", " + f.toCode + ")"
      case AllVar(x,f) => "AllVar(" + x.toCode + ", " + f.toCode + ")"
    }
  }
trait FOLFormula extends FOLExpression with HOLFormula
//trait FOLFormula extends HOLFormula with FOL
trait FOLTerm extends FOLExpression
// trait FOLTerm extends HOLExpression with FOL
{
  require( exptype == Ti() )
}

// individual variable
class FOLVar (name: VariableSymbolA, dbInd: Option[Int])
  extends HOLVar(name, Ti(), dbInd) with FOLTerm

// individual constant
class FOLConst (name: ConstantSymbolA)
  extends HOLConst(name, Ti()) with FOLTerm

class FOLApp(function: LambdaExpression, argument: LambdaExpression)
  extends HOLApp(function, argument) with FOLExpression

class FOLAbs(variable: FOLVar, expression: LambdaExpression)
  extends HOLAbs(variable, expression) with FOLExpression

object FOLAbs {
  def apply(variable: FOLVar, expression: LambdaExpression) = new FOLAbs(variable, expression)
}

object FOLVar {
  def apply(name: VariableSymbolA) = new FOLVar(name,None)
  def unapply(exp: LambdaExpression) = exp match {
    case Var( sym : VariableSymbolA, t : Ti ) => Some( sym )
    case _ => None
  }
}

object FOLConst {
  def apply(name: ConstantSymbolA) = new FOLConst(name)
  def unapply(exp: LambdaExpression) = exp match {
    case Var( sym : ConstantSymbolA, t : Ti ) => Some( sym )
    case _ => None
  }
}

// FOL atom of the form P(t_1,...,t_n)
object Atom {
  def apply( sym: ConstantSymbolA, args: List[FOLTerm]) = {
    val pred : Var = FOLFactory.createVar( sym, FunctionType( To(), args.map( a => Ti() ) ) )
    AppN(pred, args).asInstanceOf[FOLFormula]
  }
  def unapply( expression: LambdaExpression ) = expression match {
    case App(sym,_) if sym.isInstanceOf[LogicalSymbolsA] => None
    case App(App(sym,_),_) if sym.isInstanceOf[LogicalSymbolsA] => None
    case AppN( Var( name: ConstantSymbolA, t ), args )
      if t == FunctionType( To(), args.map( a => Ti() ) ) => Some( ( name, args.asInstanceOf[List[FOLTerm]] ) )
    case _ => None
  }
}

// FOL function of the form f(t_1,...,t_n)
object Function {
  def apply( sym: ConstantSymbolA, args: List[FOLTerm]) = {
    val f: Var = FOLFactory.createVar( sym, FunctionType( Ti(), args.map( a => Ti() ) ) )
    AppN( f, args ).asInstanceOf[FOLTerm]
  }
  def unapply( expression: LambdaExpression ) = expression match {
    case AppN( Var( name: ConstantSymbolA, t), args )
      if t == FunctionType( Ti(), args.map( a => Ti() ) ) => Some( (name, args.asInstanceOf[List[FOLTerm]] ) )
    case _ => None
  }
}

case object BottomC extends HOLConst(BottomSymbol, "o") with FOL with FOLFormula
case object NegC extends HOLConst(NegSymbol, "(o -> o)") with FOL
case object AndC extends HOLConst(AndSymbol, "(o -> (o -> o))") with FOL
case object OrC extends HOLConst(OrSymbol, "(o -> (o -> o))") with FOL
case object ImpC extends HOLConst(ImpSymbol, "(o -> (o -> o))") with FOL
class ExQ(e:TA) extends HOLExQ(e) with FOL
class AllQ(e:TA) extends HOLAllQ(e) with FOL
object ExQ {
  def unapply(v: Var) = v match {
    case vo: ExQ => Some(vo.exptype)
    case _ => None
  }
}
object AllQ {
  def unapply(v: Var) = v match {
    case vo: AllQ => Some(vo.exptype)
    case _ => None
  }
}
  
object Neg {
  def apply(sub: FOLFormula) = App(NegC,sub).asInstanceOf[FOLFormula]
  def unapply(expression: LambdaExpression) = expression match {
    case App(NegC,sub) => Some( (sub.asInstanceOf[FOLFormula]) )
    case _ => None
  }
}

object And {
  def apply(left: FOLFormula, right: FOLFormula) = (App(App(AndC,left),right)).asInstanceOf[FOLFormula]
  def unapply(expression: LambdaExpression) = expression match {
    case App(App(AndC,left),right) => Some( (left.asInstanceOf[FOLFormula],right.asInstanceOf[FOLFormula]) )
    case _ => None
  }
}

object Or {
  def apply(left: FOLFormula, right: FOLFormula) = App(App(OrC,left),right).asInstanceOf[FOLFormula]
  def unapply(expression: LambdaExpression) = expression match {
    case App(App(OrC,left),right) => Some( (left.asInstanceOf[FOLFormula],right.asInstanceOf[FOLFormula]) )
    case _ => None
  }
}

object Imp {
  def apply(left: FOLFormula, right: FOLFormula) = App(App(ImpC,left),right).asInstanceOf[FOLFormula]
  def unapply(expression: LambdaExpression) = expression match {
      case App(App(ImpC,left),right) => Some( (left.asInstanceOf[FOLFormula],right.asInstanceOf[FOLFormula]) )
      case _ => None
  }
}

object Ex {
  def apply(sub: LambdaExpression) = App(new ExQ(sub.exptype),sub).asInstanceOf[FOLFormula]
  def unapply(expression: LambdaExpression) = expression match {
    case App(ExQ(t),sub) => Some( (sub, t) )
    case _ => None
  }
}

object All {
  def apply(sub: LambdaExpression) = App(new AllQ(sub.exptype),sub).asInstanceOf[FOLFormula]
  def unapply(expression: LambdaExpression) = expression match {
    case App(AllQ(t),sub) => Some( (sub, t) )
    case _ => None
  }
}

object ExVar {
  def apply(variable: FOLVar, sub: FOLFormula) = Ex(FOLAbs(variable, sub))
  def unapply(expression: LambdaExpression) = expression match {
    case Ex(Abs(variable: FOLVar, sub: FOLFormula), _) => Some( (variable, sub) )
    case _ => None
  }
}

object AllVar {
  def apply(variable: FOLVar, sub: FOLFormula) = All(FOLAbs(variable, sub))
  def unapply(expression: LambdaExpression) = expression match {
    case All(Abs(variable: FOLVar, sub: FOLFormula), _) => Some( (variable, sub) )
    case _ => None
  }
}

object BinaryLogicSymbol {
  def unapply(expression: LambdaExpression) = expression match {
    case And(l, r) => Some( (AndC, l, r) )
    case Or(l, r) => Some( (OrC, l, r) )
    case Imp(l, r) => Some( (ImpC, l, r) )
    case _ => None
  }
}

object FOLFactory extends LambdaFactoryA {
  def createVar( name: SymbolA, exptype: TA, dbInd: Option[Int] ) : Var = exptype match {
    case Ti() => name match {
      case a: ConstantSymbolA => FOLConst(a)
      case a: VariableSymbolA => new FOLVar(a,dbInd)
    }
    case To() => name match {
      case a: ConstantSymbolA => new HOLConstFormula(a) with FOLFormula
      case _ => throw new Exception("In FOL, of type 'o' only constants may be created.")
    }
    case ->(tr, ta) => {
      if (!(isFirstOrderType(exptype)))
        throw new Exception("In FOL, cannot create a symbol of type " + exptype)
      name match {
        case a: ConstantSymbolA => new HOLConst(a, exptype) with FOLExpression
        case _ => throw new Exception("In FOL, of type 'a -> b' only constants may be created.")
      }
    }
  }

  def createVar( name: SymbolA ) : Var = createVar( name, Ti() )

  def createApp( fun: LambdaExpression, arg: LambdaExpression ) : App =
    if (HOLFactory.isFormulaWhenApplied(fun.exptype)) new HOLAppFormula(fun, arg) with FOLFormula
    else if (isTermWhenApplied(fun.exptype)) new FOLApp(fun, arg) with FOLTerm
    else new FOLApp(fun, arg)

  def createAbs( variable: Var, exp: LambdaExpression ) : Abs =  new HOLAbs( variable, exp ) with FOLExpression

  private def isTermWhenApplied(typ: TA) = typ match {
    case ->(_,Ti()) => true
    case _ => false
  }

  private def isFirstOrderType( exptype: TA ) = isFunctionType( exptype ) || isPredicateType( exptype )

  private def isFunctionType( exptype: TA ) = checkType( exptype, Ti(), Ti() ) 

  private def isPredicateType( exptype: TA ) = checkType( exptype, To(), Ti() )

  private def checkType( toCheck: TA, retType : TA, argType: TA ) : Boolean =
    toCheck match {
      case t : Ti => t == retType
      case t : To => t == retType
      case ->(ta, tr) => ta == argType && checkType( tr, retType, argType )
  }
}

object getFreeVariablesFOL {
  def apply( f: FOLFormula ) = f.getFreeAndBoundVariables._1.asInstanceOf[Set[FOLVar]]
}