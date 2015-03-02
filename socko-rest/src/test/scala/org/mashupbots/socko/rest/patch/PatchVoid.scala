package org.mashupbots.socko.rest.patch

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.mashupbots.socko.rest.{BodyParam, Method, PathParam, RestRegistration, RestRequest, RestRequestContext, RestResponse, RestResponseContext}

object PatchVoidRegistration extends RestRegistration {
    val method = Method.PATCH
    val path = "/void/{status}"
    val requestParams = Seq(PathParam("status"), BodyParam("pet"))
    def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
        actorSystem.actorOf(Props[PatchVoidProcessor])
}

case class PatchVoidRequest(context: RestRequestContext, status: Int, pet: Pet) extends RestRequest

case class PatchVoidResponse(context: RestResponseContext) extends RestResponse

class PatchVoidProcessor() extends Actor with akka.actor.ActorLogging {
    def receive = {
        case req: PatchVoidRequest =>
            sender ! PatchVoidResponse(req.context.responseContext(req.status))
            context.stop(self)
    }
}

case class Pet(name: String, age: Int)
