package org.denigma.kappa.notebook

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.util.ByteString
import boopickle.Default._
import org.denigma.kappa.messages._
import org.denigma.kappa.syntax.{Kappa, KappaResult}


class SocketTransport extends KappaPicklers {

/*
  def webSocketFlow(channel: String, user: String = "guest"): Flow[Message, Message, _] =
  //Factory method allows for materialization of this Source
    Flow.apply(Source.actorRef[KappaMessages.KappaMessage](bufferSize = 10, OverflowStrategy.fail)) {
      implicit builder =>
        source => //it's Source from parameter

          //flow used as input, it takes Messages
          val fromWebsocket = builder.add(
            Flow[Message].collect {
              case BinaryMessage.Strict(data) =>
                //println(s"WE GOT THE MESSAGE INSIDE!!!!!")
                Unpickle[KappaMessages.KappaMessage].fromBytes(data.toByteBuffer)
            })

          //flow used as output, it returns Messages
          val backToWebsocket = builder.add(
            Flow[KappaMessages.KappaMessage].map {
              case mess:KappaMessages.KappaMessage=>
                //println("WE GOT THE MESSAGE BACK!!!!!!!!!!!!!!!\n"+mess)
                val d = Pickle.intoBytes[KappaMessages.KappaMessage](mess)
                BinaryMessage(ByteString(d))
            }
          )

          //send messages to the actor, if sent also UserLeft(user) before stream completes.
          val mainActorSink = Sink.actorRef[KappaMessages.KappaMessage](deviceActor,UserLeft(user))

          //merges both pipes
          val merge = builder.add(Merge[KappaMessages.KappaMessage](2))

          val actorAsSource= builder.materializedValue.map(actor => UserJoined(user,actor))

          //Message from websocket is converted into IncomingMessage
          fromWebsocket ~> merge.in(0)

          //If Source actor is just created, it should be sent as UserJoined
          actorAsSource ~> merge.in(1)

          //Merges both pipes above and forwards messages to main actor
          merge ~> mainActorSink

          //Actor already sits in mainActor so each message from room is used as source and pushed back into the websocket
          source ~> backToWebsocket

          // expose ports
          (fromWebsocket.inlet, backToWebsocket.outlet)
    }.via(reportErrorsFlow[Message](channel,user))*/


  def reportErrorsFlow[T](channel: String, username: String): Flow[T, T, Unit] =
    Flow[T]
      .transform(() ⇒ new PushStage[T, T] {
        def onPush(elem: T, ctx: Context[T]): SyncDirective = ctx.push(elem)

        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]): TerminationDirective = {
          println(s"WS stream for $channel failed for $username with the following cause:\n  $cause")
          super.onUpstreamFailure(cause, ctx)
        }
      })


/*
  def openChannel(channel: String, username: String = "guest"): (Sink[Message,_],Source[Message, _]) = (channel, username) match {
    case (_, _) =>

      Source[Message].from
  /*    Flow[Message].collect {
        case BinaryMessage.Strict(data) =>
          println("IT IS ALIVE!")
          Unpickle[WebMessage].fromBytes(data.toByteBuffer) match {
            case KappaMessages.Discover(_,_)=>
              val disc = Discovered(List.empty)
              val d = Pickle.intoBytes[WebMessage](disc)
              BinaryMessage(ByteString(d))
          }
      }.via(reportErrorsFlow(channel,username)) // ... then log any processing errors on stdin
  */
  }*/

  def openChannel(channel: String, username: String = "guest"): Flow[Message, Message, Unit] = (channel, username) match {
    case (_, _) =>

      Flow[Message].collect {
        case BinaryMessage.Strict(data) =>
          println("IT IS ALIVE!")
          Unpickle[KappaMessages.Message].fromBytes(data.toByteBuffer) match {
            case KappaMessages.Code(code) =>
              val KappaResult(com,lines) = Kappa.run(code.split("\n").toSeq)
              val disc = KappaMessages.Console(com.output.toList)
              val d = Pickle.intoBytes[KappaMessages.Message](disc)
              BinaryMessage(ByteString(d))
          }
      }.via(reportErrorsFlow(channel,username)) // ... then log any processing errors on stdin
  }

}
