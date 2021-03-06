/**
 * Auxiliar functions operating on lists
 *
 */

package at.logic.gapt.utils.dssupport

object ListSupport {

  /** Cartesian product of an arbitrary list of lists */
  def product[T]( l: List[List[T]] ): List[List[T]] = l match {
    case Nil    => List( Nil )
    case h :: t => for ( eh <- h; et <- product( t ) ) yield eh :: et
  }

  /** Cartesian product of two lists */
  def product[A, B]( xs: List[A], ys: List[B] ) = {
    xs.flatMap( ( x ) => ys.map( ( y ) => ( x, y ) ) )
  }

  /** all lists obtainable by concatenating one from s1 with one from s2 */
  def times[T]( s1: List[List[T]], s2: List[List[T]] ): List[List[T]] = {
    s1.flatMap( c1 => s2.map( c2 => c1 ++ c2 ) )
  }

  /**
   * Performs a map with an accumulator.
   * Useful for e.g. mapping a custom counter onto a collection.
   *
   * @param f The mapping function. Takes an accumulator and an element from the list and returns a tuple
   *        of the new accumulator value and the mapped list element.
   * @param init The initial accumulator value.
   * @param list The list on which to perform the map.
   * @return The mapped list and the final value of the accumulator.
   */
  def mapAccumL[Acc, X, Y]( f: ( Acc, X ) => ( Acc, Y ), init: Acc, list: List[X] ): ( Acc, List[Y] ) = list match {
    case Nil => ( init, Nil )
    case ( x :: xs ) => {
      val ( new_acc, y ) = f( init, x )
      val ( new_acc2, ys ) = mapAccumL( f, new_acc, xs )

      ( new_acc2, y :: ys )
    }
  }

  /**
   * For each 3rd component which occurs in the list, remove all but the last element
   * with that 3rd component.
   */
  def distinct3rd[T, R]( l: List[Tuple3[String, T, R]] ): List[Tuple3[String, T, R]] = {
    l match {
      case head :: tail =>
        if ( tail.map( tri => tri._3 ).contains( head._3 ) )
          distinct3rd( tail )
        else
          head :: distinct3rd( tail )
      case Nil => Nil
    }
  }

  def removeFirstWhere[A]( s: List[A], prop: A => Boolean ): List[A] = s match {
    case Nil                    => Nil
    case x :: xs if prop( x )   => xs
    case x :: xs /* !prop(x) */ => x :: removeFirstWhere( xs, prop )
  }

  /**
   * Given a list xs, returns a list of copies of xs without the first, second, ..., last element.
   *
   *
   */
  def listComplements[T]( xs: Seq[T] ): Seq[Seq[T]] = xs match {
    case Nil     => Nil
    case y +: ys => ys +: listComplements( ys ).map( zs => y +: zs )
  }

  /**
   * Generates the powerset S as a List of a List, where
   * |S| <= n
   *
   * @param s list
   * @param n upperbound for the powerset
   * @tparam A type of the list
   * @return bounded powerset
   */
  def boundedPower[A]( s: List[A], n: Int ): List[List[A]] = {
    // init powerset
    val powerset = List[List[A]]()

    // function for generating a subset of the powerset of a particular size
    def genLists( l: List[A], i: Int, n: Int ): List[List[A]] = l match {
      // if no elements are left terminate
      case Nil                   => List[List[A]]()
      // if we can still add an element
      // EITHER do not add it and leave i (size of already chosen elements) as it is
      // OR add it and increment i
      case a :: as if i + 1 < n  => genLists( as, i, n ) ++ ( genLists( as, i + 1, n ) map ( a :: _ ) )
      // if we can add just one more element
      // either do so, or not
      case a :: as if i + 1 >= n => List( List( a ) ) ++ genLists( as, i, n )
    }
    // call genLists for 1 <= i <= n times
    // and concatenate all results, s.t. we get the intended result
    ( for ( i <- List.range( 1, n + 1 ) ) yield genLists( s, 0, i ) ).foldLeft( List[List[A]]() )( ( prevLists, l ) => prevLists ++ l )
  }

}

