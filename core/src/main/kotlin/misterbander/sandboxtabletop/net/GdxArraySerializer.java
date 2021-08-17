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

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class GdxArraySerializer<T extends Array<Object>> extends Serializer<T>
{
	@Override
	public void write(Kryo kryo, Output output, T array)
	{
		output.writeVarInt(array.size, true);
		output.writeBoolean(array.ordered);
		kryo.writeClass(output, array.items.getClass().getComponentType());
		for (Object element : array)
			kryo.writeClassAndObject(output, element);
	}
	
	@SuppressWarnings("unchecked")
	protected T create(boolean ordered, int capacity, Class<?> type)
	{
		return (T)new Array<>(ordered, capacity, type);
	}
	
	@Override
	public T read(Kryo kryo, Input input, Class<? extends T> type)
	{
		int length = input.readVarInt(true);
		boolean ordered = input.readBoolean();
		Class<?> cls = kryo.readClass(input).getType();
		T array = create(ordered, length, cls);
		kryo.reference(array);
		for (int i = 0; i < length; i++)
			array.add(kryo.readClassAndObject(input));
		return array;
	}
	
	@Override
	public T copy(Kryo kryo, T original)
	{
		Class<?> cls = original.items.getClass().getComponentType();
		T copy = create(original.ordered, original.size, cls);
		kryo.reference(copy);
		copy.addAll(original);
		return copy;
	}
}
