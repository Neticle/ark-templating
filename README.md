[![Build Status](https://travis-ci.org/Neticle/ark-templating.svg?branch=master)](https://travis-ci.org/Neticle/ark-templating)

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

Custom elements are defined using a named `<template>` element placed in the document root, 
let's start with a base template that all other templates will use, name it "x-layout.html":

```xml
<template name="x:layout">
    <html>
        <head>
            <title>{{ = title || 'Untitled' }} - My Website</title>
            <slot name="head-styles"></slot>
            <slot name="head-scripts"></slot>
        </head>
        <body>
            <div id="header">
                <h1>My Website</h1>
                <h2>{{ = title }}</h2>
            </div>
            <div id="content">
                <slot></slot>
            </div>
            <div id="footer">
                <slot name="footer"></slot>
                <p>Copyright (c) 2018 Myself</p>
            </div>
            
            <slot name="body-scripts"></slot>
        </body>
    </html>
</template>
```

Now let's create another template, call it "view-home.html":

```xml
<template name="view:home">
    <x:layout title="Home">
        <p>Hello {{ = userName }}! Welcome to my brand new webpage.</p>
        <p slot="footer">This page was brought to you by ark-templating</p>
        
        <style slot="head-styles" rel="stylesheet" href="/css/base.css" />
        <script slot="body-scripts" type="text/javascript" src="/js/script.js"></script>
    </x:layout>
</template>
```

Now, if we render our `view:home` template, assigning `userName` to `Joe` on our scope, we would yield the
following result:

```xml
<html>
    <head>
        <title>Home - My Website</title>
        <style rel="stylesheet" href="/css/base.css" />
    </head>
    <body>
        <div id="header">
            <h1>My Website</h1>
            <h2>Home</h2>
        </div>
        <div id="content">
            <p>Hello Joe! Welcome to my brand new webpage.</p>
        </div>
        <div id="footer">
            <p>This page was brought to you by ark-templating</p>
            <p>Copyright (c) 2018 Myself</p>
        </div>
        
        <script type="text/javascript" src="/js/script.js"></script>
    </body>
</html>
```

## Slots

As we demonstrated in the previous example, content distribution is made possible using slots. 
Slots define where child nodes of your custom elements show up.

A slot with no name - `<slot></slot>` - is a catch-all slot, any child elements of our custom element that aren't 
assigned to a specific slot will be rendered in place of `<slot></slot>`.

In addition to the basic usage demonstrated, slots can also be propagated through multiple levels of nested templates.

For instance, following our example, if we wanted to add a new type of layout - `x:special-layout` - that was based 
of the existing `x:layout` element, behaved the same but added a few extra elements in it's body, and allowed assigning 
elements to `body-scripts` but not to any other slot, we could do it like this:

```xml
<template name="x:special-layout">
    <x:layout title="{{ title }}">
        <div id="sidebar">
            <slot name="sidebar"></slot>
        </div>
        <div id="main">
            <slot></slot>
        </div>
        
        <slot name="body-scripts" slot="body-scripts"></slot>
    </x:layout>
</template>
```

Now we could use `x:special-layout` in our `view:home` element instead of `x:layout`. The result would still be 
based off the original layout, since `x:special-layout` is wrapped in a `x:layout` element.

However the real purpose of this example was to show slot propragation. As we can see we added 
`<slot name="body-scripts" slot="body-scripts"></slot>` at the end there.

This means that any elements passed to `x:special-layout` in the `body-scripts` slot are automatically passed to the 
`x:layout` slot of the same name. Anything passed as other slots however won't, because we didn't explicitly 
propagated it.

## Inner Templates

There are a few elements that are handled differently when used within a custom element declaration.

### For Each

```xml
<template is="foreach" data="{{ menuEntries }}" as="entry">
    <a href="{{ = entry.key }}">{{ = entry.value }}</a>
</template>
```

The `data` attribute must contain a reference to either an `Iterable` object or an array.

The `as` attribute is optional, defaults to `item`.

You may pass a `loop` attribute, it works similar to the `as` attribute in the sense that it defines the name 
of a variable to be included in the iteration-scope. It will point to an object containing meta-data about the 
loop itself. For instance if you define `loop="meta"` you can then inside the loop reference the object as `meta`.

The meta-object will contain useful information such as `meta.index`, `meta.isFirst`, `meta.isLast`, 
`meta.indexIsOdd`, etc.

You may also include a child element within the `<template>` element assigned to the slot `empty`, to be displayed 
when the provided data-set is empty, like this:

```xml
    <p slot="empty">Nothing to show here!</p>
```

### If Condition

```xml
<template if="{{ foo.active }}">
    <p>Foo is active!</p>
</template>
```

The `if` attribute must contain an expression that resolves to a boolean value.

The `if` template can be incorporated with any other inner-template type. For instance, we can combine it 
with a `foreach` template like this: `<template is="foreach" if="{{ ... }}" ... >`.

Similarly to the foreach's `empty` slot, if-templates will display any childs assigned to the `else` slot in case 
the expression evaluates to false.

## References

You can add references to any objects that are present in the scope when rendering.

You can use the dot notation to access attributes of variables. For instance `foo.bar` resolves the `bar` 
member of the `foo` object contained within the scope.

If `foo` is a Map, the reference is resolved internally by calling `foo.get("bar")`. 

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

## Setup

Ark Templating is available in the maven central repository. Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>pt.neticle.ark</groupId>
    <artifactId>ark-templating</artifactId>
    <version>0.2.0</version>
</dependency>
``` 

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
    "my-custom-element", // name of the element, not name of the file
    
    MainScope.builder()
        .with("message", "Hello world")
        .with("name", "John Doe")
        .withList("tags", "html", "components", "custom", "elements", "rendering")
        .withMap("foo", (foo) -> foo.put("bar", "test"))
        .build(),
    
    (OutputStream)System.out
);
```