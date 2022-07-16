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

class GdxSetSerializer : Serializer<GdxSet<*>>()
{
	override fun write(kryo: Kryo, output: Output, objectSet: GdxSet<*>)
	{
		val length = objectSet.size
		output.writeVarInt(length, true)
		output.writeBoolean(false) // Whether type is written (in case future version of _root_ide_package_.ktx.collections.GdxSet supports type awareness)
		objectSet.forEach { kryo.writeClassAndObject(output, it) }
	}
	
	private fun create(capacity: Int): GdxSet<Any> = GdxSet(capacity)
	
	override fun read(kryo: Kryo, input: Input, type: Class<out GdxSet<*>>): GdxSet<*>
	{
		val length = input.readVarInt(true)
		input.readBoolean() // Currently unused
		val objectSet = create(length)
		kryo.reference(objectSet)
		repeat(length) { objectSet.add(kryo.readClassAndObject(input)) }
		return objectSet
	}
	
	override fun copy(kryo: Kryo, original: GdxSet<*>): GdxSet<*>
	{
		val copy = create(original.size)
		kryo.reference(copy)
		copy.addAll(original)
		return copy
	}
}
