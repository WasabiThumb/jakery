# Jakery
Gradle-powered toolbox for "pre locating" groups of Java code elements at
compile time, meant to replace many of the use cases of the
now-abandoned [Reflections](https://github.com/ronmamo/reflections) library.

Descriptors resolved by Jakery are stored in a compact form within
``META-INF/.jakery`` via ``jakery-agent``, then lazily read back and
resolved by ``jakery-runtime``. Correct usage of this system may be much
faster and more reliable than the classical approach.

> [!IMPORTANT]
> Jakery's tooling requires a Java 25 build environment (Gradle 9.1.0+)
> due to its use of the [Class-File API](https://openjdk.org/jeps/484), 
> however all runtime components target Java 8. Using Jakery will not prevent 
> you from targeting older JREs.

## Getting Started
Adding the Gradle plugin will automatically create the ``jakery`` source set
to hold your ``JakeryAgent`` implementation and related code. This code will be
responsible for declaring the groups used at runtime.

### Kotlin DSL
```kotlin
plugins {
    id("java")
    id("io.github.wasabithumb.jakery-gradle") version "0.1.0"
}

repositories {
    mavenCentral()
}
```

### Groovy DSL
```groovy
plugins {
    id 'java'
    id 'io.github.wasabithumb.jakery-gradle' version '0.1.0'
}

repositories {
    mavenCentral()
}
```

> [!NOTE]
> By default, this plugin configures the ``main`` source set to depend on
> ``io.github.wasabithumb:jakery-runtime`` and the ``jakery`` source
> set to depend on ``io.github.wasabithumb:jakery-agent``. If
> a repository such as ``mavenCentral()`` which holds these artifacts is not
> declared, these dependencies will fail to resolve.

## Example
Take an interface ``zoo.Animal`` and several classes
within ``zoo.impl`` which implement this interface. 
Here's how you might use Jakery to
list all of these implementation classes at runtime:

### ``src/jakery/java/zoo/ZooJakeryAgent.java``
```java
package zoo;

import io.github.wasabithumb.jakery.agent.JakeryAgent;
import io.github.wasabithumb.jakery.agent.set.ClassSet;
import java.lang.reflect.Modifier;

public final class ZooJakeryAgent extends JakeryAgent {
    
    @Group("animals")
    public ClassSet findAnimals() {
        return find("zoo.impl")
                .withSupertype(Animal.class)
                .withoutModifiers(Modifier.INTERFACE | Modifier.ABSTRACT);
    }
    
}
```

### ``src/main/java/zoo/Animals.java``
```java
package zoo;

public final class Animals {
    
    static Set<Class<?>> implClasses() {
        return Jakery.jakery().typeGroup("animals");
    }
    
}
```

> [!TIP]
> The ``jakery`` source set inherits the classpath of the ``main`` source set.
> You can leverage this to define a ``GroupNames`` class within ``main`` enumerating
> the names of groups defined by your agent that is visible for use in both the 
> ``@Group`` annotation and the ``Jakery`` runtime group accessors.

> [!NOTE]
> ``@Group`` methods may return ``ClassSet``, ``FieldSet``, ``MethodSet`` or ``ConstructorSet``.
> These are accessed at runtime via ``typeGroup``, ``fieldGroup``, ``methodGroup`` and ``constructorGroup`` respectively.
> In the future, it may be possible to declare type groups containing primitive or array types.

## Roadmap
- [ ] Support relocation via Shadow
- [ ] Negotiate behavior when multiple agent classes are present
- [ ] Improve documentation

## License
```text
Copyright 2026 Xavier Pedraza

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
