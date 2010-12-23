/*
 * SubsumptionManager.scala
 *
 * Managers are used to manage a buffer of sequents that a subsumption is tested against.
 * The buffer notifies the manager when elements are added or removed so it can update its
 * internal datastructures.
 */

package at.logic.algorithms.subsumption.managers

import at.logic.utils.patterns.listeners._
import at.logic.algorithms.subsumption._
import at.logic.calculi.lk.base._
import at.logic.utils.ds.{Add, Remove, PublishingBuffer, PublishingBufferEvent}

trait SubsumptionManager {
  val sequents: PublishingBuffer[SequentLike]
  val sbsmpAlg: SubsumptionAlgorithm
  protected def init = sequents.addListener((x:PublishingBufferEvent[SequentLike])=> x.ar match {
      case Add => addSequent(x.elem)
      case Remove => removeSequent(x.elem)
  })

  // checks if s is being subsumed by any of the sequents managed by the manager
  def forwardSubsumption(s: SequentLike): Boolean
  // removes any sequent that is subsumed by s
  def backwardSubsumption(s: SequentLike): Unit

  protected def addSequent(s: SequentLike): Unit
  protected def removeSequent(s: SequentLike): Unit
}