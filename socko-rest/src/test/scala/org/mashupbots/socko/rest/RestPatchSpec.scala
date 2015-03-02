package org.mashupbots.socko.rest

import java.net.URI
import java.util.Date

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.apache.http.client.methods._
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpEntity, _}
import org.json4s.NoTypeHints
import org.json4s.native.{Serialization => json}
import org.mashupbots.socko.infrastructure.{Logger, WebLogFormat}
import org.mashupbots.socko.routes.{HttpRequest, PathSegments, Routes}
import org.mashupbots.socko.webserver.{HttpConfig, WebLogConfig, WebServer, WebServerConfig}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object RestPatchSpec {
  val cfg =
    """
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "INFO"
	  }    
    """
}

class RestPatchSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll with TestHttpClient with Logger {

  def this() = this(ActorSystem("HttpPatchSpec", ConfigFactory.parseString(RestPutSpec.cfg)))

  var webServer: WebServer = null
  val port = 9022
  val path = "http://localhost:" + port + "/"

  val restRegistry = RestRegistry("org.mashupbots.socko.rest.patch",
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

  "RestPatchSpec" should {
    // Apache HttpComponents client 4.4
    // https://hc.apache.org/httpcomponents-client-4.4.x/httpclient/examples/org/apache/http/examples/client/QuickStart.java

    "PATCH void operations" in {
      val httpClient: CloseableHttpClient = HttpClients.createDefault()
      try {
        val httpPatch1: HttpPatch = new HttpPatch(new URI(path + "api/void/200"))
        httpPatch1.setEntity(new StringEntity("{\"name\":\"Boo\",\"age\":5}",
          ContentType.APPLICATION_JSON))
        val response1 = execute(httpClient, httpPatch1)
        try {
          val entity: HttpEntity = response1.getEntity()
          EntityUtils.consume(entity)
          response1.getStatusLine.getStatusCode should equal(200)
          response1.getEntity.getContentLength should be(0)
        } finally {
          response1.close()
        }
        val httpPatch2: HttpPatch = new HttpPatch(new URI(path + "api/void/204"))
        httpPatch2.setEntity(new StringEntity("{\"name\":\"Boo\",\"age\":5}",
          ContentType.APPLICATION_JSON))
        val response2 = execute(httpClient, httpPatch2)
        try {
          response2.getStatusLine.getStatusCode should equal(204)
        } finally {
          response2.close()
        }
      } finally {
        httpClient.close
      }
    }

    "PATCH object operations" in {
      val httpClient: CloseableHttpClient = HttpClients.createDefault()
      try {
        val httpPatch1: HttpPatch = new HttpPatch(new URI(path + "api/object/200"))
        httpPatch1.setEntity(new StringEntity("{\"name\":\"Boo\",\"age\":5}",
          ContentType.APPLICATION_JSON))
        val response1 = execute(httpClient, httpPatch1)
        try {
          val entity: HttpEntity = response1.getEntity()
          response1.getStatusLine.getStatusCode should equal(200)
          EntityUtils.toString(entity,
            Consts.UTF_8) should be ("{\"name\":\"Boo\",\"age\":5,\"history\":[]}")
          response1.getHeaders(HttpHeaders.CONTENT_TYPE)
              .head.getValue should be(ContentType.APPLICATION_JSON.toString)
        } finally {
          response1.close()
        }
        val httpPatch2: HttpPatch = new HttpPatch(new URI(path + "api/object/404"))
        httpPatch2.setEntity(new StringEntity("{\"name\":\"Boo\",\"age\":5}",
          ContentType.APPLICATION_JSON))
        val response2 = execute(httpClient, httpPatch2)
        try {
          response2.getStatusLine.getStatusCode should equal(404)
          EntityUtils.toString(response2.getEntity(), Consts.UTF_8).length should be(0)
        } finally {
          response2.close()
        }
        // Nested objects
        val evt1 = org.mashupbots.socko.rest.put.Event(new Date(), "born")
        val evt2 = org.mashupbots.socko.rest.put.Event(new Date(), "Died")
        val data = org.mashupbots.socko.rest.put.Fish("Flounder", 1, List(evt1, evt2))
        implicit val formats = json.formats(NoTypeHints)
        val s = json.write(data)
        val url3 = new URI(path + "api/object/200")
        val httpPatch3: HttpPatch = new HttpPatch(url3)
        httpPatch3.setEntity(new StringEntity(s, ContentType.APPLICATION_JSON))
        val response3 = execute(httpClient, httpPatch3)
        try {
          response3.getStatusLine.getStatusCode should equal(200)
          EntityUtils.toString(response3.getEntity(), Consts.UTF_8) should be(s)
          response3.getHeaders(HttpHeaders.CONTENT_TYPE)
              .head.getValue should be(ContentType.APPLICATION_JSON.toString)
        } finally {
          response3.close
        }

        // Empty content = bad request because object is required
        val httpPatch4: HttpPatch = new HttpPatch(new URI(path + "api/object/204"))
        httpPatch4.setEntity(new StringEntity("", ContentType.APPLICATION_JSON))
        val response4 = execute(httpClient, httpPatch4)
        try {
          response4.getStatusLine.getStatusCode should equal(400)
        } finally {
          response4.close
        }
      } finally {
        httpClient.close
      }
    }

    "PATCH bytes operations" in {
      val httpClient: CloseableHttpClient = HttpClients.createDefault()
      try {
        val httpPatch1: HttpPatch = new HttpPatch(new URI(path + "api/bytes/200"))
        httpPatch1.setEntity(new StringEntity("YeeHaa", ContentType.APPLICATION_JSON))
        val response1 = execute(httpClient, httpPatch1)
        try {
          response1.getStatusLine.getStatusCode should equal(200)
          EntityUtils.toString(response1.getEntity(), Consts.UTF_8) should be("YeeHaa")
        } finally {
          response1.close()
        }

        val httpPatch2: HttpPatch = new HttpPatch(new URI(path + "api/bytes/404"))
        httpPatch2.setEntity(new StringEntity("YeeHaa", ContentType.APPLICATION_JSON))
        val response2 = execute(httpClient, httpPatch2)
        try {
          response2.getStatusLine.getStatusCode should equal(404)
          EntityUtils.toString(response2.getEntity(), Consts.UTF_8).length should be(0)
        } finally {
          response2.close()
        }

        // Empty byte array
        val httpPatch3: HttpPatch = new HttpPatch(new URI(path + "api/bytes/204"))
        httpPatch3.setEntity(new StringEntity("", ContentType.APPLICATION_JSON))
        val response3 = execute(httpClient, httpPatch3)
        try {
          response3.getStatusLine.getStatusCode should equal(204)
        } finally {
          response3.close()
        }
      } finally {
        httpClient.close
      }
    }

    "PATCH primitive operations" in {
      val httpClient: CloseableHttpClient = HttpClients.createDefault()
      val httpPatch1: HttpPatch = new HttpPatch(new URI(path + "api/primitive/200"))
      httpPatch1.setEntity(new StringEntity("1.23", ContentType.APPLICATION_JSON))
      val response1 = execute(httpClient, httpPatch1)
      try {
        response1.getStatusLine.getStatusCode should equal(200)
        EntityUtils.toString(response1.getEntity(), Consts.UTF_8) should be("1.23")
      } finally {
        response1.close()
      }

      val httpPatch2: HttpPatch = new HttpPatch(new URI(path + "api/primitive/404"))
      httpPatch2.setEntity(new StringEntity("1.23", ContentType.APPLICATION_JSON))
      val response2 = execute(httpClient, httpPatch2)
      try {
        response2.getStatusLine.getStatusCode should equal(404)
      } finally {
        response2.close()
      }

      // Empty content = binding error because body is required
      val httpPatch3: HttpPatch = new HttpPatch(new URI(path + "api/primitive/204"))
      httpPatch3.setEntity(new StringEntity("", ContentType.APPLICATION_JSON))
      val response3 = execute(httpClient, httpPatch3)
      try {
        response3.getStatusLine.getStatusCode should equal(400)
      } finally {
        response3.close()
      }
    }

  }
}