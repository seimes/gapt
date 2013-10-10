
package at.logic.language.lambda

import symbols._

/* 
 * Substitution preserves the following:
 *  1) it is a valid function, i.e. order of elements is irrelevant and each variable is mapped to only one element
 *  2) all mappings are applied simultaneously to a term i.e. {x |-> y, y |-> a}x = y and not a.
 */
class Substitution(val map: Map[Var, LambdaExpression]) {
 
  // Require that each variable is substituted by a term of the same type
  require( map.forall( s => s._1.exptype == s._2.exptype) )

  // Substitution (capture-avoinding)
  // as in http://en.wikipedia.org/wiki/Lambda_calculus#Capture-avoiding_substitutions   
  def apply[T <: LambdaExpression](t: T): T = t match {
    case v : Var if map.contains(v) => map(v).asInstanceOf[T]
    case v : Var if !map.contains(v) => v.asInstanceOf[T]
    case Cons(_, _) => t
    case App(t1, t2) => //App(apply(t1), apply(t2))
      t.factory.createApp( apply(t1), apply(t2) ).asInstanceOf[T]
    case Abs(v, t1) =>
      val fv = range
      val dom = domain
      if (domain.contains(v)) {
        // Abs(x, t) [x -> u] = Abs(x, t)
        // The replacement of v is not done, removing it from the substitution and applying to t1
        val newMap = map - v
        val newSub = Substitution(newMap)
        //Abs(v, newSub(t1))
        t.factory.createAbs(v, newSub(t1)).asInstanceOf[T]
      }
      else if (!fv.contains(v)) {
        // No variable capture
        //Abs(v, apply(t1))
        t.factory.createAbs(v, apply(t1)).asInstanceOf[T]
      }
      else {
        // Variable captured, renaming the abstracted variable
        val freshVar = v.rename(fv)
        val sub = Substitution(v, freshVar)
        val newTerm = sub(t1)
        //Abs(freshVar, apply(newTerm))
        t.factory.createAbs(freshVar, apply(newTerm)).asInstanceOf[T]
      }
  }

  def domain : List[Var] = map.keys.toList
  def range : List[Var] = map.foldLeft(List[Var]()) ( (acc, data) => data._2.freeVariables ++ acc )

  def ::(sub: (Var, LambdaExpression)) = new Substitution(map + sub)
  def ::(otherSubstitution: Substitution) = new Substitution(map ++ otherSubstitution.map)

  override def equals(a: Any) = a match {
    case s: Substitution => map.equals(s.map)
    case _ => false
  }

  //an identity function maps all terms to themselves
  def isIdentity = map.filterNot((p : (Var, LambdaExpression)) => p._1 == p._2).isEmpty

  // make sure the overriden keys are of the applying sub
  // note: compose is in function application notation i.e. (sigma compose tau) apply x = sigma(tau(x)) = x.tau.sigma
  def compose(sub: Substitution): Substitution = Substitution(map ++ sub.map.map(x => (x._1, apply(x._2))))

  // TODO ??? used twice in resolution/PCNF.scala
  // like compose but do not apply the first sub to the second not that the sub might not be idempotent
  def simultaneousCompose(sub: Substitution): Substitution = Substitution(map ++ sub.map)

  def isRenaming = map.forall( p => p._2.isInstanceOf[Var] )

  def getTerm(v: Var) = map.get(v) match {
    case Some(t) => t
    case None => throw new Exception("ERROR: No term associated with variable " + v + " in substitution " + this)
  }

}

object Substitution {
  def apply(subs: List[(Var, LambdaExpression)]): Substitution = new Substitution(Map() ++ subs)
  def apply(variable: Var, expression: LambdaExpression): Substitution = new Substitution(Map(variable -> expression))
  def apply(map: Map[Var, LambdaExpression]): Substitution = new Substitution( map )
  def apply() = new Substitution(Map())
}

