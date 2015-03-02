package org.mashupbots.socko.rest.put

import org.mashupbots.socko.rest.BodyParam
import org.mashupbots.socko.rest.Method
import org.mashupbots.socko.rest.PathParam
import org.mashupbots.socko.rest.RestRegistration
import org.mashupbots.socko.rest.RestRequest
import org.mashupbots.socko.rest.RestRequestContext
import org.mashupbots.socko.rest.RestResponse
import org.mashupbots.socko.rest.RestResponseContext

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object PutVoidRegistration extends RestRegistration {
  val method = Method.PUT
  val path = "/void/{status}"
  val requestParams = Seq(PathParam("status"), BodyParam("pet"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[PutVoidProcessor])
}

case class PutVoidRequest(context: RestRequestContext, status: Int, pet: Pet) extends RestRequest

case class PutVoidResponse(context: RestResponseContext) extends RestResponse

class PutVoidProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PutVoidRequest =>
      sender ! PutVoidResponse(req.context.responseContext(req.status))
      context.stop(self)
  }
}

case class Pet(name: String, age: Int)

