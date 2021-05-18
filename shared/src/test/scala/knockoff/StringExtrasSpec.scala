package knockoff

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class StringExtrasSpec extends AnyFunSpec with Matchers with StringExtras {

  describe("StringExtras") {
    it( "should find two different groups of the same time" ) {
      "a `foo` b `bar`".nextNIndicesOf(2,"`", None) should equal ( List(2, 6) )
    }

    it( "should deal with only one index" ) {
      "a `foo with nothin'".nextNIndicesOf(2, "`", None) should equal (Nil)
    }

    it("should ignore escaped sequences") {
      val actual = """a ** normal \**escaped ** normal"""
                      .nextNIndicesOf( 2, "**", Some('\\') )
      actual should equal( List(2, 23) )
    }
  }

  describe("StringExtras.countLeading") {
    it("should be ok with nothing to match") {
      "no leading".countLeading('#') should equal (0)
      "".countLeading('#') should equal (0)
    }

    it("should be fine with only these characters") {
      "###".countLeading('#') should equal (3)
    }

    it("should handle only the characters up front") {
      "## unbalanced #".countLeading('#') should equal (2)
    }
  }

  describe("StringExtras.findBalanced") {
    it("should find balanced brackets") {
      val src = "With [embedded [brackets]] [b]."
      val firstSpan = src.indexOf('[')
      src.findBalanced( '[', ']', firstSpan ).get should equal (
        "With [embedded [brackets]".length )
    }
  }
}
