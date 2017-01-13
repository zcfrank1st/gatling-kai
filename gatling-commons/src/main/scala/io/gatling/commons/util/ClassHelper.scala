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

object ClassHelper {

  def toClassShortName(className: String): String = {
    val sb = new StringBuilder
    val parts = className.split('.')
    var i = 0
    while (i < parts.length - 1) {
      sb.append(parts(i).head).append('.')
      i += 1
    }
    sb.append(parts.last).toString
  }

  implicit class PimpedClass(val clazz: Class[_]) extends AnyVal {
    def getShortName: String = toClassShortName(clazz.getName)
  }
}
