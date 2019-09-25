package com.sksamuel.avro4k

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.ElementValueEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.NamedValueEncoder
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.StructureKind
import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.util.Utf8
import java.nio.ByteBuffer

class RecordEncoder(private val schema: Schema,
                    val builder: RecordBuilder) : NamedValueEncoder() {

   override fun encodeTaggedString(tag: String, value: String) {
      val s = schema.getField(tag).schema()
      builder.add(tag, StringToValue.toValue(s, value))
   }

   override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
      println("beginStructure $desc")
      return when (desc.kind) {
         StructureKind.LIST -> {
            val fieldName = popTag()
            val s = schema.getField(fieldName).schema()
            ListEncoder(s) { builder.add(fieldName, it) }
         }
         else -> super.beginStructure(desc, *typeParams)
      }
   }

   override fun endEncode(desc: SerialDescriptor) {
      println("endEncode $desc")
      super.endEncode(desc)
   }

   override fun beginCollection(desc: SerialDescriptor,
                                collectionSize: Int,
                                vararg typeParams: KSerializer<*>): CompositeEncoder {
      println("beginCollection $desc $collectionSize")
      return super.beginCollection(desc, collectionSize, *typeParams)
   }

   override fun encodeTaggedDouble(tag: String, value: Double) {
      builder.add(tag, value)
   }

   override fun encodeTaggedLong(tag: String, value: Long) {
      builder.add(tag, value)
   }

   override fun encodeTaggedByte(tag: String, value: Byte) {
      builder.add(tag, value)
   }

   override fun encodeTaggedBoolean(tag: String, value: Boolean) {
      builder.add(tag, value)
   }

   override fun encodeTaggedShort(tag: String, value: Short) {
      builder.add(tag, value)
   }

   override fun encodeTaggedInt(tag: String, value: Int) {
      builder.add(tag, value)
   }

   override fun encodeTaggedFloat(tag: String, value: Float) {
      builder.add(tag, value)
   }
}

class ListEncoder(private val schema: Schema,
                  private val callback: (GenericData.Array<Any?>) -> Unit) : ElementValueEncoder() {

   private val list = mutableListOf<Any?>()

   init {
      println("Created list encoder $schema")
   }

   override fun endStructure(desc: SerialDescriptor) {
      println("End structure $desc")
      val generic = GenericData.Array(schema, list.toList())
      callback(generic)
   }

   override fun encodeString(value: String) {
      list.add(StringToValue.toValue(schema, value))
   }

   override fun encodeLong(value: Long) {
      list.add(value)
   }

   override fun encodeDouble(value: Double) {
      list.add(value)
   }

   override fun encodeBoolean(value: Boolean) {
      list.add(value)
   }

   override fun encodeShort(value: Short) {
      list.add(value)
   }

   override fun encodeByte(value: Byte) {
      list.add(value)
   }

   override fun encodeFloat(value: Float) {
      list.add(value)
   }

   override fun encodeInt(value: Int) {
      list.add(value)
   }
}

class RecordBuilder(private val schema: Schema) {

   private val map = mutableMapOf<String, Any?>()

   fun add(key: String, value: Any?) {
      map[key] = value
   }

   fun record(): Record {
      return MapRecord(schema, map)
   }
}

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
