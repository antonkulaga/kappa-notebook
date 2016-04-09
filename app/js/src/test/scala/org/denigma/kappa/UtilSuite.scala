package org.denigma.kappa

import org.scalatest.{Matchers, WordSpec}
import org.denigma.kappa.notebook.extensions._
import rx._

/**
  * Created by antonkulaga on 09/04/16.
  */
class UtilSuite extends WordSpec with Matchers {

  "Map updates should work fine" should {
    "provide nice diff" in {

      val past = Map(1->"one",2->"two",3->"three", 4->"four",5->"five")
      val present = Map(1->"one", 7->"seven", 3->"trois", 5->"cinq", 6->"six")
      val map = Var(past)
      val updates = map.updates
      map() = present
      val changes = updates.now
      changes.removed shouldEqual Map(2->"two", 4->"four")
      changes.added shouldEqual Map(7->"seven", 6->"six")
      changes.updated shouldEqual Map(5->("five", "cinq"), 3 -> ("three", "trois"))
    }


  }
}
