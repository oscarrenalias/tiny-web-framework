package net.renalias.tawebf.core

import java.net.InetSocketAddress
import java.util.concurrent.{Executors}

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.{ ChannelPipeline, ChannelPipelineFactory, Channels }
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

import org.jboss.netty.buffer._
import org.jboss.netty.channel._
import org.jboss.netty.bootstrap._
import org.jboss.netty.channel.Channels.pipeline
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.channel.socket.nio._
import org.jboss.netty.handler.stream._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpHeaders.Values._
import java.util.concurrent.atomic.AtomicLong
import util.Properties
import java.io.{InputStreamReader, BufferedReader}
import scala.actors.Futures._
import net.renalias.tawebf.core.Framework.Request
import java.nio.charset.Charset


object Bootstrap extends Logger {
  def init = {
    // Configure the server.
    val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool))

    val upstreamHandler = new ServerHandler

    // Set up the pipeline factory.
    class DefaultPipelineFactory extends ChannelPipelineFactory {
      def getPipeline = {
        val newPipeline = pipeline()
        newPipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192))
        newPipeline.addLast("encoder", new HttpResponseEncoder())
        newPipeline.addLast("handler", upstreamHandler)
        newPipeline
      }
    }

    bootstrap.setPipelineFactory(new DefaultPipelineFactory)

    // Bind and start to accept incoming connections
    val port = Integer.parseInt(Properties.envOrElse("tawebf.port", "8080"))
    bootstrap.bind(new InetSocketAddress(port))
    log.info("Server started in port %s".format(port))

    future {
      log.info("Press any key to stop")
      val br = new BufferedReader(new InputStreamReader(System.in));
      val sample = br.readLine()
      bootstrap.releaseExternalResources()
    }
  }

  def main(args: Array[String]) = init
}

class ServerHandler extends SimpleChannelUpstreamHandler with Logger {

  val transferredBytes = new AtomicLong

  def getTransferredBytes = transferredBytes.get

  val nettyResponseCodes = Map(
    200 -> HttpResponseStatus.OK,
    404 -> HttpResponseStatus.NOT_FOUND,
    500 -> HttpResponseStatus.INTERNAL_SERVER_ERROR
  )

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case httpRequest: HttpRequest => {
        val uri = httpRequest.getUri
        val method = httpRequest.getMethod
        val body = {
          val cBuffer = httpRequest.getContent()
          val bytes = new Array[Byte](cBuffer.readableBytes())
          cBuffer.readBytes(bytes)
          bytes
        }

        log.info("Received request: URI=%s, method=%s".format(uri, method))

        // map to our own request type
        val request = Request(uri, body.map(_.toChar).mkString)
        // pass it to the framework
        val response = Framework.runApp(sample.Application.app, request)
        // collect the response and send it back
        val httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, nettyResponseCodes(response.status))
        httpResponse.setHeader(HttpHeaders.Names.CONTENT_TYPE, response.contentType)

        httpResponse.setContent(ChannelBuffers.copiedBuffer(response.body, Charset.forName("UTF-8")))
        e.getChannel().write(httpResponse).addListener(new ChannelFutureListener() {
          def operationComplete(future: ChannelFuture) {
            future.getChannel().close()
        }})
      }
      case x => {
        log.debug("Unexpected message type of class = %s".format(x.getClass.getName))
        throw new ClassCastException
      }
    }
  }

  override def exceptionCaught(context: ChannelHandlerContext, e: ExceptionEvent) {
    // Close the connection when an exception is raised.
    log.warn("Unexpected exception from downstream.", Some(e.getCause))
    e.getChannel.close()
  }
}
