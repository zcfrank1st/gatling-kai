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
package io.gatling.recorder.http

import scala.util.{ Failure, Success, Try }

import io.netty.channel.{ Channel, ChannelFuture, ChannelFutureListener }
import io.netty.handler.codec.http._
import org.asynchttpclient.uri.Uri

object Netty {

  type ChannelId = Long

  implicit class PimpedChannelFuture(val cf: ChannelFuture) extends AnyVal {

    def addScalaListener(f: Try[Channel] => Unit): ChannelFuture =
      cf.addListener(new ChannelFutureListener {
        override def operationComplete(future: ChannelFuture): Unit = {
          val outcome =
            if (future.isSuccess) {
              Success(future.channel)
            } else {
              Failure(future.cause)
            }
          f(outcome)
        }
      })
  }

  implicit class PimpedChannel(val channel: Channel) extends AnyVal {

    def id: ChannelId = channel.hashCode

    def reply500AndClose(): Unit =
      channel
        .writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
        .addListener(ChannelFutureListener.CLOSE)
  }

  implicit class PimpedFullHttpRequest(val request: FullHttpRequest) extends AnyVal {

    def makeRelative: FullHttpRequest = {
      val relativeUrl = Uri.create(request.getUri).toRelativeUrl
      val relativeRequest = new DefaultFullHttpRequest(request.getProtocolVersion, request.getMethod, relativeUrl, request.content.retain())
      relativeRequest.headers.add(request.headers)
      relativeRequest
    }
  }
}
