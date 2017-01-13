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
package io.gatling.core.check.extractor.bytes

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.extractor._
import io.gatling.core.session._

trait BodyBytesCheckType

object BodyBytesCheckBuilder {

  val BodyBytes = {

    val extractor = new Extractor[Array[Byte], Array[Byte]] with SingleArity {
      val name = "bodyBytes"
      def apply(prepared: Array[Byte]) = Some(prepared).success
    }.expressionSuccess

    new DefaultFindCheckBuilder[BodyBytesCheckType, Array[Byte], Array[Byte]](extractor)
  }
}
