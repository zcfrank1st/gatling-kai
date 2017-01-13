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

import io.gatling.core.check._
import io.gatling.core.session._
import io.gatling.core.check.extractor.regex._
import io.gatling.http.check.async.AsyncCheckBuilders._

trait AsyncRegexOfType {
  self: AsyncRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor](implicit extractorFactory: OldRegexExtractorFactory) = new AsyncRegexCheckBuilder[X](expression, specializer)
}

object AsyncRegexCheckBuilder {

  def regex(expression: Expression[String], specializer: Specializer[AsyncCheck, String])(implicit extractorFactory: OldRegexExtractorFactory) =
    new AsyncRegexCheckBuilder[String](expression, specializer) with AsyncRegexOfType
}

class AsyncRegexCheckBuilder[X: GroupExtractor](
  private[async] val expression:  Expression[String],
  private[async] val specializer: Specializer[AsyncCheck, String]
)(implicit extractorFactory: OldRegexExtractorFactory)
    extends OldDefaultMultipleFindCheckBuilder[AsyncCheck, String, CharSequence, X](specializer, PassThroughMessagePreparer) {

  import extractorFactory._

  override def findExtractor(occurrence: Int) = expression.map(newSingleExtractor[X](_, occurrence))
  override def findAllExtractor = expression.map(newMultipleExtractor[X])
  override def countExtractor = expression.map(newCountExtractor)
}
