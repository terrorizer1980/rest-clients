package com.nicehash.clients.util.options;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A strongly-typed option to configure an aspect of a service or connection.  Options are immutable and use identity comparisons
 * and hash codes.  Options should always be declared as <code>public static final</code> members in order to support serialization.
 *
 * @param <T> the option value type
 */
public abstract class Option<T> implements Serializable {

    private static final long serialVersionUID = -1564427329140182760L;
    protected static final ErrorMessages msg = ErrorMessages.INSTANCE;

    private final Class<?> declClass;
    private final String name;
    private final boolean required;

    Option(final Class<?> declClass, final String name) {
        this(declClass, name, false);
    }

    Option(final Class<?> declClass, final String name, boolean required) {
        if (declClass == null) {
            throw msg.nullParameter("declClass");
        }
        if (name == null) {
            throw msg.nullParameter("name");
        }
        this.declClass = declClass;
        this.name = name;
        this.required = required;
    }

    /**
     * Is sequence.
     *
     * @return true if sequence, false otherwise
     */
    public abstract boolean isSequence();

    /**
     * Best effort for the type, can be null.
     *
     * @return the type or null if it cannot be determined
     */
    public abstract Class<?> getType();

    /**
     * Create an option with a simple type.  The class object given <b>must</b> represent some immutable type, otherwise
     * unexpected behavior may result.
     *
     * @param declClass the declaring class of the option
     * @param name      the (field) name of this option
     * @param type      the class of the value associated with this option
     * @return the option instance
     */
    public static <T> Option<T> simple(final Class<?> declClass, final String name, final Class<T> type) {
        return simple(declClass, name, type, false);
    }

    /**
     * Create an option with a simple type.  The class object given <b>must</b> represent some immutable type, otherwise
     * unexpected behavior may result.
     *
     * @param declClass the declaring class of the option
     * @param name      the (field) name of this option
     * @param type      the class of the value associated with this option
     * @param required  is the option required
     * @return the option instance
     */
    public static <T> Option<T> simple(final Class<?> declClass, final String name, final Class<T> type, boolean required) {
        return new SingleOption<>(declClass, name, type, required);
    }

    /**
     * Create an option with a sequence type.  The class object given <b>must</b> represent some immutable type, otherwise
     * unexpected behavior may result.
     *
     * @param declClass   the declaring class of the option
     * @param name        the (field) name of this option
     * @param elementType the class of the sequence element value associated with this option
     * @return the option instance
     */
    public static <T> Option<Sequence<T>> sequence(final Class<?> declClass, final String name, final Class<T> elementType) {
        return new SequenceOption<>(declClass, name, elementType);
    }

    /**
     * Create an option with a class type.  The class object given may represent any type.
     *
     * @param declClass the declaring class of the option
     * @param name      the (field) name of this option
     * @param declType  the class object for the type of the class object given
     * @param <T>       the type of the class object given
     * @return the option instance
     */
    public static <T> Option<Class<? extends T>> type(final Class<?> declClass, final String name, final Class<T> declType) {
        return new TypeOption<T>(declClass, name, declType);
    }

    /**
     * Create an option with a sequence-of-types type.  The class object given may represent any type.
     *
     * @param declClass       the declaring class of the option
     * @param name            the (field) name of this option
     * @param elementDeclType the class object for the type of the sequence element class object given
     * @param <T>             the type of the sequence element class object given
     * @return the option instance
     */
    public static <T> Option<Sequence<Class<? extends T>>> typeSequence(final Class<?> declClass, final String name, final Class<T> elementDeclType) {
        return new TypeSequenceOption<T>(declClass, name, elementDeclType);
    }

    /**
     * Get the name of this option.
     *
     * @return the option name
     */
    public String getName() {
        return name;
    }

    /**
     * Is required option.
     *
     * @return true if required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Get a human-readable string representation of this object.
     *
     * @return the string representation
     */
    public String toString() {
        return declClass.getName() + "." + name;
    }

    /**
     * Get an option from a string name, using the given classloader.  If the classloader is {@code null}, the bootstrap
     * classloader will be used.
     *
     * @param name        the option string
     * @param classLoader the class loader
     * @return the option
     * @throws IllegalArgumentException if the given option name is not valid
     */
    public static Option<?> fromString(String name, ClassLoader classLoader) throws IllegalArgumentException {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            throw msg.invalidOptionName(name);
        }
        final String fieldName = name.substring(lastDot + 1);
        final String className = name.substring(0, lastDot);
        final Class<?> clazz;
        try {
            clazz = Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw msg.optionClassNotFound(className, classLoader);
        }
        final Field field;
        try {
            field = clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw msg.noField(fieldName, clazz);
        }
        final int modifiers = field.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            throw msg.fieldNotAccessible(fieldName, clazz);
        }
        if (!Modifier.isStatic(modifiers)) {
            throw msg.fieldNotStatic(fieldName, clazz);
        }
        final Option<?> option;
        try {
            option = (Option<?>) field.get(null);
        } catch (IllegalAccessException e) {
            throw msg.fieldNotAccessible(fieldName, clazz);
        }
        if (option == null) {
            throw msg.invalidNullOption(name);
        }
        return option;
    }

    /**
     * Return the given object as the type of this option.  If the cast could not be completed, an exception is thrown.
     *
     * @param o the object to cast
     * @return the cast object
     * @throws ClassCastException if the object is not of a compatible type
     */
    public abstract T cast(Object o) throws ClassCastException;

    /**
     * Return the given object as the type of this option.  If the cast could not be completed, an exception is thrown.
     *
     * @param o          the object to cast
     * @param defaultVal the value to return if {@code o} is {@code null}
     * @return the cast object
     * @throws ClassCastException if the object is not of a compatible type
     */
    public final T cast(Object o, T defaultVal) throws ClassCastException {
        return o == null ? defaultVal : cast(o);
    }

    /**
     * Parse a string value for this option.
     *
     * @param string      the string
     * @param classLoader the class loader to use to parse the value
     * @return the parsed value
     * @throws IllegalArgumentException if the argument could not be parsed
     */
    public abstract T parseValue(String string, ClassLoader classLoader) throws IllegalArgumentException;

    /**
     * Resolve this instance for serialization.
     *
     * @return the resolved object
     * @throws java.io.ObjectStreamException if the object could not be resolved
     */
    protected final Object readResolve() throws ObjectStreamException {
        try {
            final Field field = declClass.getField(name);
            final int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers)) {
                throw new InvalidObjectException("Invalid Option instance (the field is not public)");
            }
            if (!Modifier.isStatic(modifiers)) {
                throw new InvalidObjectException("Invalid Option instance (the field is not static)");
            }
            final Option<?> option = (Option<?>) field.get(null);
            if (option == null) {
                throw new InvalidObjectException("Invalid null Option");
            }
            return option;
        } catch (NoSuchFieldException e) {
            throw new InvalidObjectException("Invalid Option instance (no matching field)");
        } catch (IllegalAccessException e) {
            throw new InvalidObjectException("Invalid Option instance (Illegal access on field get)");
        }
    }

    /**
     * Create a builder for an immutable option set.
     *
     * @return the builder
     */
    public static Option.SetBuilder setBuilder() {
        return new Option.SetBuilder();
    }

    /**
     * A builder for an immutable option set.
     */
    public static class SetBuilder {
        private List<Option<?>> optionSet = new ArrayList<>();

        SetBuilder() {
        }

        /**
         * Add an option to this set.
         *
         * @param option the option to add
         * @return this builder
         */
        public Option.SetBuilder add(Option<?> option) {
            if (option == null) {
                throw msg.nullParameter("option");
            }
            optionSet.add(option);
            return this;
        }

        /**
         * Add options to this set.
         *
         * @param option1 the first option to add
         * @param option2 the second option to add
         * @return this builder
         */
        public Option.SetBuilder add(Option<?> option1, Option<?> option2) {
            if (option1 == null) {
                throw msg.nullParameter("option1");
            }
            if (option2 == null) {
                throw msg.nullParameter("option2");
            }
            optionSet.add(option1);
            optionSet.add(option2);
            return this;
        }

        /**
         * Add options to this set.
         *
         * @param option1 the first option to add
         * @param option2 the second option to add
         * @param option3 the third option to add
         * @return this builder
         */
        public Option.SetBuilder add(Option<?> option1, Option<?> option2, Option<?> option3) {
            if (option1 == null) {
                throw msg.nullParameter("option1");
            }
            if (option2 == null) {
                throw msg.nullParameter("option2");
            }
            if (option3 == null) {
                throw msg.nullParameter("option3");
            }
            optionSet.add(option1);
            optionSet.add(option2);
            optionSet.add(option3);
            return this;
        }

        /**
         * Add options to this set.
         *
         * @param options the options to add
         * @return this builder
         */
        public Option.SetBuilder add(Option<?>... options) {
            if (options == null) {
                throw msg.nullParameter("options");
            }
            for (Option<?> option : options) {
                add(option);
            }
            return this;
        }

        /**
         * Add all options from a collection to this set.
         *
         * @param options the options to add
         * @return this builder
         */
        public Option.SetBuilder addAll(Collection<Option<?>> options) {
            if (options == null) {
                throw msg.nullParameter("option");
            }
            for (Option<?> option : options) {
                add(option);
            }
            return this;
        }

        /**
         * Create the immutable option set instance.
         *
         * @return the option set
         */
        public Set<Option<?>> create() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(optionSet));
        }
    }

    interface ValueParser<T> {
        T parseValue(String string, ClassLoader classLoader) throws IllegalArgumentException;
    }

    private static final Map<Class<?>, Option.ValueParser<?>> parsers;

    private static final Option.ValueParser<?> noParser = (ValueParser<Object>) (string, classLoader) -> {
        throw msg.noOptionParser();
    };

    static {
        final Map<Class<?>, Option.ValueParser<?>> map = new HashMap<>();
        map.put(Byte.class, (ValueParser<Byte>) (string, classLoader) -> Byte.decode(string.trim()));
        map.put(Short.class, (ValueParser<Short>) (string, classLoader) -> Short.decode(string.trim()));
        map.put(Integer.class, (ValueParser<Integer>) (string, classLoader) -> Integer.decode(string.trim()));
        map.put(Long.class, (ValueParser<Long>) (string, classLoader) -> Long.decode(string.trim()));
        map.put(String.class, (ValueParser<String>) (string, classLoader) -> string.trim());
        map.put(Boolean.class, (ValueParser<Boolean>) (string, classLoader) -> Boolean.valueOf(string.trim()));
        parsers = map;
    }

    static <T> Option.ValueParser<Class<? extends T>> getClassParser(final Class<T> argType) {
        return (string, classLoader) -> {
            try {
                return Class.forName(string, false, classLoader).asSubclass(argType);
            } catch (ClassNotFoundException e) {
                throw msg.classNotFound(string, e);
            } catch (ClassCastException e) {
                throw msg.classNotInstance(string, argType);
            }
        };
    }

    static <T> Option.ValueParser<T> getEnumParser(final Class<T> enumType) {
        return (string, classLoader) -> (T) enumType.cast(Enum.valueOf(enumType.asSubclass(Enum.class), string.trim()));
    }

    @SuppressWarnings("unchecked")
    static <T> Option.ValueParser<T> getParser(final Class<T> argType) {
        if (argType.isEnum()) {
            return getEnumParser(argType);
        } else {
            final Option.ValueParser<?> value = parsers.get(argType);
            return (Option.ValueParser<T>) (value == null ? noParser : value);
        }
    }
}

