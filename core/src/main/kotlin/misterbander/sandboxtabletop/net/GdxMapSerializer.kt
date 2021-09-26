/*
 * Copyright 2017 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package misterbander.sandboxtabletop.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import ktx.collections.GdxMap

open class GdxMapSerializer : Serializer<GdxMap<*, *>>()
{
	override fun write(kryo: Kryo, output: Output, map: GdxMap<*, *>)
	{
		val length = map.size
		output.writeVarInt(length, true)
		output.writeBoolean(false) // Whether type is written (in case future version of GdxMap supports type awareness)
		for (objectObjectEntry in map)
		{
			kryo.writeClassAndObject(output, objectObjectEntry.key)
			kryo.writeClassAndObject(output, objectObjectEntry.value)
		}
	}
	
	open fun create(length: Int): GdxMap<Any, Any> = GdxMap(length)
	
	override fun read(kryo: Kryo, input: Input, type: Class<out GdxMap<*, *>>): GdxMap<*, *>
	{
		val length = input.readVarInt(true)
		input.readBoolean() // Currently unused
		val map = create(length)
		kryo.reference(map)
		for (i in 0 until length)
		{
			val key = kryo.readClassAndObject(input)
			val value = kryo.readClassAndObject(input)
			map.put(key, value)
		}
		return map
	}
	
	override fun copy(kryo: Kryo, original: GdxMap<*, *>): GdxMap<*, *>
	{
		val copy = create(original.size)
		kryo.reference(copy)
		copy.putAll(original)
		return copy
	}
}
