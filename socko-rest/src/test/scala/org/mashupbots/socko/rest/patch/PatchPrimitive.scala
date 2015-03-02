//
// Copyright 2013 Vibul Imtarnasan, David Bolton and Socko contributors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.mashupbots.socko.rest.patch

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

object PatchPrimitiveRegistration extends RestRegistration {
  val method = Method.PATCH
  val path = "/primitive/{status}"
  val requestParams = Seq(PathParam("status"), BodyParam("double"))
  def processorActor(actorSystem: ActorSystem, request: RestRequest): ActorRef =
    actorSystem.actorOf(Props[PatchPrimitiveProcessor])
}

case class PatchPrimitiveRequest(context: RestRequestContext, status: Int, double: Double) extends RestRequest

case class PatchPrimitiveResponse(context: RestResponseContext, data: Double) extends RestResponse

class PatchPrimitiveProcessor() extends Actor with akka.actor.ActorLogging {
  def receive = {
    case req: PatchPrimitiveRequest =>
      if (req.status == 200) {
        sender ! PatchPrimitiveResponse(
          req.context.responseContext(req.status),
          req.double)
      } else {
        sender ! PatchPrimitiveResponse(
          req.context.responseContext(req.status), 0)
      }
      context.stop(self)
  }
}

