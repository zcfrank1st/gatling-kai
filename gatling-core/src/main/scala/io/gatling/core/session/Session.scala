/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.session

import scala.annotation.tailrec
import scala.reflect.ClassTag

import io.gatling.commons.NotNothing
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.commons.util.TypeCaster
import io.gatling.commons.util.TypeHelper._
import io.gatling.commons.validation._
import io.gatling.core.session.el.ElMessages
import io.gatling.core.action.Action

import com.typesafe.scalalogging.LazyLogging

/**
 * Private Gatling Session attributes
 */
object SessionPrivateAttributes {

  val PrivateAttributePrefix = "gatling."
}

case class SessionAttribute(session: Session, key: String) {

  def as[T: NotNothing]: T = session.attributes(key).asInstanceOf[T]
  def asOption[T: TypeCaster: ClassTag: NotNothing]: Option[T] = session.attributes.get(key).flatMap(_.asOption[T])
  def validate[T: TypeCaster: ClassTag: NotNothing]: Validation[T] = session.attributes.get(key) match {
    case Some(value) => value.asValidation[T]
    case None        => ElMessages.undefinedSessionAttribute(key)
  }
}

object Session {
  val MarkAsFailedUpdate: Session => Session = _.markAsFailed
  val Identity: Session => Session = identity[Session]
  val NothingOnExit: Session => Unit = _ => ()
}

/**
 * Session class representing the session passing through a scenario for a given user
 *
 * This session stores all needed data between requests
 *
 * @constructor creates a new session
 * @param scenario the name of the current scenario
 * @param userId the id of the current user
 * @param attributes the map that stores all values needed
 * @param startDate when the user was started
 * @param drift the cumulated time that was spent in Gatling on computation and that wasn't compensated for
 * @param baseStatus the status when not in a TryMax blocks hierarchy
 * @param blockStack the block stack
 * @param onExit hook to execute once the user reaches the exit
 */
case class Session(
    scenario:   String,
    userId:     Long,
    attributes: Map[String, Any] = Map.empty,
    startDate:  Long             = nowMillis,
    drift:      Long             = 0L,
    baseStatus: Status           = OK,
    blockStack: List[Block]      = Nil,
    onExit:     Session => Unit  = Session.NothingOnExit
) extends LazyLogging {

  def apply(name: String) = SessionAttribute(this, name)
  def setAll(newAttributes: (String, Any)*): Session = setAll(newAttributes.toIterable)
  def setAll(newAttributes: Iterable[(String, Any)]): Session = copy(attributes = attributes ++ newAttributes)
  def set(key: String, value: Any) = copy(attributes = attributes + (key -> value))
  def remove(key: String) = if (contains(key)) copy(attributes = attributes - key) else this
  def removeAll(keys: String*) = keys.foldLeft(this)(_ remove _)
  def contains(attributeKey: String) = attributes.contains(attributeKey)
  def reset = copy(attributes = Map.empty)

  private[gatling] def setDrift(drift: Long) = copy(drift = drift)
  private[gatling] def increaseDrift(time: Long) = copy(drift = time + drift)

  private def timestampName(counterName: String) = "timestamp." + counterName

  def loopCounterValue(counterName: String) = attributes(counterName).asInstanceOf[Int]

  def loopTimestampValue(counterName: String) = attributes(timestampName(counterName)).asInstanceOf[Long]

  private[gatling] def enterGroup(groupName: String) = {
    val groupHierarchy = blockStack.collectFirst { case g: GroupBlock => g.hierarchy } match {
      case None    => List(groupName)
      case Some(l) => l :+ groupName
    }
    copy(blockStack = GroupBlock(groupHierarchy) :: blockStack)
  }

  private[gatling] def exitGroup = blockStack match {
    case head :: tail if head.isInstanceOf[GroupBlock] => copy(blockStack = tail)
    case _ =>
      logger.error(s"exitGroup called but stack head $blockStack isn't a GroupBlock, please report.")
      this
  }

  private[gatling] def logGroupRequest(responseTime: Int, status: Status) = blockStack match {
    case Nil => this
    case _ =>
      copy(blockStack = blockStack.map {
        case g: GroupBlock => g.copy(cumulatedResponseTime = g.cumulatedResponseTime + responseTime, status = if (status == KO) KO else g.status)
        case b             => b
      })
  }

  def groupHierarchy: List[String] = {

      @tailrec
      def gh(blocks: List[Block]): List[String] = blocks match {
        case Nil => Nil
        case head :: tail => head match {
          case g: GroupBlock => g.hierarchy
          case _             => gh(tail)
        }
      }

    gh(blockStack)
  }

  private[gatling] def enterTryMax(counterName: String, loopAction: Action) =
    copy(blockStack = TryMaxBlock(counterName, loopAction) :: blockStack).initCounter(counterName, withTimestamp = false)

  private[gatling] def exitTryMax: Session = blockStack match {
    case TryMaxBlock(counterName, _, status) :: tail =>
      copy(blockStack = tail).updateStatus(status).removeCounter(counterName)

    case _ =>
      logger.error(s"exitTryMax called but stack head $blockStack isn't a TryMaxBlock, please report.")
      this
  }

  def isFailed = baseStatus == KO || blockStack.exists {
    case TryMaxBlock(_, _, KO) => true
    case _                     => false
  }

  def status: Status = if (isFailed) KO else OK

  private def failStatusUntilFirstTryMaxBlock: List[Block] = {
    var firstTryMaxBlockNotReached = true
    blockStack.map {
      case tryMaxBlock: TryMaxBlock if firstTryMaxBlockNotReached && tryMaxBlock.status == OK =>
        firstTryMaxBlockNotReached = false
        tryMaxBlock.copy(status = KO)
      case groupBlock: GroupBlock if firstTryMaxBlockNotReached && groupBlock.status == OK =>
        groupBlock.copy(status = KO)
      case b => b
    }
  }

  private def restoreFirstTryMaxBlockStatus: List[Block] = {
    var firstTryMaxBlockNotReached = true
    blockStack.map {
      case tryMaxBlock: TryMaxBlock if firstTryMaxBlockNotReached && tryMaxBlock.status == KO =>
        firstTryMaxBlockNotReached = false
        tryMaxBlock.copy(status = OK)
      case b => b
    }
  }

  private def updateStatus(newStatus: Status): Session =
    if (newStatus == OK) {
      markAsSucceeded
    } else {
      markAsFailed
    }

  private def isWithinTryMax: Boolean = blockStack.exists(_.isInstanceOf[TryMaxBlock])

  def markAsSucceeded: Session =
    if (isWithinTryMax) {
      copy(blockStack = restoreFirstTryMaxBlockStatus)
    } else if (baseStatus == KO) {
      copy(baseStatus = OK)
    } else {
      this
    }

  def markAsFailed: Session =
    if (baseStatus == KO && blockStack.isEmpty) {
      this
    } else {
      val updatedStatus = if (isWithinTryMax) baseStatus else KO
      copy(baseStatus = updatedStatus, blockStack = failStatusUntilFirstTryMaxBlock)
    }

  private[gatling] def enterLoop(counterName: String, condition: Expression[Boolean], exitAction: Action, exitASAP: Boolean, timebased: Boolean): Session = {

    val newBlock =
      if (exitASAP)
        ExitAsapLoopBlock(counterName, condition, exitAction)
      else
        ExitOnCompleteLoopBlock(counterName)

    copy(blockStack = newBlock :: blockStack).initCounter(counterName, withTimestamp = timebased)
  }

  private[gatling] def exitLoop: Session = blockStack match {
    case LoopBlock(counterName) :: tail => copy(blockStack = tail).removeCounter(counterName)
    case _ =>
      logger.error(s"exitLoop called but stack head $blockStack isn't a Loop Block, please report.")
      this
  }

  private[gatling] def initCounter(counterName: String, withTimestamp: Boolean): Session = {
    val withCounter = attributes.updated(counterName, 0)
    val newAttributes =
      if (withTimestamp)
        withCounter.updated(timestampName(counterName), nowMillis)
      else
        withCounter
    copy(attributes = newAttributes)
  }

  private[gatling] def incrementCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case Some(counterValue: Int) => copy(attributes = attributes.updated(counterName, counterValue + 1))
      case _ =>
        logger.error(s"incrementCounter called but attribute for counterName $counterName is missing, please report.")
        this
    }

  private[gatling] def removeCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case Some(counterValue: Int) =>
        copy(attributes = attributes - counterName - timestampName(counterName))
      case _ =>
        logger.error(s"removeCounter called but attribute for counterName $counterName is missing, please report.")
        this
    }

  def update(updates: Iterable[Session => Session]): Session = updates.foldLeft(this) {
    (session, update) => update(session)
  }

  def exit(): Unit = onExit(this)
}
