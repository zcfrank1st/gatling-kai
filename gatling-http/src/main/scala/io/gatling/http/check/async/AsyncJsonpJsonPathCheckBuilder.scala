/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.async

import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.check.{ OldDefaultMultipleFindCheckBuilder, Preparer, Specializer }
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.Expression
import io.gatling.http.check.body.HttpBodyJsonpJsonPathProvider

trait AsyncJsonpJsonPathOfType {
  self: AsyncJsonpJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter](implicit extractorFactory: OldJsonPathExtractorFactory) =
    new AsyncJsonpJsonPathCheckBuilder[X](path, specializer, jsonParsers)
}

object AsyncJsonpJsonPathCheckBuilder {

  def asyncJsonpPreparer(jsonParsers: JsonParsers): Preparer[String, Any] = HttpBodyJsonpJsonPathProvider.parseJsonpString(_, jsonParsers)

  def jsonpJsonPath(path: Expression[String], specializer: Specializer[AsyncCheck, String])(implicit extractorFactory: OldJsonPathExtractorFactory, jsonParsers: JsonParsers) =
    new AsyncJsonpJsonPathCheckBuilder[String](path, specializer, jsonParsers) with AsyncJsonpJsonPathOfType
}

class AsyncJsonpJsonPathCheckBuilder[X: JsonFilter](
  private[async] val path:        Expression[String],
  private[async] val specializer: Specializer[AsyncCheck, String],
  private[async] val jsonParsers: JsonParsers
)(implicit extractorFactory: OldJsonPathExtractorFactory)
    extends OldDefaultMultipleFindCheckBuilder[AsyncCheck, String, Any, X](specializer, AsyncJsonpJsonPathCheckBuilder.asyncJsonpPreparer(jsonParsers)) {

  import extractorFactory._

  override def findExtractor(occurrence: Int) = path.map(newSingleExtractor[X](_, occurrence))
  override def findAllExtractor = path.map(newMultipleExtractor[X])
  override def countExtractor = path.map(newCountExtractor)
}
