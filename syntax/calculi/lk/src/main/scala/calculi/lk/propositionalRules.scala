/*
 * LK.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.calculi.lk

import at.logic.calculi.occurrences._
import at.logic.calculi.proofs._
import at.logic.language.hol._
import at.logic.language.lambda.typedLambdaCalculus._
import at.logic.utils.ds.trees._
import scala.collection.immutable.Set
import scala.collection.mutable.HashMap
import base._
import lkExtractors._

package propositionalRules {

  // axioms
  case object InitialRuleType extends NullaryRuleTypeA

  // structural rules
  case object WeakeningLeftRuleType extends UnaryRuleTypeA
  case object WeakeningRightRuleType extends UnaryRuleTypeA
  case object ContractionLeftRuleType extends UnaryRuleTypeA
  case object ContractionRightRuleType extends UnaryRuleTypeA
  case object CutRuleType extends BinaryRuleTypeA

  // Propositional rules
  case object AndRightRuleType extends BinaryRuleTypeA
  case object AndLeft1RuleType extends UnaryRuleTypeA
  case object AndLeft2RuleType extends UnaryRuleTypeA
  case object OrRight1RuleType extends UnaryRuleTypeA
  case object OrRight2RuleType extends UnaryRuleTypeA
  case object OrLeftRuleType extends BinaryRuleTypeA
  case object ImpRightRuleType extends UnaryRuleTypeA
  case object ImpLeftRuleType extends BinaryRuleTypeA
  case object NegLeftRuleType extends UnaryRuleTypeA
  case object NegRightRuleType extends UnaryRuleTypeA


  // lk proofs
  // rules are extracted in the form (UpperSequent(s), LowerSequent, AuxialiaryFormula(s), PrincipalFormula(s))

  // actual rule extractor/factories
  // Axioms (and weakenings) always return a pair(Proof, mapping) which maps the indices of the list given into the new occurrences.
  // It is used together with an implicit conversion between this pair into a proof so users who are not interested in this information will not see it

  object Axiom {
    def apply[T](seq: Sequent)(implicit factory: FOFactory) = {
      val left: Set[FormulaOccurrence] = seq.antecedent.foldLeft(Set.empty[FormulaOccurrence])((st, form) => st + createOccurrence(form, st, factory))
      val right: Set[FormulaOccurrence] = seq.succedent.foldLeft(Set.empty[FormulaOccurrence])((st, form) => st + createOccurrence(form, st, factory))
      new LeafTree[SequentOccurrence](SequentOccurrence(left, right)) with NullaryLKProof {def rule = InitialRuleType}
    }
    // this method is using a simple default factory (where occurrences have pointers equality) and return a map between positions in Sequent to formula occurrences
    // we pass empty sets of formula occurrences as this information is ignored in the factory
    def createDefault(seq: Sequent): Pair[LKProof, Pair[List[FormulaOccurrence],List[FormulaOccurrence]]] = {
      val left: List[FormulaOccurrence] = seq.antecedent.map(f => createOccurrence(f, Set[FormulaOccurrence](), PointerFOFactoryInstance))
      val right: List[FormulaOccurrence] = seq.succedent.map(f => createOccurrence(f, Set[FormulaOccurrence](), PointerFOFactoryInstance))
      (new LeafTree[SequentOccurrence](SequentOccurrence(toSet(left), toSet(right))) with NullaryLKProof {def rule = InitialRuleType}, (left,right))
    }
    def createOccurrence(f: HOLFormula, others: Set[FormulaOccurrence], factory: FOFactory): FormulaOccurrence = factory.createPrincipalFormulaOccurrence(f, Nil, others)
    def unapply(proof: LKProof) = if (proof.rule == InitialRuleType) Some((proof.root)) else None
    // should be optimized as it was done now just to save coding time
    def toSet[T](list: List[T]) = {
      def traverse(list: List[T])(set: Set[T]): Set[T] = list match {
        case hd :: tail => traverse(tail)(set + hd)   // create a new Set, adding hd
        case Nil => set
      }
      traverse(list)(Set[T]())
    }
//    def copyWithNewFactory(factory: FOFactory) = Axiom()
  }

  object WeakeningLeftRule {
    def apply(s1: LKProof, f: HOLFormula)(implicit factory: FOFactory) = {
      val prinFormula = factory.createPrincipalFormulaOccurrence(f, Nil, s1.root.antecedent)
      new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent) + prinFormula, createContext(s1.root.succedent)), s1)
        with UnaryLKProof with PrincipalFormulas {
          def rule = WeakeningLeftRuleType
          def prin = prinFormula::Nil
        }
    }
    def createDefault(s1: LKProof, f: HOLFormula) = {
      val prinFormula = PointerFOFactoryInstance.createPrincipalFormulaOccurrence(f, Nil)
      new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent) + prinFormula, createContext(s1.root.succedent)), s1)
        with UnaryLKProof with PrincipalFormulas {
          def rule = WeakeningLeftRuleType
          def prin = prinFormula::Nil
        }
    }
    def unapply(proof: LKProof) = if (proof.rule == WeakeningLeftRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with PrincipalFormulas]
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, p1))
      }
      else None
  }

  object WeakeningRightRule {
    def apply(s1: LKProof, f: HOLFormula)(implicit factory: FOFactory) = {
      val prinFormula = factory.createPrincipalFormulaOccurrence(f, Nil, s1.root.succedent)
      new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent), createContext(s1.root.succedent) + prinFormula), s1)
        with UnaryLKProof with PrincipalFormulas {
          def rule = WeakeningRightRuleType
          def prin = prinFormula::Nil
        }
    }
    def createDefault(s1: LKProof, f: HOLFormula) = {
      val prinFormula = PointerFOFactoryInstance.createPrincipalFormulaOccurrence(f, Nil)
      new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent), createContext(s1.root.succedent) + prinFormula), s1)
        with UnaryLKProof with PrincipalFormulas {
          def rule = WeakeningRightRuleType
          def prin = prinFormula::Nil
        }
    }
    def unapply(proof: LKProof) = if (proof.rule == WeakeningRightRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with PrincipalFormulas]
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, p1))
      }
      else None
  }

  object ContractionLeftRule {
    def apply(s1: LKProof, term1oc: Occurrence, term2oc: Occurrence) = {
      val term1op = s1.root.antecedent.find(x => x == term1oc)
      val term2op = s1.root.antecedent.find(x => x == term2oc)
      if (term1op == None || term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val term2 = term2op.get
        if (term1.formula != term2.formula) throw new LKRuleCreationException("Formulas to be contracted are not identical: term1.formula = " + term1.formula.toStringSimple + ", term2.formula = " + term2.formula.toStringSimple)
        else if (term1 == term2) throw new LKRuleCreationException("Formulas to be contracted are of the same occurrence")
        else {
          val prinFormula = term1.factory.createPrincipalFormulaOccurrence(term1.formula, term1::term2::Nil, s1.root.antecedent - term1 - term2)
          new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent - term1 - term2) + prinFormula, createContext(s1.root.succedent)), s1)
            with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
              def rule = ContractionLeftRuleType
              def aux = (term1::term2::Nil)::Nil
              def prin = prinFormula::Nil
            }
          }
      }
    }
    // convenient method to choose the first two formulas
    def apply(s1: LKProof, term1: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas  = {
      (s1.root.antecedent.filter(x => x.formula == term1)).toList match {
        case (x::y::_) => apply(s1, x, y)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == ContractionLeftRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::a2::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, a2, p1))
      }
      else None
  }

  object ContractionRightRule {
    def apply(s1: LKProof, term1oc: Occurrence, term2oc: Occurrence) = {
      val term1op = s1.root.succedent.find(x => x == term1oc)
      val term2op = s1.root.succedent.find(x => x == term2oc)
      if (term1op == None || term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val term2 = term2op.get
        if (term1.formula != term2.formula) throw new LKRuleCreationException("Formulas to be contracted are not identical: term1.formula = " + term1.formula.toStringSimple + ", term2.formula = " + term2.formula.toStringSimple + ", end-sequent: " + s1.root.getSequent.toStringSimple)
        else if (term1 == term2) throw new LKRuleCreationException("Formulas to be contracted are of the same occurrence")
        else {
          val prinFormula = term1.factory.createPrincipalFormulaOccurrence(term1.formula, term1::term2::Nil, s1.root.succedent - term1 - term2)
          new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent), createContext(s1.root.succedent - term1 - term2) + prinFormula), s1)
            with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
              def rule = ContractionRightRuleType
              def aux = (term1::term2::Nil)::Nil
              def prin = prinFormula::Nil
            }
        }
      }
    }
    // convenient method to choose the first two formulas
    def apply(s1: LKProof, term1: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas  = {
      (s1.root.succedent.filter(x => x.formula == term1)).toList match {
        case (x::y::_) => apply(s1, x, y)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == ContractionRightRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::a2::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, a2, p1))
      }
      else None
  }

  object CutRule {
    def apply(s1: LKProof, s2: LKProof, term1oc: Occurrence, term2oc: Occurrence) = {
      val term1op = s1.root.succedent.find(x => x == term1oc)
      val term2op = s2.root.antecedent.find(x => x == term2oc)
      if (term1op == None || term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val term2 = term2op.get
        if (term1.formula != term2.formula) throw new LKRuleCreationException("Formulas to be cut are not identical")
        else {
          new BinaryTree[SequentOccurrence](SequentOccurrence(
              createContext(s1.root.antecedent) ++ createContext(s2.root.antecedent - term2, s1.root.antecedent),
              createContext(s1.root.succedent - term1) ++ createContext(s2.root.succedent, s1.root.succedent - term1))
            , s1, s2)
            with BinaryLKProof with AuxiliaryFormulas {
                def rule = CutRuleType
                def aux = (term1::Nil)::(term2::Nil)::Nil
            }
        }
      }
    }
    // convenient method to choose the first two formulas
    def apply(s1: LKProof, s2: LKProof, term1: HOLFormula): BinaryTree[SequentOccurrence] with BinaryLKProof with AuxiliaryFormulas  = {
      ((s1.root.succedent.filter(x => x.formula == term1)).toList,(s2.root.antecedent.filter(x => x.formula == term1)).toList) match {
        case ((x::_),(y::_)) => apply(s1, s2, x, y)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == CutRuleType) {
        val r = proof.asInstanceOf[BinaryLKProof with AuxiliaryFormulas]
        val ((a1::Nil)::(a2::Nil)::Nil) = r.aux
        Some((r.uProof1, r.uProof2, r.root, a1, a2))
      }
      else None
  }

  object AndRightRule {
    def computeLeftAux( main: HOLFormula ) = main match { case And(l, _) => l }

    def computeRightAux( main: HOLFormula ) = main match { case And(_, r) => r }
    
    def apply(s1: LKProof, s2: LKProof, term1oc: Occurrence, term2oc: Occurrence) = {
      val term1op = s1.root.succedent.find(x => x == term1oc)
      val term2op = s2.root.succedent.find(x => x == term2oc)
      if (term1op == None || term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val term2 = term2op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(And(term1.formula, term2.formula), term1::term2::Nil, ((s1.root.succedent - term1) ++ (s2.root.succedent - term2)))
        new BinaryTree[SequentOccurrence](SequentOccurrence(
            createContext(s1.root.antecedent) ++ createContext(s2.root.antecedent, s1.root.antecedent),
            createContext(s1.root.succedent - term1) ++ createContext(s2.root.succedent - term2, s1.root.succedent - term1) + prinFormula
          ), s1, s2)
          with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = AndRightRuleType
            def aux = ((term1)::Nil)::(term2::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first two formulas
    def apply(s1: LKProof, s2: LKProof, term1: HOLFormula, term2: HOLFormula): BinaryLKProof with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas  = {
      ((s1.root.succedent.filter(x => x.formula == term1)).toList,(s2.root.succedent.filter(x => x.formula == term2)).toList) match {
        case ((x::_),(y::_)) => apply(s1, s2, x, y)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == AndRightRuleType) {
        val r = proof.asInstanceOf[BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::(a2::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof1, r.uProof2, r.root, a1, a2, p1))
      }
      else None
  }

  object AndLeft1Rule {
    def computeAux( main: HOLFormula ) = main match { case And(l, _) => l }

    def apply(s1: LKProof, term1oc: Occurrence, term2: HOLFormula) = {
      val term1op = s1.root.antecedent.find(x => x == term1oc)
      if (term1op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the left part of the sequent")
      else {
        val term1 = term1op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(And(term1.formula, term2), term1::Nil, s1.root.antecedent - term1)
        new UnaryTree[SequentOccurrence](SequentOccurrence(createContext((s1.root.antecedent - term1)) + prinFormula, createContext(s1.root.succedent)), s1)
          with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = AndLeft1RuleType
            def aux = (term1::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first  formula
    def apply(s1: LKProof, term1: HOLFormula, term2: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
      (s1.root.antecedent.filter(x => x.formula == term1)).toList match {
        case (x::_) => apply(s1, x, term2)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == AndLeft1RuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1))
      }
      else None
  }

  object AndLeft2Rule {
    def computeAux( main: HOLFormula ) = main match { case And(_, r) => r }

    def apply(s1: LKProof, term1: HOLFormula, term2oc: Occurrence) = {
      val term2op = s1.root.antecedent.find(x => x == term2oc)
      if (term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the left part of the sequent")
      else {
        val term2 = term2op.get
        val prinFormula = term2.factory.createPrincipalFormulaOccurrence(And(term1, term2.formula), term2::Nil, s1.root.antecedent - term2)
        new UnaryTree[SequentOccurrence](SequentOccurrence(createContext((s1.root.antecedent - term2)) + prinFormula, createContext(s1.root.succedent)), s1)
          with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = AndLeft2RuleType
            def aux = (term2::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first  formula
    def apply(s1: LKProof, term1: HOLFormula, term2: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
      (s1.root.antecedent.filter(x => x.formula == term2)).toList match {
        case (x::_) => apply(s1, term1, x)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == AndLeft2RuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1))
      }
      else None
  }

  object OrLeftRule {
    def computeLeftAux( main: HOLFormula ) = main match { case Or(l, _) => l }

    def computeRightAux( main: HOLFormula ) = main match { case Or(_, r) => r }

    def apply(s1: LKProof, s2: LKProof, term1oc: Occurrence, term2oc: Occurrence) = {
      val term1op = s1.root.antecedent.find(x => x == term1oc)
      val term2op = s2.root.antecedent.find(x => x == term2oc)
      if (term1op == None || term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val term2 = term2op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(Or(term1.formula, term2.formula), term1::term2::Nil, (s1.root.antecedent - term1) ++ (s2.root.antecedent - term2))
        new BinaryTree[SequentOccurrence](SequentOccurrence(
            ((createContext(s1.root.antecedent - term1) ++ createContext(s2.root.antecedent - term2, s1.root.antecedent - term1))) + prinFormula,
            createContext(s1.root.succedent) ++ createContext(s2.root.succedent, s1.root.succedent)
          ), s1, s2)
          with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = OrLeftRuleType
            def aux = ((term1)::Nil)::(term2::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first two formulas
    def apply(s1: LKProof, s2: LKProof, term1: HOLFormula, term2: HOLFormula): BinaryTree[SequentOccurrence] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas  = {
      ((s1.root.antecedent.filter(x => x.formula == term1)).toList,(s2.root.antecedent.filter(x => x.formula == term2)).toList) match {
        case ((x::_),(y::_)) => apply(s1, s2, x, y)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == OrLeftRuleType) {
        val r = proof.asInstanceOf[BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::(a2::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof1, r.uProof2, r.root, a1, a2, p1))
      }
      else None
  }

  object OrRight1Rule {
    def computeAux( main: HOLFormula ) = main match { case Or(l, _) => l }

    def apply(s1: LKProof, term1oc: Occurrence, term2: HOLFormula) = {
      val term1op = s1.root.succedent.find(x => x == term1oc)
      if (term1op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(Or(term1.formula, term2), term1::Nil, s1.root.succedent - term1)
        new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent), createContext((s1.root.succedent - term1)) + prinFormula), s1)
          with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = OrRight1RuleType
            def aux = ((term1)::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first  formula
    def apply(s1: LKProof, term1: HOLFormula, term2: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
      (s1.root.succedent.filter(x => x.formula == term1)).toList match {
        case (x::_) => apply(s1, x, term2)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == OrRight1RuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1))
      }
      else None
  }

  object OrRight2Rule {
    def computeAux( main: HOLFormula ) = main match { case Or(_, r) => r }

    def apply(s1: LKProof, term1: HOLFormula, term2oc: Occurrence) = {
      val term2op = s1.root.succedent.find(x => x == term2oc)
      if (term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term2 = term2op.get
        val prinFormula = term2.factory.createPrincipalFormulaOccurrence(Or(term1, term2.formula), term2::Nil, s1.root.succedent - term2)
        new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent), createContext((s1.root.succedent - term2)) + prinFormula), s1)
          with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = OrRight2RuleType
            def aux = ((term2)::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first formula
    def apply(s1: LKProof, term1: HOLFormula, term2: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
      (s1.root.succedent.filter(x => x.formula == term2)).toList match {
        case (x::_) => apply(s1, term1, x)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == OrRight2RuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1))
      }
      else None
  }

  object ImpLeftRule {
    def computeLeftAux( main: HOLFormula ) = main match { case Imp(l, _) => l }

    def computeRightAux( main: HOLFormula ) = main match { case Imp(_, r) => r }

    def apply(s1: LKProof, s2: LKProof, term1oc: Occurrence, term2oc: Occurrence) = {
      val term1op = s1.root.succedent.find(x => x == term1oc)
      val term2op = s2.root.antecedent.find(x => x == term2oc)
      if (term1op == None || term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val term2 = term2op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(Imp(term1.formula, term2.formula), term1::term2::Nil, s1.root.antecedent  ++ (s2.root.antecedent - term2))
        new BinaryTree[SequentOccurrence](SequentOccurrence(
            createContext(s1.root.antecedent)  ++ createContext(s2.root.antecedent - term2, s1.root.antecedent) + prinFormula,
            createContext(s1.root.succedent - term1) ++ createContext(s2.root.succedent, s1.root.succedent - term1)
          ), s1, s2)
          with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = ImpLeftRuleType
            def aux = (term1::Nil)::(term2::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first two formulas
    def apply(s1: LKProof, s2: LKProof, term1: HOLFormula, term2: HOLFormula): BinaryTree[SequentOccurrence] with BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas  = {
      ((s1.root.succedent.filter(x => x.formula == term1)).toList,(s2.root.antecedent.filter(x => x.formula == term2)).toList) match {
        case ((x::_),(y::_)) => apply(s1, s2, x, y)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == ImpLeftRuleType) {
        val r = proof.asInstanceOf[BinaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::(a2::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof1, r.uProof2, r.root, a1, a2, p1))
      }
      else None
  }

  object ImpRightRule {
    def apply(s1: LKProof, term1oc: Occurrence, term2oc: Occurrence) = {
      val term1op = s1.root.antecedent.find(x => x == term1oc)
      val term2op = s1.root.succedent.find(x => x == term2oc)
      if (term1op == None || term2op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val term2 = term2op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(Imp(term1.formula, term2.formula), term1::term2::Nil, s1.root.succedent - term2)
        new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent - term1), createContext(s1.root.succedent - term2) + prinFormula), s1)
          with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = ImpRightRuleType
            def aux = (term1::term2::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first formula
    def apply(s1: LKProof, term1: HOLFormula, term2: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
      ((s1.root.antecedent.filter(x => x.formula == term1)).toList,(s1.root.succedent.filter(x => x.formula == term2)).toList) match {
        case ((x::_),(y::_)) => apply(s1, x, y)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == ImpRightRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::a2::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, a2, p1))
      }
      else None
  }

  object NegLeftRule {
    def computeAux( main: HOLFormula ) = main match { case Neg(s) => s }
    def apply(s1: LKProof, term1oc: Occurrence) = {
      val term1op = s1.root.succedent.find(x => x == term1oc)
      if (term1op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(Neg(term1.formula), term1::Nil, s1.root.antecedent)
        new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent) + prinFormula, createContext(s1.root.succedent - term1)), s1)
          with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = NegLeftRuleType
            def aux = (term1::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first formula
    def apply(s1: LKProof, term1: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
      (s1.root.succedent.filter(x => x.formula == term1)).toList match {
        case (x::_) => apply(s1, x)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == NegLeftRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1))
      }
      else None
  }

  object NegRightRule {
    def computeAux( main: HOLFormula ) = main match { case Neg(s) => s }
    def apply(s1: LKProof, term1oc: Occurrence) = {
      val term1op = s1.root.antecedent.find(x => x == term1oc)
      if (term1op == None) throw new LKRuleCreationException("Auxialiary formulas are not contained in the right part of the sequent")
      else {
        val term1 = term1op.get
        val prinFormula = term1.factory.createPrincipalFormulaOccurrence(Neg(term1.formula), term1::Nil, s1.root.succedent)
        new UnaryTree[SequentOccurrence](SequentOccurrence(createContext(s1.root.antecedent - term1), createContext(s1.root.succedent) + prinFormula), s1)
          with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas {
            def rule = NegRightRuleType
            def aux = (term1::Nil)::Nil
            def prin = prinFormula::Nil
          }
      }
    }
    // convenient method to choose the first formula
    def apply(s1: LKProof, term1: HOLFormula): UnaryTree[SequentOccurrence] with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas = {
      (s1.root.antecedent.filter(x => x.formula == term1)).toList match {
        case (x::_) => apply(s1, x)
        case _ => throw new LKRuleCreationException("Not matching formula occurrences found for application of the rule with the given formula")
      }
    }
    def unapply(proof: LKProof) = if (proof.rule == NegRightRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1))
      }
      else None
  }

  object ImplicitConverters {
    implicit def axiomMapToAxiom(axiomMap: Pair[LKProof, Pair[List[FormulaOccurrence],List[FormulaOccurrence]]]): LKProof = axiomMap._1
  }
}