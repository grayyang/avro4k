package com.sksamuel.avro4k.encoder

import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.util.Utf8
import java.nio.ByteBuffer

interface ToValue<T> {
   fun toValue(schema: Schema, t: T): Any
}

object StringToValue : ToValue<String> {
   override fun toValue(schema: Schema, t: String): Any {
      return when (schema.type) {
         Schema.Type.FIXED -> {
            val size = t.toByteArray().size
            if (size > schema.fixedSize)
               throw AvroRuntimeException("Cannot write string with $size bytes to fixed type of size ${schema.fixedSize}")
            // the array passed in must be padded to size
            val bytes = ByteBuffer.allocate(schema.fixedSize).put(t.toByteArray()).array()
            GenericData.get().createFixed(null, bytes, schema)
         }
         Schema.Type.BYTES -> ByteBuffer.wrap(t.toByteArray())
         else -> Utf8(t)
      }
   }
}