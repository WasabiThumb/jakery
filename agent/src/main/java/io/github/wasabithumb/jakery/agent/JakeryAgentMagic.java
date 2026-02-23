/*
 * Copyright 2026 Xavier Pedraza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.wasabithumb.jakery.agent;

import io.github.wasabithumb.jakery.agent.set.ClassSet;
import io.github.wasabithumb.jakery.file.JakeFileApplicable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.lang.classfile.ClassModel;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@NullMarked
@ApiStatus.Internal
final class JakeryAgentMagic {

    private static final Constructor<?> CLASS_SET_IMPL_NEW;
    static {
        Constructor<?> classSetImplNew;
        try {
            classSetImplNew = Class.forName(JakeryAgent.class.getPackageName() + ".set.ClassSetImpl")
                    .getDeclaredConstructor(Stream.class);
            classSetImplNew.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hook into ClassSetImpl constructor", e);
        }
        CLASS_SET_IMPL_NEW = classSetImplNew;
    }

    //

    static ClassSet newClassSet(Stream<ClassModel> src) {
        try {
            return (ClassSet) CLASS_SET_IMPL_NEW.newInstance(src);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            if (t.getCause() instanceof RuntimeException re) throw re;
            throw new IllegalStateException("Failed to invoke ClassSetImpl constructor", t);
        }
    }

    static List<ResolvedGroup> resolveGroups(JakeryAgent instance) {
        List<ResolvedGroup> ret = new ArrayList<>();
        Class<?> type = instance.getClass();

        do {
            Method[] methods = type.getDeclaredMethods();
            for (Method method : methods) {
                Group annotation = method.getAnnotation(Group.class);
                if (annotation == null) continue;
                String groupName = annotation.value();

                if (Modifier.isStatic(method.getModifiers()))
                    throw badGroupMethod(method, "is static");

                if (method.getParameterCount() != 0)
                    throw badGroupMethod(method, "has parameters");

                Object src = executeGroupMethod(instance, method);
                if (!(src instanceof JakeFileApplicable qual))
                    throw badGroupMethod(method, "does not return ClassSet, FieldSet, MethodSet or ConstructorSet");

                ret.add(new ResolvedGroup(groupName, qual));
            }
        } while (
                (type = type.getSuperclass()) != null &&
                JakeryAgent.class.isAssignableFrom(type)
        );

        return Collections.unmodifiableList(ret);
    }

    private static Object executeGroupMethod(JakeryAgent instance, Method method) {
        try {
            return method.invoke(instance);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            if (t.getCause() instanceof RuntimeException re) throw re;
            throw new IllegalStateException("Failed to invoke group method", t);
        }
    }

    @Contract("_, _ -> new")
    private static IllegalStateException badGroupMethod(Method method, String reason) {
        return new IllegalStateException(
                "Group method \"" + method.getName() + "\" in class " +
                method.getDeclaringClass().getName() + " violates contract (" +
                reason + ")"
        );
    }

}
