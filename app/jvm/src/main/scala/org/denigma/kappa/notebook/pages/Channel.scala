package org.denigma.kappa.notebook.pages

import akka.actor._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._

case class ChatMessage(sender: String, message: String)

class Channel(chatInSink:String=> Sink[ChatEvent, Unit],chatOutSource: Source[ChatMessage, ActorRef], moderator:ActorRef) {
  def chatFlow(sender: String): Flow[String, ChatMessage, Unit] =
    Flow(chatInSink(sender), chatOutSource)(Keep.right) { implicit b ⇒
      (chatActorIn, chatActorOut) ⇒
        import akka.stream.scaladsl.FlowGraph.Implicits._
        val enveloper = b.add(Flow[String].map(ReceivedMessage(sender, _))) // put the message in an envelope
      val merge = b.add(Merge[ChatEvent](2))

        // the main flow
        enveloper ~> merge.in(0)

        // a side branch of the graph that sends the ActorRef of the listening actor
        // to the chatActor
        b.materializedValue ~> Flow[ActorRef].map(NewParticipant(sender, _)) ~> merge.in(1)

        // send the output of the merge to the chatActor
        merge ~> chatActorIn

        (enveloper.inlet, chatActorOut.outlet)
    }.mapMaterializedValue(_ ⇒ ())

  def injectMessage(message: ChatMessage): Unit = moderator ! message // non-streams interface
}

object Channel {
  def create(system: ActorSystem): Channel = {
    // The implementation uses a single actor per chat to collect and distribute
    // chat messages. It would be nicer if this could be built by stream operations
    // directly.
    val moderator =    system.actorOf(Props(new Moderator))

    // Wraps the chatActor in a sink. When the stream to this sink will be completed
    // it sends the `ParticipantLeft` message to the chatActor.
    // FIXME: here some rate-limiting should be applied to prevent single users flooding the chat
    def chatInSink(sender: String): Sink[ChatEvent, Unit] = Sink.actorRef[ChatEvent](moderator, ParticipantLeft(sender))

    // The counter-part which is a source that will create a target ActorRef per
    // materialization where the chatActor will send its messages to.
    // This source will only buffer one element and will fail if the client doesn't read
    // messages fast enough.
    val chatOutSource = Source.actorRef[ChatMessage](1, OverflowStrategy.fail)

    new Channel(chatInSink,chatOutSource, moderator)

  }



}

sealed trait ChatEvent
case class NewParticipant(name: String, subscriber: ActorRef) extends ChatEvent
case class ParticipantLeft(name: String) extends ChatEvent
case class ReceivedMessage(sender: String, message: String) extends ChatEvent {
  def toChatMessage: ChatMessage = ChatMessage(sender, message)
}

class Moderator extends Actor with ActorLogging
{
  var subscribers = Set.empty[ActorRef]

  def receive: Receive = {
    case NewParticipant(name, subscriber) ⇒
      context.watch(subscriber)
      subscribers += subscriber
      sendAdminMessage(s"$name joined!")
    case msg: ReceivedMessage    ⇒ dispatch(msg.toChatMessage)
    case ParticipantLeft(person) ⇒ sendAdminMessage(s"$person left!")
    case Terminated(sub)         ⇒ subscribers -= sub // clean up dead subscribers
  }
  def sendAdminMessage(msg: String): Unit = dispatch(ChatMessage("admin", msg))
  def dispatch(msg: ChatMessage): Unit = subscribers.foreach(_ ! msg)
}
