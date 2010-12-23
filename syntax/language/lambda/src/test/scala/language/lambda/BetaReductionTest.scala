/*
 * BetaReductionTest.scala
 *
 */

package at.logic.language.lambda

import org.specs._
import org.specs.runner._


import types._
import types.Definitions._
import symbols._
import symbols.ImplicitConverters._
import typedLambdaCalculus._
import substitutions._
import BetaReduction._

class BetaReductionTest extends SpecificationWithJUnit {
  import StrategyOuterInner._
  import StrategyLeftRight._
  level = Debug  // sets the printing of extra information (level can be: Debug, Info, Warning, Error)

  val v = Var("v", i, LambdaFactory); val x = Var("x", i, LambdaFactory); val y = Var("y", i, LambdaFactory);
  val f = Var("f", i -> i, LambdaFactory); val g = Var("g", i -> i, LambdaFactory)
  val f2 = Var("f2", i -> i, LambdaFactory); val g2 = Var("g2", i -> i, LambdaFactory)

  "BetaReduction" should {
    "betaReduce a simple redex" in {
        val e = App(Abs(x, App(f, x)),v)
        ( betaReduce(e)(Outermost, Leftmost) ) must beEqual ( App(f, v) )
    }
    "betaReduce correctly with outermost strategy" in {
        val e = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
        ( betaReduce(e)(Outermost, Leftmost) ) must beEqual ( App(Abs(x, App(f, x)),y) )
    }
    "betaReduce correctly with innermost strategy" in {
        val e = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
        ( betaReduce(e)(Innermost, Leftmost) ) must beEqual ( App(Abs(v, App(f, v)),y) )
    }
    "betaReduce correctly with leftmost strategy" in {
        val er = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
        val el = Abs(v, App(Abs(x, App(f, x)),v))
        val e = App(el,er)
        ( betaReduce(e)(Innermost, Leftmost) ) must beEqual ( App(Abs(v, App(f, v)),er) )
    }
    "betaReduce correctly with rightmost strategy" in {
        val er = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
        val el = Abs(v, App(Abs(x, App(f, x)),v))
        val e = App(el,er)
        ( betaReduce(e)(Innermost, Rightmost) ) must beEqual ( App(el,App(Abs(v, App(f, v)),y)) )
    }
    "betaNormalize correctly with outermost strategy" in {
      "- 1" in {
          val er = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
          val el = Abs(v, App(Abs(x, App(f, x)),v))
          val e = App(el,er)
          ( betaNormalize(e)(Outermost) ) must beEqual ( App(f,App(f,y)) )
      }
      "- 2" in {
          val e = App(App(Abs(g, Abs(y, App(g,y))), f), v)
          ( betaNormalize(e)(Outermost) ) must beEqual ( App(f,v) )
      }
      "- 3" in {
          val e = App(App(App(Abs(g2, Abs(g, Abs(y, App(g2,App(g,y))))), f2), f), v)
          ( betaNormalize(e)(Outermost) ) must beEqual ( App(f2,App(f,v)) )
      }
    }
    "betaNormalize correctly with innermost strategy" in {
        val er = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
        val el = Abs(v, App(Abs(x, App(f, x)),v))
        val e = App(el,er)
        ( betaNormalize(e)(Innermost) ) must beEqual ( App(f,App(f,y)) )
    }
    "betaNormalize correctly with implicit standard strategy" in {
        import ImplicitStandardStrategy._
        val er = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
        val el = Abs(v, App(Abs(x, App(f, x)),v))
        val e = App(el,er)
        ( betaNormalize(e) ) must beEqual ( App(f,App(f,y)) )
    }
    "betaReduce correctly with implicit standard strategy" in {
        import ImplicitStandardStrategy._
        val e = App(Abs(v, App(Abs(x, App(f, x)),v)),y)
        ( betaReduce(e) ) must beEqual ( App(Abs(x, App(f, x)),y) )
    }
    "betaReduce correctly with regard to de Bruijn indices" in {
      "- 1" in {
        val term1 = App(Abs(LambdaVar("x",i->i),Abs(LambdaVar("y",i),App(LambdaVar("x",i->i),LambdaVar("y",i)))),Abs(LambdaVar("z",i),LambdaVar("z",i)))
        val term2 = Abs(LambdaVar("y",i),App(Abs(LambdaVar("z",i),LambdaVar("z",i)),LambdaVar("y",i)))
        (betaReduce(term1)(Outermost, Leftmost)) must beEqual (term2)
      }
      "- 2" in {
        val term1 = App(Abs(LambdaVar("x",i->i),Abs(LambdaVar("x",i),App(LambdaVar("x",i->i),LambdaVar("x",i)))),Abs(LambdaVar("x",i),LambdaVar("x",i)))
        val term2 = Abs(LambdaVar("y",i),App(Abs(LambdaVar("z",i),LambdaVar("z",i)),LambdaVar("y",i)))
        (betaReduce(term1)(Outermost, Leftmost)) must beEqual (term2)
      }
      "- 3" in {
        val x1 = LambdaVar("x",i->i)
        val x2 = LambdaVar("y",i)
        val x3 = LambdaVar("z",i)
        val x4 = LambdaVar("w",i)
        val x5 = LambdaVar("v",i)
        val c1 = LambdaVar("f", i->(i->i))
        val t1 = App(c1,App(x1,x2))
        val t2 = App(t1,App(x1,x3))
        val t3 = Abs(x4,x4)
        val term1 = App(AbsN(x1::x2::x3::Nil, t2),t3)
        val term2 = AbsN(x2::x3::Nil, App(App(c1,App(t3,x2)),App(t3,x3)))
        (betaReduce(term1)(Outermost, Leftmost)) must beEqual (term2)
      }
      "- 4" in {
        val e = Abs(x, App(Abs(g, App(g,x)), f))
        (betaReduce(e)(Outermost, Leftmost)) must beEqual (Abs(y, App(f, y)))
      }
    }
  }
}