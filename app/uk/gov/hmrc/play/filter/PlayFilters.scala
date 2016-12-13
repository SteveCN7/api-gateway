package uk.gov.hmrc.play.filter

import javax.inject.{Inject, Singleton}

import play.api.Environment
import play.api.http.HttpFilters

@Singleton
class PlayFilters @Inject()(environment: Environment, proxyFilter: ProxyFilter) extends HttpFilters {

  override val filters = Seq(proxyFilter)

}
