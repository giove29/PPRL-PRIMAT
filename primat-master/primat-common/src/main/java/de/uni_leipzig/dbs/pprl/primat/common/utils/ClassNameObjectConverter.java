/*******************************************************************************
 *  Copyright © 2017 - 2022 Leipzig University (Database Research Group)
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under 
 * the License.
 *******************************************************************************/

package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Class for dynamically creating objects with given class name and arguments.
 * 
 * @author mfranke
 *
 */
public final class ClassNameObjectConverter {

	private ClassNameObjectConverter() {
		throw new RuntimeException();
	}

	/**
	 * Creates an object of given class.
	 * 
	 * @param  dir                       Package to search for class name.
	 * @param  className                 Name of the class.
	 * @param  params                    Arguments passed to the constructor.
	 * @return                           An instance of the specified class with
	 *                                   the given arguments.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public static Object getObject(String dir, String className, String... params)
		throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
		ClassNotFoundException, NoSuchMethodException, SecurityException {

		final Class<?> clazz = Class.forName(dir + "." + className);

		final int arguments = params.length;

		if (arguments == 0) {
			return clazz.getConstructor().newInstance();
		}
		else {
			final Constructor<?>[] ctors = clazz.getConstructors();

			for (final Constructor<?> ctor : ctors) {
				final Class<?>[] ctorTypes = ctor.getParameterTypes();
				final int ctorArgs = ctorTypes.length;

				if (ctorArgs != arguments) {
					continue;
				}
				else {
					final Object[] initargs = new Object[ctorTypes.length];

					for (int i = 0; i < initargs.length; i++) {
						final Class<?> paramType = ctorTypes[i];
						initargs[i] = StringToObjectConverter.convert(params[i], paramType);
					}

					try {
						return ctor.newInstance(initargs);
					}
					catch (Exception e) {
						continue;
					}
				}
			}
		}
		throw new InstantiationException();
	}
}