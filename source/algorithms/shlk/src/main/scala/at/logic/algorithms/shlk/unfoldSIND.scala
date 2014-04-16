package at.logic.algorithms.shlk

import at.logic.calculi.slk._
import at.logic.language.schema._
import at.logic.calculi.occurrences._
import at.logic.calculi.lk._
import at.logic.calculi.lk.base._
import scala.Predef._
import at.logic.language.schema.{lessThan,sims,leq}
import at.logic.language.lambda.types.{->, Ti, Tindex}
import at.logic.language.lambda.symbols._

object applySchemaSubstitution2 {
  def apply( proof_name: String, number: Int, ProofLinkPassing:List[Pair[SchemaExpression,SchemaExpression]]): LKProof =  {
     val aa = function001(proof_name,number,ProofLinkPassing)
     println(aa._1.toString())
     //CloneLKProof2(aa._2,"",aa._1,0,0,List())._2
    aa._2
  }
  def function001( proof_name: String, number: Int, ProofLinkPassing:List[Pair[SchemaExpression,SchemaExpression]]): Pair[List[Pair[SchemaFormula,SchemaFormula]],LKProof] =
    if(number == 0) CloneLKProof2(SchemaProofDB.get(proof_name).base,proof_name,List(),0,1,ProofLinkPassing) else anotherfunction(proof_name: String, maketogether(number), ProofLinkPassing)
  def anotherfunction(proof_name: String, number:SchemaExpression, ProofLinkPassing:List[Pair[SchemaExpression,SchemaExpression]] ): Pair[List[Pair[SchemaFormula,SchemaFormula]],LKProof] = {
    val k =  backToInt(number)
    if (k  == 0)  CloneLKProof2(SchemaProofDB.get(proof_name).base,proof_name,List(),k,1,ProofLinkPassing)
    else{
      val proofist = CloneLKProof2(SchemaProofDB.get(proof_name).rec,proof_name,List(),k-1,2, ProofLinkPassing)
      CloneLKProof2(proofist._2,proof_name,List(),k-1,1,ProofLinkPassing)
    }
  }
}
object CloneLKProof2 {
  def apply(proof: LKProof,name:String, rewriterules:List[Pair[SchemaFormula,SchemaFormula]],proofSize: Int, version: Int, ProofLinkPassing:List[Pair[SchemaExpression,SchemaExpression]] ):Pair[List[Pair[SchemaFormula,SchemaFormula]],LKProof] = {
    proof match {
      case trsArrowLeftRule(p, s, a, m) =>  if(version == 0) Pair(List(),proof) else if(version == 1) apply(p,name,rewriterules,proofSize,version,ProofLinkPassing) else if(version == 2) Pair(List(),proof) else  Pair(List(),proof)
      case trsArrowRightRule(p, s, a, m) => if(version == 0) Pair(List(),proof) else if(version == 1) apply(p,name,rewriterules,proofSize,version,ProofLinkPassing) else if(version == 2) Pair(List(),proof) else  Pair(List(),proof)
      case foldLeftRule(p, s, a, m) => {
        if(version == 0) apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
        else if(version == 1) {
          val aa = iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)
          val mm = iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
           Pair(new_p._1 :+ rewriterulereplace(mm,aa),foldLeftRule(new_p._2, aa, mm))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,foldLeftRule(new_p._2,cloneMySol(a.formula.asInstanceOf[SchemaFormula],proofSize),cloneMySol(m.formula.asInstanceOf[SchemaFormula],proofSize)))
        }
        else Pair(List(),proof)
      }
      case foldRightRule(p, s, a, m) => {
        if(version == 0) apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
        else if(version == 1){
          val aa = iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)
          val mm = iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1 :+ rewriterulereplace(mm,aa),foldRightRule(new_p._2, aa, mm))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,foldRightRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize)))
        }
        else Pair(List(),proof)
      }
      case foldRule(p, s, a, m) => if(version == 0) Pair(List(),proof) else if(version == 1)apply(p,name,rewriterules,proofSize,version,ProofLinkPassing) else if(version == 2) Pair(List(),proof) else Pair(List(),proof)


      case Axiom(ro) => {
        if(version == 0)Pair(List(),Axiom(ro.antecedent.map(fo => defineremove(fo.formula.asInstanceOf[SchemaFormula],rewriterules)),ro.succedent.map(fo => defineremove(fo.formula.asInstanceOf[SchemaFormula],rewriterules))))
        else if(version == 1)  Pair(List(),Axiom(ro.antecedent.map(fo => iterateOnFormula(fo.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)),ro.succedent.map(fo => iterateOnFormula(fo.formula.asInstanceOf[SchemaFormula],ProofLinkPassing))))
        else if(version == 2){
          Pair(List(),Axiom(ro.antecedent.map(fo => cloneMySol(fo.formula.asInstanceOf[SchemaFormula],proofSize)),ro.succedent.map(fo => cloneMySol(fo.formula.asInstanceOf[SchemaFormula],proofSize))))
        }
        else Pair(List(),proof)
      }

      case AndLeftEquivalenceRule1(p, s, a, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndLeftEquivalenceRule1(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndLeftEquivalenceRule1(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndLeftEquivalenceRule1(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize)))
        }
        else Pair(List(),proof)
      }
      case AndRightEquivalenceRule1(p, s, a, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, AndRightEquivalenceRule1(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, AndRightEquivalenceRule1(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, AndRightEquivalenceRule1(new_p._2, cloneMySol(a.formula,proofSize),cloneMySol(m.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case OrRightEquivalenceRule1(p, s, a, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,OrRightEquivalenceRule1(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,OrRightEquivalenceRule1(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,OrRightEquivalenceRule1(new_p._2, cloneMySol(a.formula,proofSize),  cloneMySol(m.formula,proofSize)))
        }
        else Pair(List(),proof)
      }
      case AndLeftEquivalenceRule3(p, s, a, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndLeftEquivalenceRule3(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndLeftEquivalenceRule3(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndLeftEquivalenceRule3(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize)))
        }
        else Pair(List(),proof)
      }
      case AndRightEquivalenceRule3(p, s, a, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndRightEquivalenceRule3(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,AndRightEquivalenceRule3(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, AndRightEquivalenceRule3(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case OrRightEquivalenceRule3(p, s, a, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,OrRightEquivalenceRule3(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules),defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,OrRightEquivalenceRule3(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing),iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,OrRightEquivalenceRule3(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case WeakeningLeftRule(p, _, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          implicit val factory = defaultFormulaOccurrenceFactory
          Pair(new_p._1,WeakeningLeftRule( new_p._2, defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          implicit val factory = defaultFormulaOccurrenceFactory
          Pair(new_p._1,WeakeningLeftRule( new_p._2, iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          implicit val factory = defaultFormulaOccurrenceFactory
          Pair(new_p._1,WeakeningLeftRule( new_p._2, cloneMySol(m.formula,proofSize) ) )
        }
        else Pair(List(),proof)
      }

      case WeakeningRightRule(p, _, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          implicit val factory = defaultFormulaOccurrenceFactory
          Pair(new_p._1,WeakeningRightRule( new_p._2, defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          implicit val factory = defaultFormulaOccurrenceFactory
          Pair(new_p._1,WeakeningRightRule( new_p._2, iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          implicit val factory = defaultFormulaOccurrenceFactory
          Pair(new_p._1, WeakeningRightRule( new_p._2, cloneMySol(m.formula,proofSize) ))
        }
        else Pair(List(),proof)
      }

      case CutRule( p1, p2, _, a1, a2 ) => {
        if(version == 0){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,CutRule(new_p1._2, new_p2._2, defineremove(a2.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,CutRule(new_p1._2, new_p2._2, iterateOnFormula(a2.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,CutRule(new_p1._2, new_p2._2, cloneMySol(a2.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case OrLeftRule(p1, p2, _, a1, a2, m) => {
        if(version == 0){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,OrLeftRule(new_p1._2, new_p2._2, defineremove(a1.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(a2.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,OrLeftRule(new_p1._2, new_p2._2, iterateOnFormula(a1.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(a2.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,OrLeftRule(new_p1._2, new_p2._2, cloneMySol(a1.formula,proofSize), cloneMySol(a2.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case AndRightRule(p1, p2, _, a1, a2, m) => {
        if(version == 0){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1, AndRightRule(new_p1._2, new_p2._2, defineremove(a1.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(a2.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1, AndRightRule(new_p1._2, new_p2._2, iterateOnFormula(a1.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(a2.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,AndRightRule(new_p1._2, new_p2._2, cloneMySol(a1.formula,proofSize), cloneMySol(a2.formula,proofSize)) )
        }
        else Pair(List(),proof)
      }
      case ImpLeftRule(p1, p2, s, a1, a2, m) => {
        if(version == 0){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1, ImpLeftRule(new_p1._2, new_p2._2, defineremove(a1.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(a2.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1, ImpLeftRule(new_p1._2, new_p2._2, iterateOnFormula(a1.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(a2.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p1 =  apply(p1,name,rewriterules,proofSize,version,ProofLinkPassing)
          val new_p2 =  apply(p2,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p1._1 ++ new_p2._1,ImpLeftRule(new_p1._2, new_p2._2, cloneMySol(a1.formula,proofSize), cloneMySol(a2.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case NegLeftRule( p, _, a, m ) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,NegLeftRule( new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules) ))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
         Pair(new_p._1,NegLeftRule( new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing) ))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,NegLeftRule( new_p._2, cloneMySol(a.formula,proofSize) ))
        }
        else Pair(List(),proof)
      }

      case AndLeft1Rule(p, r, a, m) =>  {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case And(l, right) => right }
          Pair(new_p._1,AndLeft1Rule( new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(a2.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case And(l, right) => right }
          Pair(new_p._1,AndLeft1Rule( new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(a2.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case And(l, right) => right }
          Pair(new_p._1,AndLeft1Rule( new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(a2,proofSize)) )
        }
        else Pair(List(),proof)
      }

      case AndLeft2Rule(p, r, a, m) =>  {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case And(l, _) => l }
          Pair(new_p._1, AndLeft2Rule( new_p._2, defineremove(a2.asInstanceOf[SchemaFormula],rewriterules), defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules) ) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case And(l, _) => l }
          Pair(new_p._1, AndLeft2Rule( new_p._2, iterateOnFormula(a2.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing) ) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case And(l, _) => l }
          Pair(new_p._1,AndLeft2Rule( new_p._2, cloneMySol(a2,proofSize), cloneMySol(a.formula,proofSize) ))
        }
        else Pair(List(),proof)
      }

      case OrRight1Rule(p, r, a, m) =>  {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case Or(_, ra) => ra }
          Pair(new_p._1,OrRight1Rule( new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules),defineremove(a2.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
           val a2 = m.formula  match { case Or(_, ra) => ra }
          Pair(new_p._1,OrRight1Rule( new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing),iterateOnFormula(a2.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case Or(_, ra) => ra }
          Pair(new_p._1, OrRight1Rule( new_p._2,cloneMySol(a.formula,proofSize),cloneMySol(a2,proofSize)))
        }
        else Pair(List(),proof)
      }

      case OrRight2Rule(p, r, a, m) =>  {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula match { case Or(l, _) => l }
          Pair(new_p._1, OrRight2Rule( new_p._2, defineremove(a2.asInstanceOf[SchemaFormula],rewriterules), defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
           val a2 = m.formula match { case Or(l, _) => l }
          Pair(new_p._1, OrRight2Rule( new_p._2, iterateOnFormula(a2.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          val a2 = m.formula  match { case Or(l, _) => l }
          Pair(new_p._1, OrRight2Rule( new_p._2, cloneMySol(a2,proofSize), cloneMySol(a.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case NegRightRule( p, _, a, m ) => {
         if(version == 0){
           val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
           Pair(new_p._1, NegRightRule( new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules) ))
         }
         else if(version == 1){
            val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
           Pair(new_p._1, NegRightRule( new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing) ))
         }
         else if(version == 2){
           val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
           Pair(new_p._1,NegRightRule( new_p._2, cloneMySol(a.formula,proofSize)))
         }
         else Pair(List(),proof)
      }

      case ContractionLeftRule(p, _, a1, a2, m) => {


        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          println("fdkjskjf  "+defineremove(a1.formula.asInstanceOf[SchemaFormula],rewriterules))
          println("fdkjskjssdfdsf  "+ new_p._2.root)
          Pair(new_p._1,ContractionLeftRule(new_p._2, defineremove(a1.formula.asInstanceOf[SchemaFormula],rewriterules)))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ContractionLeftRule( new_p._2, iterateOnFormula(a1.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ContractionLeftRule( new_p._2, cloneMySol(a1.formula,proofSize)))
        }
        else Pair(List(),proof)
      }

      case ContractionRightRule(p, _, a1, a2, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ContractionRightRule( new_p._2, defineremove(a1.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ContractionRightRule( new_p._2, iterateOnFormula(a1.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ContractionRightRule( new_p._2, cloneMySol(a1.formula,proofSize)))
        }
        else Pair(List(),proof)
      }
      // QUANTIFIER WILL NEED EXTRA WORK FOR TERM ->>
      case ForallLeftRule(p, seq, a, m, t) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          println(defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules).toString+ "   "+ defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules).toString)
          Pair(new_p._1, ForallLeftRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), t))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, ForallLeftRule(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(t,ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallLeftRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(t,proofSize)))
        }
        else Pair(List(),proof)
      }
      case ForallRightRule(p, seq, a, m, v) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallRightRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), v) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallRightRule(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(v,ProofLinkPassing).asInstanceOf[SchemaVar]) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallRightRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(v,proofSize).asInstanceOf[SchemaVar]) )
        }
        else Pair(List(),proof)
      }

      case ExistsRightRule(p, seq, a, m, t) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsRightRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), t))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsRightRule(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(t,ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsRightRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(t,proofSize)))
        }
        else Pair(List(),proof)
      }
      case ExistsLeftRule(p, seq, a, m, v) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, ExistsLeftRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), v))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, ExistsLeftRule(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(v,ProofLinkPassing).asInstanceOf[SchemaVar]))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsLeftRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(v,proofSize).asInstanceOf[SchemaVar]))
        }
        else Pair(List(),proof)
      }

      case ExistsHyperRightRule(p, seq, a, m, t) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsHyperRightRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), t))
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsHyperRightRule(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(t,ProofLinkPassing)))
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsHyperRightRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(t,proofSize)) )
        }
        else Pair(List(),proof)
      }
      case ExistsHyperLeftRule(p, seq, a, m, v) => {
        if(version == 0)apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsHyperLeftRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), v) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ExistsHyperLeftRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(v,proofSize).asInstanceOf[SchemaVar]))
        }
        else Pair(List(),proof)
      }
      case ForallHyperLeftRule(p, seq, a, m, t) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallHyperLeftRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), t) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallHyperLeftRule(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(t,ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, ForallHyperLeftRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(t,proofSize) ))
        }
        else Pair(List(),proof)
      }
      case ForallHyperRightRule(p, seq, a, m, v) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallHyperRightRule(new_p._2, defineremove(a.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(m.formula.asInstanceOf[SchemaFormula],rewriterules), v) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallHyperRightRule(new_p._2, iterateOnFormula(a.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(m.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(v,ProofLinkPassing).asInstanceOf[SchemaVar]) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ForallHyperRightRule(new_p._2, cloneMySol(a.formula,proofSize), cloneMySol(m.formula,proofSize), cloneMyTerm(v,proofSize).asInstanceOf[SchemaVar] ))
        }
        else Pair(List(),proof)
      }

      case ImpRightRule(p, s, a1, a2, m) => {
        if(version == 0){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ImpRightRule(new_p._2, defineremove(a1.formula.asInstanceOf[SchemaFormula],rewriterules), defineremove(a2.formula.asInstanceOf[SchemaFormula],rewriterules)) )
        }
        else if(version == 1){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1,ImpRightRule(new_p._2, iterateOnFormula(a1.formula.asInstanceOf[SchemaFormula],ProofLinkPassing), iterateOnFormula(a2.formula.asInstanceOf[SchemaFormula],ProofLinkPassing)) )
        }
        else if(version == 2){
          val new_p = apply(p,name,rewriterules,proofSize,version,ProofLinkPassing)
          Pair(new_p._1, ImpRightRule(new_p._2, cloneMySol(a1.formula,proofSize), cloneMySol(a2.formula,proofSize)) )
        }
        else Pair(List(),proof)
      }
      case FOSchemaProofLinkRule(s,name2,l) => {
        if(version == 0) Pair(List(),proof)

        else if(version == 1){
          val next = backToInt(l.head)
          val newList= l.tail.map(x => iterateOnFormula(x,ProofLinkPassing))
          if(next == 0){
            if(SchemaProofDB.getLinkTerms(name2).length != 0 && SchemaProofDB.getLinkTerms(name2).length == newList.length) {

              val ProofLinkPassingNew = SchemaProofDB.getLinkTerms(name2).zip(newList)
              CloneLKProof2(SchemaProofDB.get(name2).base,name2,rewriterules,0,version,ProofLinkPassingNew)
            }
            else if(SchemaProofDB.getLinkTerms(name2).length == 0 && SchemaProofDB.getLinkTerms(name2).length == newList.length)
              CloneLKProof2(SchemaProofDB.get(name2).base,name2,rewriterules,0,version,List())
            else throw new Exception("ERROR ProofLinks are wrong !\n")
          }
          else{
            if(SchemaProofDB.getLinkTerms(name2).length != 0 && SchemaProofDB.getLinkTerms(name2).length == newList.length) {
              val ProofLinkPassingNew = SchemaProofDB.getLinkTerms(name2).zip(newList)
              applySchemaSubstitution2.function001(name2,next,ProofLinkPassingNew)
            }
            else if(SchemaProofDB.getLinkTerms(name2).length == 0 && SchemaProofDB.getLinkTerms(name2).length == newList.length)
              applySchemaSubstitution2.function001(name2,next,List())
            else throw new Exception("ERROR ProofLinks are wrong !\n")
          }
        }
        else if(version == 2) Pair(List(),FOSchemaProofLinkRule(
          new FSequent(s.antecedent.map( x => cloneMySol(x.formula,proofSize)),
                      s.succedent.map(x => cloneMySol(x.formula,proofSize))),name2,l.map(x => cloneMyTerm(x,proofSize))))
        else Pair(List(),proof)
      }
      case _ => throw new Exception("ERROR in unfolding: CloneLKProof2: missing rule !\n" + proof.rule + "\n")
    }}
}

object defineremove {def apply(form:SchemaFormula,rewriterules:List[Pair[SchemaFormula,SchemaFormula]]):SchemaFormula  =  rewriterules.foldLeft(Pair(true,form))((f,p)=> if (AtomMatch(f._2,p._1)&& f._1) Pair(false,cloneMySol(p._2,rewriterules)) else f)._2}
object rewriterulereplace {def apply(p:Pair[SchemaFormula,SchemaFormula]):Pair[SchemaFormula,SchemaFormula]  =  if (AtomMatch(p._1)){
  val pairone:SchemaFormula = {p._1 match{ case Atom(x,y) => y case _ => List()}}.tail.foldLeft(Pair(0,p._2))((pairppair,t)=> (pairppair._1+1,genterm(pairppair._1,pairppair._2,t)))._2
  Pair(p._1 match{ case Atom(x,y) => Atom(x, List(y.head) ++ y.tail.foldLeft(Pair(0,List().asInstanceOf[List[SchemaExpression]]))((pair,t) => (pair._1 +1, pair._2.asInstanceOf[List[SchemaExpression]]:+ SchemaConst(("!"+pair._1+"!" ),Ti) ) )._2 )
  case x => x},pairone) }
else p}
object iterateOnFormula {
  def apply(form:SchemaFormula,ProofLinkPassing:List[Pair[SchemaExpression,SchemaExpression]]):SchemaFormula = ProofLinkPassing.foldLeft(form)( (f, pair) => cloneMySol(f,pair._1,pair._2))
  def apply(term:SchemaExpression,ProofLinkPassing:List[Pair[SchemaExpression,SchemaExpression]]):SchemaExpression = ProofLinkPassing.foldLeft(term)( (t, pair) => {cloneMyTerm(t,pair._1,pair._2)})
}
object genterm {
  def apply( n: Int,p:SchemaFormula,t:SchemaExpression): SchemaFormula = {
    p match {
      case Neg(nF) => {
        val finForm = apply(n,nF,t)
        Neg(finForm)
      }
      case And(lF,rF) => {
        val finFormL = apply(n,lF,t)
        val finFormR = apply(n,rF,t)
        And(finFormL,finFormR)
      }
      case Or(lF,rF) => {
        val finFormL = apply(n,lF,t)
        val finFormR = apply(n,rF,t)
        Or(finFormL,finFormR)
      }
      case Imp(lF,rF) => {
        val finFormL = apply(n,lF,t)
        val finFormR = apply(n,rF,t)
        Imp(finFormL,finFormR)
      }
      case AllVar(aV,aF) => {
        val finForm = apply(n,aF,t)
        AllVar(aV,finForm)
      }
      case ExVar(aV,aF) => {
        val finForm = apply(n,aF,t)
        ExVar(aV,finForm)
      }
      case Equation(l,r) => Equation(apply(n,l,t),apply(n,r,t))
      case lessThan(l,r) => lessThan(apply(n,l,t),apply(n,r,t))
      case sims(l,r) => sims(apply(n,l,t),apply(n,r,t))
      case leq(l,r) =>  leq(apply(n,l,t),apply(n,r,t))
      case Atom(x,y) if isIndexSort(y.head) => Atom(x,List(y.head) ++ y.map( x => apply(n,x,t) ))
      case Atom(x,y) => Atom(x, y.map( x => apply(n,x,t) ))
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + p.toString + "\n")
    }
  }
  def apply( ii: Int, p:SchemaExpression, t:SchemaExpression): SchemaExpression = {
    t match {
      case Function(head,l,Tindex) => t
      case SchemaVar(name,Tindex) if name == "k" => t
      case Function(head,l,Ti)  => p match {
        case Function(headi,li,Ti) if head.name == headi.name && l.length == li.length &&
         l.zip(li).foldLeft(true,true)((b,pair) => if(equalterms(pair._1,pair._2)&&b._2) b else (b._1,false) )._1  => SchemaConst(SymbolA("!"+ii+"!" ),Ti)
        case Function(headi,li,Ti) => Function(head, li.map(x => apply(ii,x,t)), Ti)
        case _ => p
      }
      case SchemaVar(head,->(Tindex,Ti)) => p match {
        case Function(headi,li,Ti) if  headi == head  => Function(SchemaConst("!"+ii+"!", FunctionType(Ti, li.map(_.exptype))),li,Ti)
        case _ => p
      }
      case SchemaVar(head,Ti)  => p match {
        case SchemaVar(head2,Ti) if  head2 == head  => SchemaConst("!"+ii+"!", Ti)
        case _ => p
      }
      case SchemaConst(head,tt)  =>  p match {
        case SchemaConst(head2,t2) if  tt == t2  && head2 == head => SchemaConst("!"+ii+"!", Ti)
        case _ => p
      }
      case SchemaAbs(x, tt) => p match {case SchemaAbs(x2, t2) if x.compare(x2) == 0 && equalterms(tt,t2) => apply(ii,t2,t)}
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + t.toString + "\n")

    }
  }
}

object cloneMySol{
  def apply(form:SchemaFormula, proofSize: Int ):SchemaFormula = {
    form match {
      case Neg(nF) => {
        val finForm = apply(nF.asInstanceOf[SchemaFormula],proofSize)
        Neg(finForm)
      }
      case And(lF,rF) => {
        val finFormL = apply(lF,proofSize)
        val finFormR = apply(rF,proofSize)
        And(finFormL,finFormR)
      }
      case Or(lF,rF) => {
        val finFormL = apply(lF,proofSize)
        val finFormR = apply(rF,proofSize)
        Or(finFormL,finFormR)
      }
      case Imp(lF,rF) => {
        val finFormL = apply(lF,proofSize)
        val finFormR = apply(rF,proofSize)
        Imp(finFormL,finFormR)
      }
      case AllVar(aV,aF) => {
        val finForm = apply(aF,proofSize)
        AllVar(aV,finForm)
      }
      case ExVar(aV,aF) => {
        val finForm = apply(aF,proofSize)
        ExVar(aV,finForm)
      }
      case Equation(l,r) => Equation(cloneMyTerm(l,proofSize),cloneMyTerm(r,proofSize))
      case lessThan(l,r) => lessThan(cloneMyTerm(l,proofSize),cloneMyTerm(r,proofSize))
      case sims(l,r) => sims(cloneMyTerm(l,proofSize),cloneMyTerm(r,proofSize))
      case leq(l,r) =>  leq(cloneMyTerm(l,proofSize),cloneMyTerm(r,proofSize))
      case Atom(set) => {
        val atomName = set.asInstanceOf[Pair[SymbolA,List[SchemaExpression]]]._1
        val SOLList = set.asInstanceOf[Pair[SymbolA,List[SchemaExpression]]]._2
        val finSOLList = SOLList.asInstanceOf[List[SchemaExpression]].map( x => cloneMyTerm(x,proofSize))
        Atom(atomName,finSOLList)
      }
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + form.toString + "\n")
    }
  }
  def apply(form:SchemaFormula, IN:SchemaExpression, OUT:SchemaExpression):SchemaFormula = {
    form match {
      case Neg(nF) => {
        val finForm = apply(nF.asInstanceOf[SchemaFormula],IN,OUT)
        Neg(finForm)
      }
      case And(lF,rF) => {
        val finFormL = apply(lF,IN,OUT)
        val finFormR = apply(rF,IN,OUT)
        And(finFormL,finFormR)
      }
      case Or(lF,rF) => {
        val finFormL = apply(lF,IN,OUT)
        val finFormR = apply(rF,IN,OUT)
        Or(finFormL,finFormR)
      }
      case Imp(lF,rF) => {
        val finFormL = apply(lF,IN,OUT)
        val finFormR = apply(rF,IN,OUT)
        Imp(finFormL,finFormR)
      }
      case AllVar(aV,aF) => {
        val finForm = apply(aF,IN,OUT)
        AllVar(aV,finForm)
      }
      case ExVar(aV,aF) => {
        val finForm = apply(aF,IN,OUT)
        ExVar(aV,finForm)
      }
      case Equation(l,r) => Equation(cloneMyTerm(l,IN,OUT),cloneMyTerm(r,IN,OUT))
      case lessThan(l,r) => lessThan(cloneMyTerm(l,IN,OUT),cloneMyTerm(r,IN,OUT))
      case sims(l,r) => sims(cloneMyTerm(l,IN,OUT),cloneMyTerm(r,IN,OUT))
      case leq(l,r) =>  leq(cloneMyTerm(l,IN,OUT),cloneMyTerm(r,IN,OUT))
      case Atom(set) => {
        val atomName = set.asInstanceOf[Pair[SymbolA,List[SchemaExpression]]]._1
        val SOLList = set.asInstanceOf[Pair[SymbolA,List[SchemaExpression]]]._2
        val finSOLList = SOLList.asInstanceOf[List[SchemaExpression]].map( x => cloneMyTerm(x,IN,OUT))
        Atom(atomName,finSOLList)
      }
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + form.toString + "\n")
    }
  }
  def apply(form:SchemaFormula,rewriterules:List[Pair[SchemaFormula,SchemaFormula]]):SchemaFormula = {
    form match {
      case Neg(nF) => {
        val finForm = apply(nF.asInstanceOf[SchemaFormula],rewriterules)
        Neg(finForm)
      }
      case And(lF,rF) => {
        val finFormL = apply(lF,rewriterules)
        val finFormR = apply(rF,rewriterules)
        And(finFormL,finFormR)
      }
      case Or(lF,rF) => {
        val finFormL = apply(lF,rewriterules)
        val finFormR = apply(rF,rewriterules)
        Or(finFormL,finFormR)
      }
      case Imp(lF,rF) => {
        val finFormL = apply(lF,rewriterules)
        val finFormR = apply(rF,rewriterules)
        Imp(finFormL,finFormR)
      }
      case AllVar(aV,aF) => {
        val finForm = apply(aF,rewriterules)
        AllVar(aV,finForm)
      }
      case ExVar(aV,aF) => {
        val finForm = apply(aF,rewriterules)
        ExVar(aV,finForm)
      }
      case Equation(l,r) => Equation(l,r)
      case lessThan(l,r) => lessThan(l,r)
      case sims(l,r) => sims(l,r)
      case leq(l,r) =>  leq(l,r)
      case Atom(_,_) => defineremove(form,rewriterules)
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + form.toString + "\n")
    }
  }
}

object cloneMyTerm{
  def apply(term:SchemaExpression , proofSize:Int):SchemaExpression = {
    term match {
      case Function(SymbolA(n),l,t) if n =="schS" && t == ind => Function(SymbolA(n),l.map(x => apply(x,proofSize)),t)
      case SchemaVar(SymbolA(n),t) if n== "k" && t == ind =>  maketogether(proofSize)
      case Function(SymbolA(n),l,t) if t == ind => Function(SymbolA(n),l.map(x => apply(x,proofSize)),t)
      case Function(SymbolA(n),l,t) if t == i => Function(SymbolA(n),l.map(x => apply(x,proofSize)),t)
      case SchemaVar(n,t) if t == i | t == ind->i =>SchemaVar(n,t)
      case SchemaConst(n,t)  => SchemaConst(n,t)
      case SchemaAbs(x, t)   => SchemaAbs(x, apply(t,proofSize))
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + term.toString + "\n")

    }
  }
  def apply(term:SchemaExpression , IN:SchemaExpression, OUT:SchemaExpression):SchemaExpression = {
    term match {
      case Function(SymbolA("schS"),l,Tindex) => Function(SymbolA("schS"),l,Tindex)
      case SchemaVar(SymbolA("k"),Tindex) =>  SchemaVar(SymbolA("k"),Tindex)
      case Function(SymbolA(n),l,Tindex) => Function(SymbolA(n),l.map(x => apply(x,IN,OUT)),Tindex)
      case Function(SymbolA(n),l,Ti)  => IN match {
          case Function(SymbolA(ni),li,Ti)
            if n == ni && l.length == li.length && l.zip(li).foldLeft(true,true)((b,pair) => if(equalterms(pair._1,pair._2)&&b._2) b else (b._1,false) )._1  => OUT
          case Function(SymbolA(ni),li,Ti) if n.toList.head == ni.toList.head  => OUT match {
            case SchemaVar(SymbolA(no),Ti) =>  Function(SymbolA(no),li,Ti)
            case _ => Function(SymbolA(ni),li,Ti)
          }
          case _ =>  Function(SymbolA(n),l.map(x => apply(x,IN,OUT)),Ti)
        }
      case SchemaVar(SymbolA(n),t) if t == i | t == ind->i => IN match {
        case SchemaVar(SymbolA(ni),ti) if  t == ti  && ni == n  => OUT
        case _ => SchemaVar(SymbolA(n),t)
      }
      case SchemaConst(SymbolA(n),t)  =>  IN match {
        case SchemaConst(SymbolA(ni),ti) if  t == ti  && ni == n => OUT
        case _ => SchemaConst(SymbolA(n),t)
      }
      case SchemaAbs(x, t)   => SchemaAbs(x, apply(t,IN,OUT))
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + term.toString + "\n")

    }
  }
}


object equalforms{
  def apply(form:SchemaFormula , form2:SchemaFormula):Boolean = {
    form match {
      case Neg(nF) => form2 match {
          case Neg(nF2) => apply(nF,nF2)
          case _ => false
      }
      case And(lF,rF) => form2 match {
        case And(lF2,rF2) => apply(lF,lF2) && apply(rF,rF2)
        case _ => false
      }
      case Or(lF,rF) => form2 match {
        case Or(lF2,rF2) => apply(lF,lF2) && apply(rF,rF2)
        case _ => false
      }
      case Imp(lF,rF) => form2 match {
        case Imp(lF2,rF2) => apply(lF,lF2) && apply(rF,rF2)
        case _ => false
      }
      case AllVar(aV,aF) => form2 match {
        case AllVar(aV2,aF2) if aV.compare(aV2) == 0 => apply(aF,aF2)
        case _ => false
      }
      case ExVar(aV,aF) => form2 match {
        case ExVar(aV2,aF2) if aV.compare(aV2) == 0 => apply(aF,aF2)
        case _ => false
      }
      case Equation(l,r) => form2 match {
        case Equation(l2,r2) => equalterms(l,l2) && equalterms(r,r2)
        case _ => false
     }
      case lessThan(l,r) => form2 match {
        case lessThan(l2,r2) => equalterms(l,l2) && equalterms(r,r2)
        case _ => false
      }
      case sims(l,r) => form2 match {
        case sims(l2,r2) => equalterms(l,l2) && equalterms(r,r2)
        case _ => false
      }
      case leq(l,r) => form2 match {
        case leq(l2,r2) => equalterms(l,l2) && equalterms(r,r2)
        case _ => false
      }
      case Atom(SymbolA(x),y)  => form2 match {
        case  Atom(SymbolA(x2),y2) if x == x2 =>
          y.zip(y2).foldLeft(Pair(true,true))((b,pair) => if(equalterms(pair._1,pair._2)&&b._2) b else Pair(b._1,false))._1
        case _ => false
      }
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + form.toString + "\n")
    }
  }
}

object equalterms{
  def apply(term:SchemaExpression , term2:SchemaExpression):Boolean = {
    term match {
      case Function(SymbolA("schS"),l,Tindex) => term2 match{
        case Function(SymbolA("schS"),l2,Tindex) =>
          l.zip(l2).foldLeft(Pair(true,true))((b,pair) => if(apply(pair._1,pair._2)&&b._2) b else Pair(b._1,false))._1
        case _ => false
      }
      case SchemaVar(SymbolA("k"),Tindex) => term2 match{
        case SchemaVar(SymbolA("k"),Tindex) =>  true
        case _ => false
      }
      case Function(SymbolA(n),l,Tindex) => term2 match{
        case Function(SymbolA(n2),l2,Tindex) if n == n2 && l.length == l2.length =>
          l.zip(l2).foldLeft(Pair(true,true))((b,pair) => if(apply(pair._1,pair._2)&&b._2) b else Pair(b._1,false))._1
        case _ => false
      }
      case Function(SymbolA(n),l,Ti)  => term2 match {
        case Function(SymbolA(n2),l2,Ti) if n == n2 && l.length == l2.length =>
          l.zip(l2).foldLeft(Pair(true,true))((b,pair) => if(apply(pair._1,pair._2)&&b._2) b else Pair(b._1,false))._1
        case _ =>  false
      }
      case SchemaVar(SymbolA(n),->(Tindex,Ti))  => term2 match {
        case SchemaVar(SymbolA(n2),->(Tindex,Ti)) if n2 == n  => true
        case _ => false
      }
      case SchemaVar(SymbolA(n),Ti)  => term2 match {
        case SchemaVar(SymbolA(n2),Ti) if  n2 == n  => true
        case _ => false
      }
      case SchemaConst(SymbolA(n),t)  =>  term2 match {
        case SchemaConst(SymbolA(n2),t2) if  t == t2  && n2 == n => true
        case _ => false
      }
      case SchemaAbs(x, t) => term2 match {case SchemaAbs(x2, t2) if x.compare(x2) == 0 => apply(t,t2)}
      case _ => throw new Exception("ERROR in unfolding missing formula !\n" + term.toString + "\n")

    }
  }
}
object AtomMatch{
  def apply(form:SchemaFormula, fform:SchemaFormula):Boolean = {
    form match {
      case Neg(nF) => apply(nF.asInstanceOf[SchemaFormula],fform)
      case And(lF,rF) => apply(lF,fform) || apply(rF,fform)
      case Or(lF,rF) => apply(lF,fform) || apply(rF,fform)
      case Imp(lF,rF) => apply(lF,fform) || apply(rF,fform)
      case AllVar(aV,aF) => apply(aF,fform)
      case ExVar(aV,aF) => apply(aF,fform)
      case Equation(l,r) => false
      case lessThan(l,r) => false
      case sims(l,r) => false
      case leq(l,r) =>  false
      case Atom(x,y) => fform match{
        case Atom(xx,yy) if xx == x && yy.length == y.length && isIndexSort(y.head) && isIndexSort(yy.head) =>
          y.zip(yy).foldLeft(Pair(true,true))((b,pair) => if(equalterms(pair._1,pair._2)&&b._2) b else Pair(b._1,false))._1        case _ => false
      }
      case Atom(x,y) => false
      case _ => false
    }
  }
  def apply(form:SchemaFormula):Boolean = {
    form match {
      case Atom(x,y) => isIndexSort(y.head)
      case _ =>  false
    }
  }
}
object isIndexSort{
  def apply(term:SchemaExpression):Boolean = {
    term match {
      case Function(SymbolA("schS"),l,Tindex) => apply(l.head)
      case SchemaVar(SymbolA("k"),Tindex) =>  true
      case SchemaConst(SymbolA("0"), Tindex) =>  true
      case _ => false

    }
  }
}



