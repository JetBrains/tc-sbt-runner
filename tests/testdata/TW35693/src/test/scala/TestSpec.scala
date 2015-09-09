import org.scalatest._
import org.scalatest.FunSpec
import org.slf4j.LoggerFactory

class TestSpec extends FunSpec {


  def logger = LoggerFactory.getLogger(this.getClass)

  describe("Some Test") {


    it("stat warnings") {
      //assume some output starting with WARNING appears in test
      println("WARNING: Invalid stat name /127.0.0.1:4010_backoffs exported as _127_0_0_1_4010_backoffs")
    }

    it("should print warnings") {
      //assume some output starting with WARNING appears in test
      println("WARNING 3/19/14 1:26 PM:liquidbase: modifyDataType will lose primary key/autoincrement/not null settings for mysql")
    }

    it("run something") {
      //assume some custom exception appears in test
      //test should fail, but not compilation error should be presented
      try {
        throw new OurTestException("Invalid stat name /127...")
      } catch {
        case ote: OurTestException => println(ote.getMessage)
        case e: Exception => println(e.getMessage)
      }
    }

    it("log warning") {
      println("- About to log waring!")
      logger.warn("WARNING: Invalid blah-blah-blah")
    }

    it("log error") {
      println("- About to log error!")
      logger.error("[error] some error in test output")
    }

  }


  case class OurTestException(smth: String) extends Exception(smth)
}