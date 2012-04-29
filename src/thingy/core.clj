(ns thingy.core
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))


(set! *warn-on-reflection* true)


(defn P
  "Print for debugging."
  [& args]
  ;(apply println args)
  ;(flush)
  nil)


(defn echo
  "Print x and return it. Useful for debugging."
  [x]
  (P x)
  x)


(defn newinstance*
  [the-ns the-name args] {:pre [(not (nil? the-ns))
                                (symbol? the-name)
                                (coll? args)]}
  (P "newinstance [the-ns the-name args]" the-ns the-name args)
  (let [the-meta  (meta the-name)
        full-name (-> (str the-ns "." the-name)
                      symbol
                      (with-meta the-meta))]
    (eval `(new ~full-name ~@args))))


(defmacro newinstance
  ([the-name args]
     `(newinstance* *ns* ~the-name ~args))
  ([the-ns the-name args]
     `(newinstance* ~the-ns ~the-name ~args)))


(defprotocol IMappable
  (asMap [this] "Return a map representation"))


(defn field-add-creates-map
  [thing k v]
  (P "field-add-creates-map [k v]" k v)
  (assoc (.asMap ^thingy.core.IMappable thing) k v))


(defn field-remove-creates-map
  [thing k]
  (P "field-remove-creates-map [k]" k)
  (dissoc (.asMap ^thingy.core.IMappable thing) k))


(defn field-update-creates-new
  [key-vec thing k v]
  (P "field-update-creates-new" k v)
  (let [values (vec (map #(if (= % k) v (get thing %)) key-vec))
        the-ns (let [full (-> thing class .getName)]
                 (subs full 0 (.lastIndexOf full ".")))
        cl-sym (-> thing class .getSimpleName symbol)]
    (newinstance the-ns cl-sym values)))


(defn get-default-rejects-missing
  [key-set thing k]
  (P "get-default-rejects-missing [k]" k)
  ;; (if (key-set k) (.valAt ^clojure.lang.ILookup thing k)
  ;;     (throw (NoSuchFieldException. (str "No such key " k))))
  (throw (NoSuchFieldException. (str "No such key " k)))
  )


(defn unsupported-throws-exception
  [& args]
  (throw (UnsupportedOperationException. "Not supported")))


(defmacro defthing
  "Define a 'thing' having fields of data. This is same as defrecord, except you
  may pass options to control the behavior of the operations. Notice that
  __meta and __extmap are still reserved field names that you may not use."
  ([the-name fields]
     `(defthing ~the-name ~fields {}))
  ([the-name fields opts & specs]
     (assert (symbol? the-name))
     (assert (vector? fields))
     (assert (every? symbol? fields))
     (assert (map? opts))
     (let [full-name (-> (str *ns*)
                         (str/replace #"-" "_")
                         (str "." the-name)
                         symbol)
           key-vec (vec (map keyword fields))
           key-pos (zipmap key-vec (range (count key-vec)))
           key-set (set key-vec)
           field? #(key-set %)
           {:keys [field-add
                   field-remove
                   field-update
                   get-default]
            :or {field-add    `field-add-creates-map
                 field-remove `field-remove-creates-map
                 field-update `(partial field-update-creates-new ~key-vec)
                 get-default  `(partial get-default-rejects-missing ~key-set)}
            :as opts} opts]
       `(let []
          (deftype ~the-name ~fields
            ;;
            IMappable
            (asMap [this#]    (zipmap ~key-vec ~fields))
            java.lang.Object
            (toString [this#] (str "#" *ns* "." '~the-name (.asMap this#)))
            (hashCode [this#] (do (P "hashCode []")
                                  (.hashCode (.toString this#))))
            (equals   [this# that#] (do (println "equals [that]" that#) (flush)
                                        (and (= (class this#) (class that#))
                                             (every? #(= (get this# %)
                                                         (get that# %)) ~key-vec))))
            clojure.lang.Counted
            (count [this#] (do (P "count []")
                               (count ~key-vec)))
            clojure.lang.Associative
            (entryAt [this# k#]  (do (P "entryAt [k]" k#)
                                     (clojure.lang.MapEntry. k# (get this# k#))))
            (assoc [this# k# v#] (do (P "assoc [k v]" k# v#)
                                     (if (~key-set k#)
                                       (~field-update this# k# v#)
                                       (~field-add this# k# v#))))
            clojure.lang.ILookup
            (valAt [this# k#]    (do (P "valAt [k]" k#)
                                     (if (~key-set k#)
                                       (.valAt this# k# nil)
                                       (~get-default this# k#))))
            (valAt [this# k# d#] (do (P "valAt [k d]" k# d#)
                                     (get ~fields (~key-pos k#) d#)))
            ;;clojure.lang.IMeta
            ;;(meta [this#])
            ;;clojure.lang.IObj
            ;;(withMeta [this# m#] this#)
            clojure.lang.IPersistentCollection
            ;;(count [this])))
            (cons  [this# n#] (do (P "cons [n]" n#)
                                  (cons n# (seq this#))))
            (empty [this#]    (do (P "empty []")
                                  (throw (UnsupportedOperationException.
                                          (str "empty not supported on " '~the-name)))))
            (equiv [this# n#] (do (P "equiv [n]" n#)
                                  (and (instance? ~the-name n#)
                                       (every? #(= (get this# %)
                                                   (get n# %)) ~key-vec))))
            clojure.lang.IPersistentMap
            ;;(assoc   [this k v])
            (assocEx [this# k# v#] (do (P "assocEx [k v]" k# v#)
                                       (.assoc this# k# v#)))
            (without [this# k#]    (do (P "without [k]" k#)
                                       (~field-remove this# k#)))
            ;; clojure.lang.IRecord  ; Not in Clojure 1.2
            clojure.lang.Seqable
            (seq [this#]           (do (P "seq []")
                                       (if (empty? ~key-vec) nil
                                           (seq (map #(.entryAt this# %) ~key-vec)))))
            java.io.Serializable
            java.lang.Iterable
            (iterator [this#] (do (P "iterator []")
                                  (clojure.lang.SeqIterator. (.seq this#))))
            java.util.Map
            (size     [this#]         (.count this#))
            (isEmpty  [this#]         (zero? (.count this#)))
            (containsKey   [this# k#] (~key-set k#))
            (containsValue [this# v#] (boolean (some #{v#} ~fields)))
            (get      [this# k#]      (.valAt this# k#))
            (put      [this# k# v#]   (throw (UnsupportedOperationException.)))
            (remove   [this# k#]      (throw (UnsupportedOperationException.)))
            (putAll   [this# m#]      (throw (UnsupportedOperationException.)))
            (clear    [this#]         (throw (UnsupportedOperationException.)))
            (keySet   [this#]         ~key-set)
            (values   [this#]         ~fields)
            (entrySet [this#]         (set this#))
            ~@specs)
          ;(import '~full-name)
          (defmethod pp/simple-dispatch ~full-name [x#]
            (pp/pprint (.asMap ^thingy.core.IMappable x#)))
          ))))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
