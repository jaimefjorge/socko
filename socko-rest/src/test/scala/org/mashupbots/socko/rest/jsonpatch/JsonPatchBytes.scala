package org.mashupbots.socko.rest.jsonpatch

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

object JsonPatchBytesRegistration extends RestRegistration {
    val method = Method.PATCH
    val path = "/bytes/{status}"
    val requestParams = Seq(PathParam("status"), BodyParam("bytes"))
    def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
        actorSystem.actorOf(Props[JsonPatchBytesProcessor])
}

case class JsonPatchBytesRequest(context: RestRequestContext, status: Int, bytes: Seq[Byte]) extends RestRequest

case class JsonPatchBytesResponse(context: RestResponseContext, data: Array[Byte]) extends RestResponse

class JsonPatchBytesProcessor() extends Actor with akka.actor.ActorLogging {
    def receive = {
        case req: JsonPatchBytesRequest =>
            if (req.status == 200) {
                sender ! JsonPatchBytesResponse(req.context.responseContext(req.status, Map.empty), req.bytes.toArray)
            } else {
                sender ! JsonPatchBytesResponse(req.context.responseContext(req.status), Array.empty)
            }
            context.stop(self)
    }
}
