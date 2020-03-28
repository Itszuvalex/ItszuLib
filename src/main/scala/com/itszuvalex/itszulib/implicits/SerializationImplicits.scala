package com.itszuvalex.itszulib.implicits

import java.nio.charset.Charset

import io.netty.buffer.ByteBuf

/**
  * Created by Chris on 8/6/2016.
  */
object SerializationImplicits {

  implicit class ByteBufImplicits(byteBuf: ByteBuf) {

    /**
      * Using Int because strings > 2 Gb are a bug in themselves.
      *
      * @param string
      */
    def writeString(string: String): Unit = {
      if (string == null) {
        byteBuf.writeInt(0)
      }
      else {
        byteBuf.writeInt(string.length)
        byteBuf.writeCharSequence(string, Charset.defaultCharset())
      }
    }

    /**
      * Using Int because strings > 2 Gb are a bug in themselves.
      *
      */
    def readString(): String = {
      val stringLength = byteBuf.readInt()
      if (stringLength > 0) {
        val fluidNameBytes = byteBuf.readCharSequence(stringLength, Charset.defaultCharset())
        String.valueOf(fluidNameBytes)
      }
      else {
        null
      }
    }
  }

}
