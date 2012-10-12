(ns datomic-simple.core
  (:use datomic-simple.db)
  (:require [datomic.api :as api]
            datomic-simple.model
            [datomic-simple.util :as util]))

(defmacro ^:private add-noir-middleware [uri]
  `(noir.server/add-middleware wrap-datomic ~uri))

(defn rand-connection []
  (str "datomic:mem://" (java.util.UUID/randomUUID)))

(defn start [{:keys [uri schemas seed-data repl]}]
  (let [uri (or uri (rand-connection))]
    (set-uri uri)
    (api/create-database uri)
    (when (seq schemas)
      (load-schemas uri schemas))
    (when (seq seed-data)
      (load-seed-data uri seed-data))
    (when (some #{"noir.server"} (map str (all-ns)))
      (add-noir-middleware uri))
    (when repl
      (repl-init uri))))

(defn build-schema-attr [attr-name value-type & options]
  (let [cardinality (if (some #{:many} options)
                      :db.cardinality/many
                      :db.cardinality/one)
        fulltext    (if-not (= value-type :string) false 
                      (not (boolean (some #{:nofulltext} options))))
        history     (boolean (some #{:nohistory} options))
        index       (not (boolean (some #{:noindex} options)))]
    
    {:db/id           (api/tempid :db.part/db)
     :db/ident        attr-name
     :db/valueType    (keyword "db.type" (name value-type))
     :db/cardinality  cardinality
     :db/index        index
     :db/fulltext     fulltext
     :db/noHistory    history
     :db.install/_attribute :db.part/db}))
    
(defn build-schema [nsp attrs]
  (map #(apply build-schema-attr
               (keyword (name nsp) (name (first %))) (rest %))
       attrs))

(defn build-seed-data [nsp attrs]
  (map (partial util/namespace-keys nsp) attrs))

; TODO: remove once create-model-fns works
(def local-find-id datomic-simple.model/local-find-id)
(def local-find-by datomic-simple.model/local-find-by)
(def expand-ref datomic-simple.model/expand-ref)
(def delete-by datomic-simple.model/delete-by)
(def local-all-by datomic-simple.model/local-all-by)
(def local-find-first-by datomic-simple.model/local-find-first-by)
(def create datomic-simple.model/create)
(def update datomic-simple.model/update)
