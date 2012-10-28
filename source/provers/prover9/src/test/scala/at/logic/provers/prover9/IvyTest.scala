package at.logic.provers.prover9.ivy

import conversion.IvyToRobinson
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.SpecificationWithJUnit
import collection.immutable
import at.logic.provers.prover9.lisp
import java.io.File.separator
import util.parsing.input.Reader
import lisp.{SExpressionParser}

/**
 * Test for the Ivy interface.
 */
@RunWith(classOf[JUnitRunner])
class IvyTest extends SpecificationWithJUnit {
  def dumpreader[T](r:Reader[T]) = {
    var reader = r
    println("=== dumping reader! ===")
    while (! reader.atEnd) {
      print(reader.first)
      reader = reader.rest
    }
    println()
  }

  def debug(s:String) = {println("Debug: "+s)}

  "The Ivy Parser " should {
       " parse an empty list " in {
         val parser = new SExpressionParser
         parser.parse("()") match {
           case parser.Success(result, rest) =>
//             debug("parsing ok!")
             true must beEqualTo(true)
           case parser.NoSuccess(msg, rest) =>
//             debug("parser failed: "+msg)
             true must beEqualTo(false)
         }
       }

    " parse the atom a1" in {
      val parser = new SExpressionParser
      parser.parse("a1") match {
        case parser.Success(result, rest) =>
//          debug("parsing ok!")
          true must beEqualTo(true)
        case parser.NoSuccess(msg, rest) =>
//          debug("parser failed: "+msg)
          true must beEqualTo(false)
      }
    }

    " parse the atom a2(space)" in {
      //this test passes because regexpparsers strip whitespace!
      val parser = new SExpressionParser
      parser.parse("a2    ") match {
        case parser.Success(result, rest) =>
          //          debug("parsing ok!)
          true must beEqualTo(true)
        case parser.NoSuccess(msg, rest) =>
          //          debug("parsing wrong!")
          true must beEqualTo(false)
      }
    }

    """ parse the atom "a b c" """ in {
      //this test passes because regexpparsers strip whitespace!
      val parser = new SExpressionParser
      parser.parse(""""a b c"""") match {
        case parser.Success(result, rest) =>
          //          debug("parsing ok!)
          true must beEqualTo(true)
        case parser.NoSuccess(msg, rest) =>
          //          debug("parsing wrong!")
          true must beEqualTo(false)
      }
    }


    " parse the list (c1 (c2 c2) c) " in {
        val parser = new SExpressionParser
      parser.parse("(c1 (c2 c2) c)") match {
        case parser.Success(result, rest) =>
          result match {
            case lisp.List(lisp.Atom("c1")::
                           lisp.List(lisp.Atom("c2"):: lisp.Atom("c2"):: Nil)::
                           lisp.Atom("c")::Nil)::Nil =>
              "matched correct list" must beEqualTo("matched correct list")
            case _ =>
              debug(result)
              dumpreader(rest)
              "matched correct list" must beEqualTo("failed")
          }
        case parser.NoSuccess(msg, rest) =>
          "parser returned success" must beEqualTo("failed")
      }
    }


    " parse the list c4;;comment" in {
      val parser = new SExpressionParser
      parser.parse("c4;;comment") match {
        case parser.Success(result, rest) =>
          result match {
            case lisp.Atom("c4")::Nil =>
              "matched correct list" must beEqualTo("matched correct list")
            case _ =>
              debug(result)
              "matched correct list" must beEqualTo("failed")
          }
        case parser.NoSuccess(msg, rest) =>
          "parser returned success" must beEqualTo("failed")
      }
    }

    " parse the comments ;;comment 1\n;;comment 2" in {
      val parser = new SExpressionParser
      parser.parse(";;comment 1\n;;comment 2\n") match {
        case parser.Success(result, rest) =>
          result match {
            case Nil =>
              "matched correct list" must beEqualTo("matched correct list")
            case _ =>
              debug("correct result:")
              debug(result)
              "matched correct list" must beEqualTo("failed")
          }
        case parser.NoSuccess(msg, rest) =>
          debug(msg)
          dumpreader(rest)
          "parser returned success" must beEqualTo("failed")
      }
    }

    " parse the list ;;comment\nc5" in {
      debug(" parse the list ;;comment\nc5")
      val parser = new SExpressionParser
      parser.parse(";;comment\nc5") match {
        case parser.Success(result, rest) =>
          result match {
            case lisp.Atom("c5")::Nil =>
              "matched correct list" must beEqualTo("matched correct list")
            case _ =>
              debug(result)
              "matched correct list" must beEqualTo("failed")
          }
        case parser.NoSuccess(msg, rest) =>
          debug(msg)
          dumpreader(rest)
          "parser returned success" must beEqualTo("failed")
      }
    }

    " parse the list (c1 (c2 c2) c) ;;comment" in {
      val parser = new SExpressionParser
      parser.parse("(c1 (c2 c2) c);;comment") match {
        case parser.Success(result, rest) =>
          result match {
            case lisp.List(lisp.Atom("c1")::
              lisp.List(lisp.Atom("c2"):: lisp.Atom("c2"):: Nil)::
              lisp.Atom("c")::Nil)::Nil =>
              "matched correct list" must beEqualTo("matched correct list")
            case _ =>
              debug(result)
              "matched correct list" must beEqualTo("failed")
          }
        case parser.NoSuccess(msg, rest) =>
          "parser returned success" must beEqualTo("failed")
      }
    }

    " parse the list (c1 (c2 c2)  ;;comment\nc)" in {
      val parser = new SExpressionParser
      parser.parse("(c1 (c2 c2) c);;comment") match {
        case parser.Success(result, rest) =>
          result match {
            case lisp.List(lisp.Atom("c1")::
              lisp.List(lisp.Atom("c2"):: lisp.Atom("c2"):: Nil)::
              lisp.Atom("c")::Nil)::Nil =>
              "matched correct list" must beEqualTo("matched correct list")
            case _ =>
              debug(result)
              "matched correct list" must beEqualTo("failed")
          }
        case parser.NoSuccess(msg, rest) =>
          "parser returned success" must beEqualTo("failed")
      }
    }


    " parse the list (c1 \"c2 c2\" c) " in {
      val parser = new SExpressionParser
      parser.parse("(c1 \"c2 c2\" c)") match {
        case parser.Success(result, rest) =>
          //          debug("parsing ok!")
          true must beEqualTo(true)
        case parser.NoSuccess(msg, rest) =>
          //          debug("parser failed: "+msg)
          true must beEqualTo(false)
      }
    }


    " parse the list_ a1 b " in {
      val parser = new SExpressionParser
      parser.parse("a1 b") match {
        case parser.Success(result, rest) =>
//          debug("parsing ok!")
          true must beEqualTo(true)
        case parser.NoSuccess(msg, rest) =>
//          debug("parser failed: "+msg)
          true must beEqualTo(false)
      }
    }

    " parse the list ;;comment 1\n(c1 (c2 c2)  ;;comment 2\nc)" in {
      val parser = new SExpressionParser
      parser.parse("(\n;;comment 1\nc1 (c2 c2) c);;comment 2") match {
        case parser.Success(result, rest) =>
          result match {
            case lisp.List(lisp.Atom("c1")::
              lisp.List(lisp.Atom("c2"):: lisp.Atom("c2"):: Nil)::
              lisp.Atom("c")::Nil) :: Nil =>
              "matched correct list" must beEqualTo("matched correct list")
            case _ =>
              debug(result)
              dumpreader(rest)
              "matched correct list" must beEqualTo("failed")
          }
        case parser.NoSuccess(msg, rest) =>
          debug(msg)
          dumpreader(rest)
          "parser returned success" must beEqualTo("failed")
      }
    }


    " parse the test file simple.ivy " in {
      try {
        val result = SExpressionParser("target" + separator + "test-classes" + separator +"simple.ivy")
        result must not beEmpty
        val proof = result.head
        proof match {
          case lisp.List( input1 :: input2 :: instantiate8 :: paramod3 :: input4 :: input5 :: instantiate9 :: resolve6 :: resolve7 :: Nil) =>
            val pinput1 = IvyParser.parse(lisp.List(input1 :: Nil))
            debug(pinput1)
            val pinput2 = IvyParser.parse(lisp.List(input2 :: Nil))
            debug(pinput2)
            val pinput3 = IvyParser.parse(lisp.List(input1 :: instantiate8 :: Nil))
            debug(pinput3)

          case _ =>
//            "The first two rules of simple.ivy must parse correctly" must beEqualTo("failed")
            "The proof in simple.ivy must have 9 inferences" must beEqualTo("failed")
        }

        true must beEqualTo(true)
      } catch {
        case e:Exception =>
        debug("Exception parsing simple.ivy: "+e.getMessage)
        "" must beEqualTo("Exception occurred during proof parsing! "+e.getMessage + " || "+
          e.getStackTrace().toList.foldLeft("")((str:String, el : StackTraceElement) => str + "\n" + el.toString)  )

      }
    }

    " parse the test file instantiations.ivy " in {
      try {
        val result = SExpressionParser("target" + separator + "test-classes" + separator +"instantiations.ivy")
        result must not beEmpty
        val proof = result.head
        proof match {
          case lisp.List( input1 :: input2 :: instantiate8 :: paramod3 :: input4 :: input5 :: instantiate9 :: resolve6 :: resolve7 :: instantiate10 :: Nil) =>
            val pinput3 = IvyParser.parse(lisp.List(paramod3 :: instantiate9 :: Nil))
            debug(pinput3)
            val pinput4 = IvyParser.parse(lisp.List(instantiate10 :: Nil))
            debug(pinput4)

          case _ =>
            //            "The first two rules of simple.ivy must parse correctly" must beEqualTo("failed")
            "The proof in simple.ivy must have 9 inferences" must beEqualTo("failed")
        }

        true must beEqualTo(true)
      } catch {
        case e:Exception =>
          debug("Exception parsing simple.ivy: "+e.getMessage)
          "" must beEqualTo("Exception occurred during proof parsing! "+e.getMessage + " || "+
            e.getStackTrace().toList.foldLeft("")((str:String, el : StackTraceElement) => str + "\n" + el.toString)  )

      }
    }

    " parse the test file resulution.ivy " in {
      try {
        val result = SExpressionParser("target" + separator + "test-classes" + separator +"resolution.ivy")
        result must not beEmpty
        val proof = result.head
        proof match {
          case lisp.List( input1 :: input2 :: instantiate8 :: paramod3 :: input4 :: input5 :: instantiate9 :: resolve6 :: resolve7 :: Nil) =>
            val pinput = IvyParser.parse(proof)
            debug("resolution: "+pinput)

          case _ =>
            //            "The first two rules of simple.ivy must parse correctly" must beEqualTo("failed")
            "The proof in resolution.ivy must have 9 inferences" must beEqualTo("failed")
        }

        true must beEqualTo(true)
      } catch {
        case e:Exception =>
          debug("Exception parsing simple.ivy: "+e.getMessage)
          "" must beEqualTo("Exception occurred during proof parsing! "+e.getMessage + " || "+
            e.getStackTrace().toList.foldLeft("")((str:String, el : StackTraceElement) => str + "\n" + el.toString)  )

      }
    }


    " parse the test files factor.ivy and factor2.ivy " in {
      try {
        val result = SExpressionParser("target" + separator + "test-classes" + separator +"factor.ivy")
        result must not beEmpty
        val proof = result.head
        proof match {
          case lisp.List(_) =>
            val pinput = IvyParser.parse(proof)
            debug("resolution: "+pinput)
            val rinput = IvyToRobinson(pinput)
            debug("robinson: "+rinput)

          case _ =>
            "The proof in factor.ivy must have some inferences" must beEqualTo("failed")
        }

        val result2 = SExpressionParser("target" + separator + "test-classes" + separator +"factor2.ivy")
        result2 must not beEmpty
        val proof2 = result2.head
        proof2 match {
          case lisp.List(_) =>
            val pinput = IvyParser.parse(proof2)
            debug("resolution: "+pinput)
            val rinput = IvyToRobinson(pinput)
            debug("robinson: "+rinput)

          case _ =>
            "The proof in factor.ivy must have some inferences" must beEqualTo("failed")
        }
        true must beEqualTo(true)
      } catch {
        case e:Exception =>
          debug("Exception parsing factor(2).ivy: "+e.getMessage)
          "" must beEqualTo("Exception occurred during proof parsing! "+e.getMessage + "\n"+
            e.getStackTrace().toList.foldLeft("")((str:String, el : StackTraceElement) => str + "\n" + el.toString)  )

      }
    }


    " parse the test file manyliterals.ivy " in {
      try {
        val result = SExpressionParser("target" + separator + "test-classes" + separator +"manyliterals.ivy")
        result must not beEmpty
        val proof = result.head
        proof match {
          case lisp.List(_) =>
            val pinput = IvyParser.parse(proof)
            debug("resolution: "+pinput)
            val rinput = IvyToRobinson(pinput)
            debug("robinson: "+rinput)

          case _ =>
            //            "The first two rules of simple.ivy must parse correctly" must beEqualTo("failed")
            "The proof in manyliterals.ivy must have some inferences" must beEqualTo("failed")
        }

        true must beEqualTo(true)
      } catch {
        case e:Exception =>
          debug("Exception parsing simple.ivy: "+e.getMessage)
          "" must beEqualTo("Exception occurred during proof parsing! "+e.getMessage + " || "+
            e.getStackTrace().toList.foldLeft("")((str:String, el : StackTraceElement) => str + "\n" + el.toString)  )

      }
    }

    " parse the test file simple2.ivy " in {
      try {
        val result = SExpressionParser("target" + separator + "test-classes" + separator +"simple2.ivy")
        true must beEqualTo(true)
      } catch {
        case e:Exception =>
          debug("Exception parsing simple.ivy: "+e.getMessage)
          true must beEqualTo(false)

      }
    }
  }

  " parse the test file prime1-0sk.ivy (clause set of the ) " in {
    try {
      val result = SExpressionParser("target" + separator + "test-classes" + separator +"prime1-0sk.ivy")
      true must beEqualTo(true)
    } catch {
      case e:Exception =>
        debug("Exception parsing simple.ivy: "+e.getMessage)
        true must beEqualTo(false)

    }
  }

  " parse the test file GRA014+1.ivy (clause set of the ) " in {
    try {
      val result = SExpressionParser("target" + separator + "test-classes" + separator +"GRA014+1.ivy")
      true must beEqualTo(true)
    } catch {
      case e:Exception =>
        debug("Exception parsing GRA014+1.ivy: "+e.getMessage)
        true must beEqualTo(false)

    }
  }

}
