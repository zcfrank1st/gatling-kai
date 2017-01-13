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
package io.gatling.commons.util

import java.nio.file.Path

case class CachingPath(path: Path) {

  private var _fileName: Path = _

  def getFileName: Path = {
    if (_fileName == null) {
      _fileName = path.getFileName
    }
    _fileName
  }

  override def toString: String = path.toString

  def filename: String = getFileName.toString
}
