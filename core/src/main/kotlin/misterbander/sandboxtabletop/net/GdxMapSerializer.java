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
package misterbander.sandboxtabletop.net;

import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class GdxMapSerializer<T extends ObjectMap<Object, Object>> extends Serializer<T>
{
	public void write(Kryo kryo, Output output, T map)
	{
		int length = map.size;
		output.writeVarInt(length, true);
		output.writeBoolean(false); // whether type is written (in case future version of ObjectMap supports type awareness)
		
		for (T.Entry<?, ?> objectObjectEntry : map)
		{
			kryo.writeClassAndObject(output, objectObjectEntry.key);
			kryo.writeClassAndObject(output, objectObjectEntry.value);
		}
	}
	
	@SuppressWarnings("unchecked")
	public T create(int length)
	{
		return (T)new ObjectMap<>(length);
	}
	
	@Override
	public T read(Kryo kryo, Input input, Class<? extends T> type)
	{
		int length = input.readVarInt(true);
		input.readBoolean(); // currently unused
		T map = create(length);
		
		kryo.reference(map);
		
		for (int i = 0; i < length; i++)
		{
			Object key = kryo.readClassAndObject(input);
			Object value = kryo.readClassAndObject(input);
			map.put(key, value);
		}
		return map;
	}
	
	public T copy(Kryo kryo, T original)
	{
		T copy = create(original.size);
		kryo.reference(copy);
		copy.putAll(original);
		return copy;
	}
}
