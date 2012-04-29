# thingy

Data abstraction of _things_ in Clojure.

At times `defrecord` may be too permissive and `deftype` may be too low-level.
_Thingy_ lets you create data abstraction with specified permissions.

TODO: Specify as dependency in `project.clj`:

```clojure
:dependencies [[thingy "0.1.0"]]  ; works w/Clojure 1.2.1, 1.3.0 and 1.4.0
```


## Usage

### Namespace

You can include _thingy_ in your namespace thusly:

```clojure
(ns myapp.domain
  (:use [thingy.core :only (defthing unsupported-throws-exception)]))
```

### Description

`defthing` is a macro that by default creates a type, which

1. converts the data to a plain map when you attempt to add or remove a field

2. retains data as a _thing_ when updating a field value

3. throws exception when you try to read a non-existent field

```clojure
(defthing Person [name gender])
(def a (Person. "Joe Mathew" :male))
(assoc a :location "SFO")  ; returns a map, which is no more a Person
=> {:name "Joe Mathew" :gender :male :location "SFO"}
(dissoc a :gender)  ; again, returns a map that is no more a Person
=> {:name "Joe Mathew"}
(get a :foo)  ; missing key :foo, will throw exception
```

### Custom configuration

You can make the permissions more aggressive by specifying options:

```clojure
(defthing VerifiedPerson [name gender checksum]
  {:field-add    unsupported-throws-exception
   :field-remove unsupported-throws-exception
   :field-update unsupported-throws-exception
   :get-default  (constantly :missing)})
(def p (VerifiedPerson. "Joe Mathew" :male "94a7357754df6db8710074c8ff47b4b9"))
(assoc  p :location "SFO")     ; adding field, throws exception
(dissoc p :gender)             ; removing field, throws exception
(assoc  p :name "Joe Walker")  ; updating field, throws exception
(:foo p)  ; returns :missing when no default is specified
```

You may choose to specify only the options you wish to override.


## Getting in touch

On Twitter: [@kumarshantanu](http://twitter.com/kumarshantanu)

Via email: kumar(dot)shantanu(at)gmail(dot)com


## License

Copyright © 2012 Shantanu Kumar

Distributed under the Eclipse Public License, the same as Clojure.
