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

import com.badlogic.gdx.utils.ObjectSet;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class GdxSetSerializer<T extends ObjectSet<Object>> extends Serializer<T>
{
	@Override
	public void write(Kryo kryo, Output output, T objectSet)
	{
		int length = objectSet.size;
		output.writeVarInt(length, true);
		output.writeBoolean(false); // whether type is written (in case future version of ObjectSet supports type awareness)
		for (Object element : objectSet)
			kryo.writeClassAndObject(output, element);
	}
	
	@SuppressWarnings("unchecked")
	protected T create(int capacity)
	{
		return (T)new ObjectSet<>(capacity);
	}
	
	@Override
	public T read(Kryo kryo, Input input, Class<? extends T> type)
	{
		int length = input.readVarInt(true);
		input.readBoolean(); // currently unused
		T objectSet = create(length);
		
		kryo.reference(objectSet);
		
		for (int i = 0; i < length; i++)
			objectSet.add(kryo.readClassAndObject(input));
		return objectSet;
	}
	
	@Override
	public T copy(Kryo kryo, T original)
	{
		T copy = create(original.size);
		kryo.reference(copy);
		copy.addAll(original);
		return copy;
	}
}
