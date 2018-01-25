package sigmastate.serializer.bytes

import com.google.common.primitives.Bytes
import sigmastate._
import scorex.core.serialization.Serializer
import sigmastate.serializer.bytes.BytesDeserializer.{arrayWithoutKnownSize, shortBytes}
import sigmastate.serializer.bytes.BytesSerializer.{arrayWithKnownSize, shortBytesEnsureCapacity}

import scala.util.Try

abstract class TwoOperandOperationSerializer[LT <: SType, RT <: SType, OPT <: Relation[LT, RT]](opCode: Short, builder: (Value[LT], Value[RT]) => OPT)
                          (implicit leftSerializer: Serializer[Value[LT]],
                           rightSerializer: Serializer[Value[RT]]) extends Serializer[OPT] {
  override def toBytes(lt: OPT): Array[Byte] = {
    Bytes.concat(
      shortBytesEnsureCapacity(opCode),
      arrayWithKnownSize(leftSerializer.toBytes(lt.left)),
      arrayWithKnownSize(rightSerializer.toBytes(lt.right))
    )
  }

  override def parseBytes(bytes: Array[Byte]): Try[OPT] = {
    for {
      (parsedOpCode, bytesAfterOpCode) <- shortBytes(bytes)
      if parsedOpCode == opCode
      (leftBytes, bytesAfterLeft) <- arrayWithoutKnownSize(bytesAfterOpCode)
      left <- leftSerializer.parseBytes(leftBytes)
      (rightBytes, bytesAfterRight) <- arrayWithoutKnownSize(bytesAfterLeft)
      right <- rightSerializer.parseBytes(rightBytes)
    } yield {
      builder(left, right)
    }
  }
}