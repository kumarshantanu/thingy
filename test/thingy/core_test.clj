(ns thingy.core-test
  (:use clojure.test)
  (:use [thingy.core :only (defthing unsupported-throws-exception)]))


(defthing Foo0 [])

(defthing Foo1 [a])

(defthing Foo3 [a b c])

(defthing Foo2Noerr [a b] {:get-default (fn [& args] :default)})

(defthing Foo2Reject [a b] {:field-add    unsupported-throws-exception
                            :field-remove unsupported-throws-exception
                            :field-update unsupported-throws-exception})


(deftest instantiation
  (testing "Can define with zero fields"
    (is (instance? Foo0 (Foo0.)))
    (is (map? (Foo0.))))
  (testing "Can define with one field"
    (is (instance? Foo1 (Foo1. 10)))
    (is (map? (Foo1. 10))))
  (testing "Can define with more than one field"
    (is (instance? Foo3 (Foo3. 10 20 30)))
    (is (map? (Foo3. 10 20 30)))))


(deftest read-field
  (testing "existing fields can be read"
    (let [f (Foo3. 10 20 30)]
      (is (= (.b f) 20))
      (is (= (:b f) 20))
      (is (thrown? NoSuchFieldException (:d f)))
      (is (thrown? IllegalArgumentException (.d ^Foo3 f)))))
  (testing "case: default values can be read via custom provider"
    (let [f (Foo2Noerr. 10 20)]
      (is (= (.b f) 20))
      (is (= (:b f) 20))
      (is (= :default (:d f)))
      (is (thrown? IllegalArgumentException (.d ^Foo3 f))))))


(deftest add-field
  (testing "case: add-field creates map"
    (let [f (Foo0.)
          a (assoc f :a 10)]
      (is (map? a))
      (is (contains? a :a))
      (is (not (instance? Foo0 a)))))
  (testing "case: add-field unsupported"
    (let [f (Foo2Reject. 10 20)]
      (is (thrown? UnsupportedOperationException
                   (assoc f :c 30))))))


(deftest remove-field
  (testing "case: remove-field creates map"
    (let [f (Foo3. 10 20 30)
          r (dissoc f :c)]
      (is (map? r))
      (is (not (contains? r :c)))
      (is (not (instance? Foo3 r)))))
  (testing "case: remove-field unsupported"
    (let [f (Foo2Reject. 10 20)]
      (is (thrown? UnsupportedOperationException
                   (dissoc f :b))))))


(deftest update-field
  (testing "case: update-field creates new instance"
    (let [f (Foo3. 10 20 30)
          u (assoc f :b 21)]
      (is (map? u))
      (is (instance? Foo3 u))
      (is (= (.b ^Foo3 u) 21))))
  (testing "case: update-field unsupported"
    (let [f (Foo2Reject. 10 20)]
      (is (thrown? UnsupportedOperationException
                   (assoc f :b 21))))))


(defthing Person-Details [name service-id gender])

(defthing Person_Data [name service-id gender])

(deftest example-names
  (testing "Person details"
    (let [p (Person-Details. "Shyam Bhushan" 952168 :male)
          q (Person_Data. "Shyam Bhushan" 952168 :male)]
      (is (instance? Person-Details p))
      (is (instance? Person_Data q))
      (is (= :male (:gender p)))
      (is (= :male (:gender q))))))


(defn test-ns-hook
  []
  (instantiation)
  (read-field)
  (add-field)
  (remove-field)
  (update-field)
  (example-names))