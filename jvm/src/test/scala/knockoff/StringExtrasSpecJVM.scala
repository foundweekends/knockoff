package knockoff

import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class StringExtrasSpecJVM extends AnyFunSpec with Matchers with StringExtras {

  describe("StringExtras.trimChars(ch)") {
    it("should remove likely headers with the match char inside") {
      "## Who does #2 work for? #".trimChars('#').trim should equal (
        "Who does #2 work for?" )
    }
  }

}
