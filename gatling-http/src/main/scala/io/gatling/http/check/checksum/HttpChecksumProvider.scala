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
package io.gatling.http.check.checksum

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.extractor.checksum.{ Md5CheckType, Sha1CheckType }
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

object HttpChecksumProvider {

  val Md5 = new HttpChecksumProvider[Md5CheckType]("MD5")
  val Sha1 = new HttpChecksumProvider[Sha1CheckType]("SHA1")
}

class HttpChecksumProvider[T](algorithm: String) extends CheckProtocolProvider[T, HttpCheck, Response, String] {

  override val specializer: Specializer[HttpCheck, Response] = new ChecksumCheck(algorithm, _)

  override val preparer: Preparer[Response, String] = _.checksum(algorithm) match {
    case Some(chk) => chk.success
    case None      => s"$algorithm checksum wasn't computed".failure
  }
}
