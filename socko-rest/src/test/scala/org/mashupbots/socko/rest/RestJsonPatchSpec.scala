package org.mashupbots.socko.rest

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.apache.http._
import org.apache.http.client.methods._
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.mashupbots.socko.infrastructure.{Logger, WebLogFormat}
import org.mashupbots.socko.routes.{HttpRequest, PathSegments, Routes}
import org.mashupbots.socko.webserver.{HttpConfig, WebLogConfig, WebServer, WebServerConfig}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object RestJsonPatchSpec {
    val cfg =
        """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "INFO"
	  }
        """
}

class RestJsonPatchSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike
with Matchers with BeforeAndAfterAll with TestHttpClient with Logger {

    def this() = this(ActorSystem("HttpJsonPatchSpec", ConfigFactory.parseString(RestPutSpec.cfg)))

    var webServer: WebServer = null
    val port = 9022
    val path = "http://localhost:" + port + "/"

    val restRegistry = RestRegistry("org.mashupbots.socko.rest.jsonpatch",
        RestConfig("1.0", "http://localhost:9022/api", reportRuntimeException = ReportRuntimeException.All))
    val restHandler = system.actorOf(Props(new RestHandler(restRegistry)))

    val routes = Routes({
        case HttpRequest(httpRequest) => httpRequest match {
            case PathSegments("api" :: x) => {
                restHandler ! httpRequest
            }
        }
    })

    override def beforeAll() {
        // Make all content compressible to pass our tests
        val httpConfig = HttpConfig(minCompressibleContentSizeInBytes = 0)
        val webLogConfig = Some(WebLogConfig(None, WebLogFormat.Common))
        val config = WebServerConfig(port = port, webLog = webLogConfig, http = httpConfig)

        webServer = new WebServer(config, routes, system)
        webServer.start()
    }

    override def afterAll() {
        webServer.stop()
    }

    "RestJsonPatchSpec" should {

        // JSON binding doesn't work, need to fix it
        /*"JSON-PATCH object operations" in {
            val httpClient: CloseableHttpClient = HttpClients.createDefault()
            try {
                val httpPatch1: HttpPatch = new HttpPatch(new URI(path + "api/object/200"))
                val jsonPatchString = """[
                                        |    { "op": "replace", "path": "/a/b/c", "value": 42 },
                                        |    { "op": "test", "path": "/a/b/c", "value": "C" }
                                        |]""".stripMargin

                val jsonPatchContentType = ContentType.create("application/json-patch+json", Consts.UTF_8)
                httpPatch1.setEntity(new StringEntity(jsonPatchString, jsonPatchContentType))
                val response1 = execute(httpClient, httpPatch1)
                try {
                    val entity: HttpEntity = response1.getEntity()
                    response1.getStatusLine.getStatusCode should equal(200)
                    EntityUtils.toString(entity, Consts.UTF_8) should be (jsonPatchString)
                    response1.getHeaders(HttpHeaders.CONTENT_TYPE)
                        .head.getValue should be(jsonPatchContentType.toString)
                } finally {
                    response1.close()
                }
            } finally {
                httpClient.close
            }
        }*/

        // send bytes and get back bytes
        "JSON-PATCH bytes operations" in {
            val httpClient: CloseableHttpClient = HttpClients.createDefault()
            try {
                val httpPatch1: HttpPatch = new HttpPatch(new URI(path + "api/bytes/200"))
                val jsonPatchContentType = ContentType.create("application/json-patch+json", Consts.UTF_8)
                val jsonPatchString = """[
                                        |    { "op": "replace", "path": "/a/b/c", "value": 42 },
                                        |    { "op": "test", "path": "/a/b/c", "value": "C" }
                                        |]""".stripMargin
                httpPatch1.setEntity(new StringEntity(jsonPatchString, jsonPatchContentType))
                val response1 = execute(httpClient, httpPatch1)
                try {
                    response1.getStatusLine.getStatusCode should equal(200)
                    val responseString = EntityUtils.toString(response1.getEntity(), Consts.UTF_8)
                    responseString should be(jsonPatchString)
                } finally {
                    response1.close()
                }
            } finally {
                httpClient.close
            }
        }
    }
}

