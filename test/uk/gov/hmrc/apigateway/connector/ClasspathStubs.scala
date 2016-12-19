package uk.gov.hmrc.apigateway.connector

import uk.gov.hmrc.apigateway.exception.GatewayError.NotFound

import scala.io.Source._

trait ClasspathStubs {

  protected def loadStubbedJson(path: String): String = {
    val inputStream = getClass.getResourceAsStream(s"/stub/$path.json")
    Option(inputStream).map(fromInputStream).map(_.mkString).getOrElse(throw NotFound())
  }

}
