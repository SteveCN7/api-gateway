package uk.gov.hmrc.model

case class ApiDefinition(context: String, serviceBaseUrl: String, versions: Seq[ApiVersion])

case class ApiVersion(version: String, endpoints: Seq[ApiEndpoint])

case class ApiEndpoint(uriPattern: String, method: String, scope: Option[String] = None)

case class ApiDefinitionMatch(context: String, serviceBaseUrl: String, apiVersion: String, scope: Option[String])
