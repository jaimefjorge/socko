package org.mashupbots.socko.rest.jsonpatch

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.mashupbots.socko.rest.{BodyParam, Method, PathParam, RestRegistration, RestRequest, RestRequestContext, RestResponse, RestResponseContext}

object JsonPatchObjectRegistration extends RestRegistration {
    val method = Method.PATCH
    val path = "/object/{status}"
    val requestParams = Seq(PathParam("status"), BodyParam("patch"))
    def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
        actorSystem.actorOf(Props[JsonPatchObjectProcessor])
}

case class JsonPatchObjectRequest(context: RestRequestContext, status: Int, patch: Patch) extends RestRequest
case class PatchEntry(op: String, path: String, value: String)
case class Patch(data: List[PatchEntry])

case class JsonPatchObjectResponse(context: RestResponseContext, patch: Patch) extends RestResponse

class JsonPatchObjectProcessor() extends Actor with akka.actor.ActorLogging {
    def receive = {
        case req: JsonPatchObjectRequest =>
            if (req.status == 200) {
                sender ! JsonPatchObjectResponse(
                    req.context.responseContext(req.status),
                    req.patch)
            } else {
                sender ! JsonPatchObjectResponse(
                    req.context.responseContext(req.status),
                    null)
            }
            context.stop(self)
    }
}
