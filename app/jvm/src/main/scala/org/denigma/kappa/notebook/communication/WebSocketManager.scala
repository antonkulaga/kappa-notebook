package org.denigma.kappa.notebook.communication

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._
import org.denigma.kappa.messages._
import org.denigma.kappa.notebook.communication.SocketMessages._

/**
  * Websocket transport that unplickles/pickles messages
  */
class WebSocketManager(system: ActorSystem) {

  val allRoom = system.actorOf(Props(classOf[RoomActor], "all"))
  val servers = system.actorOf(Props[KappaServerActor])
  def openChannel(channel: String, username: String = "guest"): Flow[Message, Message, Any] = {
    val partial: Graph[FlowShape[Message, Message], ActorRef] = GraphDSL.create(
      Source.actorPublisher[OutgoingMessage](Props(classOf[UserActor], username, servers))
    )
    {
      implicit builder => user =>
      import GraphDSL.Implicits._

        val fromWebsocket: FlowShape[Message, IncomingMessage] = builder.add( Flow[Message].map {case mes => SocketMessages.IncomingMessage(username, channel, mes)  })
        val backToWebsocket: FlowShape[OutgoingMessage, Message] = builder.add(Flow[SocketMessages.OutgoingMessage].map{ case SocketMessages.OutgoingMessage(_, _, message, _) => message })
        val actorAsSource: PortOps[SocketMessages.ChannelMessage] = builder.materializedValue.map{ case actor =>  SocketMessages.UserJoined(username, channel, actor) }
       //send messages to the actor, if send also UserLeft(user) before stream completes.
        val chatActorSink: Sink[ChannelMessage, NotUsed] = Sink.actorRef[SocketMessages.ChannelMessage](allRoom, UserLeft(username, channel))

        val merge: UniformFanInShape[ChannelMessage, ChannelMessage] = builder.add(Merge[SocketMessages.ChannelMessage](2))
        //Message from websocket is converted into IncommingMessage and should be send to each in room
        fromWebsocket ~> merge.in(0)

        //If Source actor is just created should be send as UserJoined and registered as particiant in room
        actorAsSource ~> merge.in(1)

        //Merges both pipes above and forward messages to chatroom Represented by ChatRoomActor
        merge ~> chatActorSink

        user ~> backToWebsocket

      FlowShape( fromWebsocket.in, backToWebsocket.out )
    }.named("socket_flow")
    Flow.fromGraph(partial).recover { case ex =>
      this.system.log.error(s"WS stream for $channel failed for $username with the following cause:\n  $ex")
      throw ex
    }
  }//.via(reportErrorsFlow(channel, username)) // ... then log any processing errors on stdin

}
