# Introduction

Ark Templating is a HTML rendering Java library inspired by the WebComponents/HTML Templates spec. It 
was developed as the default view rendering engine for the [Ark Framework](https://github.com/Neticle/ark) 
but it is fully independent library that can be easily integrated into any Java project. 

Some of the defining key points of this library are:

* No dependencies
* Template sources and syntax are valid XHTML
* Familiar syntax inspired in existing standard

# Syntax

## Basic templates

Custom elements are defined using a named `<template>` element placed in the document root, like this:

```xml
<template name="my-custom-element">
    <div class="custom-element">{{ = message || 'No message' }}</div>
</template>
```

Using this definition we can then use our custom element within another template, like this:

```xml
<template name="my-custom-page">
    <div class="content">
        <my-custom-element message="{{ = myMessage }}"></my-custom-element>
        <my-custom-element message="My hardcoded message"></my-custom-element>
        <my-custom-element></my-custom-element>
    </div>
</template>
```

Rendering an element of type `my-custom-page` with `myMessage` set as `Hello world` would yield the 
following result:

```xml
<div class="content">
    <div class="custom-element">Hello world</div>
    <div class="custom-element">My hardcoded message</div>
    <div class="custom-element">No message</div>
</div>
```

## Slots

Content distribution is made possible using slots. Slots define where child nodes of your custom elements show up.

For instance, we could define our custom element as such:

```xml
<template name="my-custom-element">
    <div class="custom">
        <div class="header">
            <h1>{{ = title }}</h1>
        </div>
        
        <slot></slot>

        <div class="footer">
            <slot name="footer"></slot>
        </div>
    </div>
</template>
```

As you can see we defined two slots, one in the footer named `footer`, and another one with no name. 

The slot with no name is a catch-all slot, any child elements of our custom element that aren't 
assigned to a specific slot will be rendered in place of `<slot></slot>`.

So, that being said, if we were to use our element like this:

```xml
<my-custom-element title="Testing">
    <p>This is going into the body</p>
    <p>And this too</p>
    <p slot="footer">This is the footer text</p>
</my-custom-element>
```

We would obtain the following result:

```xml
<div class="custom">
    <div class="header">
        <h1>Testing</h1>
    </div>
    
    <p>This is going into the body</p>
    <p>And this too</p>
    
    <div class="footer">
        <p>This is the footer text</p>
    </div>
</div>
```

## Inner Templates

There are a few elements that are handled specially when used within a custom element declaration.

### For Each

```xml
<template is="foreach" data="{{ = menuEntries }}" as="entry">
    <a href="{{ = entry.key }}">{{ = entry.value }}</a>
</template>
```

The `data` attribute must contain a reference to either an `Iterable` object or an array of objects.

The `as` attribute is optional, defaults to `item`.

### If Condition

```xml
<template if="{{ = foo.active }}">
    <p>Foo is active!</p>
</template>
```

The `if` attribute must contain an expression that resolves to a boolean value.

The `if` template can be incorporated with any other inner-template type. For instance, we can combine it 
with a `foreach` template like this: `<template is="foreach" if="{{ ... }}" ... >`.

## References

You can add references to any objects that are present in the scope when rendering.

The scope is a container for all referable objects, but you can also access nested members of 
said objects.

For instance `foo.bar` resolves the `bar` member of the `foo` object contained within the scope.

If `foo` is a Map, the reference is resolved by calling `foo.get("bar")`. 

If `foo` is an Object, the reference will be resolved by calling `foo.getBar()`. If `foo` doesn't have such 
method, `null` is resolved.

## Functions

The templating engine allows the definition of custom functions that can be invoked within expressions. A few 
functions already come defined out of the box, such as:

* `String[] Explode (String delimiter, String text)`
* `String Implode (String delimiter, String[] segments)`
* `boolean Empty (Collection||Object[]||String arg0)`
* `boolean NotEmpty (Collection||Object[]||String arg0)`
* `boolean Equals (Object arg0, Object arg1)`
* `boolean NotEquals (Object arg0, Object arg1)`

Functions can be invoked within an expression like this: `{{ = Explode(' ', message) }}`.

# Usage

## Creating a template engine instance with auto-discovery

```java
TemplatingEngine engine = TemplatingEngine.initializer()
    // will recursively find *.html files in the templates directory
    // and parse/register them as usable custom elements.
    .withSearchDirectory(Paths.get("templates"))
    .build();
```

## Manually registering custom elements

```java
engine.registerTemplate(Files.newInputStream(Paths.get("templates","my-custom-element.html")));
```

## Rendering

```java
engine.render
(
    "my-custom-element",
    
    MainScope.builder()
        .with("message", "Hello world")
        .with("name", "John Doe")
        .withList("tags", "html", "components", "custom", "elements", "rendering")
        .withMap("foo", (foo) -> foo.put("bar", "test"))
        .build(),
    
    (OutputStream)System.out
);
```