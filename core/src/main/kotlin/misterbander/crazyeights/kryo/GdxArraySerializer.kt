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
package misterbander.crazyeights.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import ktx.collections.*

class GdxArraySerializer : Serializer<GdxArray<*>>()
{
	override fun write(kryo: Kryo, output: Output, array: GdxArray<*>)
	{
		output.writeVarInt(array.size, true)
		output.writeBoolean(array.ordered)
		kryo.writeClass(output, array.items.javaClass.componentType)
		array.forEach { kryo.writeClassAndObject(output, it) }
	}
	
	private fun create(ordered: Boolean, capacity: Int, type: Class<*>): GdxArray<Any> =
		GdxArray(ordered, capacity, type)
	
	override fun read(kryo: Kryo, input: Input, type: Class<out GdxArray<*>>): GdxArray<*>
	{
		val length = input.readVarInt(true)
		val ordered = input.readBoolean()
		val cls = kryo.readClass(input).type
		val array = create(ordered, length, cls)
		kryo.reference(array)
		repeat(length) { array.add(kryo.readClassAndObject(input)) }
		return array
	}
	
	override fun copy(kryo: Kryo, original: GdxArray<*>): GdxArray<*>
	{
		val cls = original.items.javaClass.componentType
		val copy = create(original.ordered, original.size, cls)
		kryo.reference(copy)
		val addAll: (GdxArray<*>) -> Unit = copy::addAll
		addAll(original)
		return copy
	}
}
