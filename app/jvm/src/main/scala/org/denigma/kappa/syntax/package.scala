package org.denigma.kappa

case class Agent()

/**
  * Created by antonkulaga on 11/6/15.
  */
package object syntax {

  implicit class kappaString(str:String){

    def apply(a:Double) = "!!!!!!!"

  }

//  implicit def tuple2Kapp[T](one:T)

}

/*
sealed trait KappaMagnet{
//
//  type Result
//  def directive: Directive1[Result]
}

object KappaMagnet {
//
//  implicit def usernameByToken(getName:String=>Option[String]): OptionalMagnet[String] = new OptionalMagnet[String](getName)
//  implicit def loginInfoByToken(getName:String=>Option[LoginInfo]): OptionalMagnet[LoginInfo] = new OptionalMagnet[LoginInfo](getName)
//  implicit def futureUsernameByToken(getName:String=>Future[String]): FutureMagnet[String] = new FutureMagnet[String](getName)
//  implicit def futureLoginInfoByToken(getName:String=>Future[LoginInfo]): FutureMagnet[LoginInfo] = new FutureMagnet[LoginInfo](getName)

}
*/
