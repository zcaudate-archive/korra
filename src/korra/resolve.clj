(ns korra.resolve
  (:require [korra.resolve.jar :as jar]
            [korra.common :refer :all]
            [cemerick.pomegranate.aether :as aether])
  (:import [clojure.lang Symbol PersistentVector]))

(defn coordinate-dependencies [coordinates & [repos]]
  (->> (aether/resolve-dependencies
        :coordinates coordinates
        :repositories (merge {"clojars" "http://clojars.org/repo"
                              "central" "http://repo1.maven.org/maven2/"}
                             repos))
       (map #(take 2 (first %)))
       (mapv vec)))

(defn resolve-jar
  ([x] (jar/resolve-jar x nil))
  ([x context & args]
     (cond (keyword? context)
           (apply jar/resolve-jar x context args)

           (string? context)
           (jar/resolve-jar x :jar-path context)

           (instance? ClassLoader context)
           (jar/resolve-jar x :classloader context)

           (vector? context)
           (condp = (type (first context))
             String (jar/resolve-jar x :jar-paths context)
             Symbol (jar/resolve-jar x :coordinate context)
             PersistentVector (jar/resolve-jar x :coordinates context)))))

(defn resolve-coordinates
  [x & more] (if-let [path (-> (apply resolve-jar x more)
                               (first))]
               (maven-coordinate path)))

(defn resolve-with-deps
  ([x] (resolve-with-deps x nil))
  ([x context & {:keys [repositories] :as options}]
     (cond (nil? context)
           (resolve-with-deps x (-> x jar/resolve-jar first))

           (string? context)
           (apply resolve-with-deps x (maven-coordinate context) options)

           (vector? context)
           (condp = (type (first context))
              String
              (apply resolve-with-deps x (map maven-coordinate context) options)

              Symbol (jar/resolve-jar x :coordinates
                                      (coordinate-dependencies [context] repositories))
              PersistentVector
              (jar/resolve-jar x :coordinates
                               (coordinate-dependencies context repositories))))))

(comment
  (resolve-coordinates 'clojure.core)
  (resolve-coordinates 'com.jcraft.jsch.Channel)
  (resolve-jar 'com.jcraft.jsch.Channel)
  (resolve-coordinates 'iroh.core)
  [im.chit/iroh "0.1.11"]
  ;;(resolve-with-deps 'clojure.core "/Users/zhengc/.m2/repository/im/chit/iroh/0.1.6/iroh-0.1.6.jar")
  (-> (resolve-with-deps
       'clojure.core
       '[im.chit/korra "0.1.2"])
      ["/Users/zhengc/.m2/repository/org/clojure/clojure/1.6.0/clojure-1.6.0.jar" "clojure/core.clj"]
      ["/Users/zhengc/.m2/repository/org/clojure/clojure/1.5.1/clojure-1.5.1.jar" "clojure/core.clj"]
      (first) (maven-coordinate))
  [org.clojure/clojure "1.5.1"]

  (resolve-coordinates )

  (jar/resolve-jar 'com.jcraft.jsch.Channel)
  (println (clojure.repl/pst))
  (resolve-with-deps 'clojure.core '[[org.clojure/clojure "1.5.1"] [im.chit/iroh "0.1.6"]])
  (resolve-with-deps 'clojure.core '[org.clojure/clojure "1.5.1"]))
