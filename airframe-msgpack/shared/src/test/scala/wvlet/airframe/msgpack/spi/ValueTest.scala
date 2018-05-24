/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.msgpack.spi

import java.math.BigInteger

import org.scalatest.prop.PropertyChecks
import wvlet.airframe.AirframeSpec
import wvlet.airframe.msgpack.spi.Value._

/**
  *
  */
class ValueTest extends AirframeSpec with PropertyChecks {
  def checkSuccinctType(pack: WriteCursor => Unit, expectedAtMost: MessageFormat) {

//    val b  = createMessagePackData(pack)
//    val v1 = MessagePack.newDefaultUnpacker(b).unpackValue()
//    val mf = v1.asIntegerValue().mostSuccinctMessageFormat()
//    mf.getValueType shouldBe ValueType.INTEGER
//    mf.ordinal() shouldBe <=(expectedAtMost.ordinal())
//
//    val v2 = new Variable
//    MessagePack.newDefaultUnpacker(b).unpackValue(v2)
//    val mf2 = v2.asIntegerValue().mostSuccinctMessageFormat()
//    mf2.getValueType shouldBe ValueType.INTEGER
//    mf2.ordinal() shouldBe <=(expectedAtMost.ordinal())
  }

  "Value" should {
    "tell most succinct integer type" in {
      forAll { (v: Byte) =>
        checkSuccinctType(Packer.packByte(_, v), MessageFormat.INT8)
      }
      forAll { (v: Short) =>
        checkSuccinctType(Packer.packShort(_, v), MessageFormat.INT16)
      }
      forAll { (v: Int) =>
        checkSuccinctType(Packer.packInt(_, v), MessageFormat.INT32)
      }
      forAll { (v: Long) =>
        checkSuccinctType(Packer.packLong(_, v), MessageFormat.INT64)
      }
      forAll { (v: Long) =>
        checkSuccinctType(Packer.packBigInteger(_, BigInteger.valueOf(v)), MessageFormat.INT64)
      }
      forAll { (v: Long) =>
        whenever(v > 0) {
          // Create value between 2^63-1 < v <= 2^64-1
          checkSuccinctType(Packer.packBigInteger(_, BigInteger.valueOf(Long.MaxValue).add(BigInteger.valueOf(v))), MessageFormat.UINT64)
        }
      }
    }

    import ValueFactory._

    def check(v: Value, expectedType: ValueType, expectedStr: String, expectedJson: String): Unit = {
      v.toString shouldBe expectedStr
      v.toJson shouldBe expectedJson
      v.valueType shouldBe expectedType
    }

    "have nil" in {
      check(newNil, ValueType.NIL, "null", "null")
    }

    "have boolean" in {
      check(newBoolean(true), ValueType.BOOLEAN, "true", "true")
      check(newBoolean(false), ValueType.BOOLEAN, "false", "false")
    }

    "have integer" in {
      check(newInteger(3), ValueType.INTEGER, "3", "3")
      check(newInteger(BigInteger.valueOf(1324134134134L)), ValueType.INTEGER, "1324134134134", "1324134134134")
    }

    "have float" in {
      check(newFloat(0.1), ValueType.FLOAT, "0.1", "0.1")
    }

    "have array" in {
      check(newArray(newInteger(0), newString("hello")), ValueType.ARRAY, "[0,\"hello\"]", "[0,\"hello\"]")
      check(newArray(newArray(newString("Apple"), newFloat(0.2)), newNil), ValueType.ARRAY, """[["Apple",0.2],null]""", """[["Apple",0.2],null]""")
    }

    "have string" in {
      // toString is for extracting string values
      // toJson should quote strings
      check(newString("1"), ValueType.STRING, "1", "\"1\"")
    }

    "have map" in {
      // Map value
      val m = newMap(
        newString("id")      -> newInteger(1001),
        newString("name")    -> newString("leo"),
        newString("address") -> newArray(newString("xxx-xxxx"), newString("yyy-yyyy")),
        newString("name")    -> newString("mitsu")
      )
      val json = """{"id":1001,"name":"mitsu","address":["xxx-xxxx","yyy-yyyy"]}"""
      // TODO check the equality as json objects instead of using direct json string comparison
      check(m, ValueType.MAP, json, json)
    }

    "check appropriate range for integers" in {
      import ValueFactory._
      import java.lang.Byte
      import java.lang.Short

      newInteger(Byte.MAX_VALUE).asByte shouldBe Byte.MAX_VALUE
      newInteger(Byte.MIN_VALUE).asByte shouldBe Byte.MIN_VALUE
      newInteger(Short.MAX_VALUE).asShort shouldBe Short.MAX_VALUE
      newInteger(Short.MIN_VALUE).asShort shouldBe Short.MIN_VALUE
      newInteger(Integer.MAX_VALUE).asInt shouldBe Integer.MAX_VALUE
      newInteger(Integer.MIN_VALUE).asInt shouldBe Integer.MIN_VALUE
      intercept[IntegerOverflowException] {
        newInteger(Byte.MAX_VALUE + 1).asByte
      }
      intercept[IntegerOverflowException] {
        newInteger(Byte.MIN_VALUE - 1).asByte
      }
      intercept[IntegerOverflowException] {
        newInteger(Short.MAX_VALUE + 1).asShort
      }
      intercept[IntegerOverflowException] {
        newInteger(Short.MIN_VALUE - 1).asShort
      }
      intercept[IntegerOverflowException] {
        newInteger(Integer.MAX_VALUE + 1.toLong).asInt
      }
      intercept[IntegerOverflowException] {
        newInteger(Integer.MIN_VALUE - 1.toLong).asInt
      }
    }
  }
}
