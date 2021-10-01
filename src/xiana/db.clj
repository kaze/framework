(ns xiana.db
  (:require
    [honeysql-postgres.helpers :as helpers]
    [honeysql.core :as sql]
    [next.jdbc :as jdbc]
    [potemkin :refer [import-vars]]
    [xiana.config :as config]
    [xiana.core :as xiana]))

(import-vars
  [honeysql-postgres.helpers
   upsert
   insert-into-as
   returning
   on-conflict
   do-update-set!
   over
   window])

(import-vars
  [honeysql.helpers
   select
   merge-select
   un-select
   from
   merge-from
   join
   merge-join
   left-join
   merge-left-join
   merge-right-join
   full-join
   merge-full-join
   cross-join
   merge-group-by
   order-by
   merge-order-by
   limit
   offset
   lock
   modifiers
   where])

(import-vars
  [honeysql.core
   call])

(defn start
  "Start database instance.
  Get the database specification from
  the 'edn' configuration file in case of
  db-spec isn't set."
  ([] (start nil))
  ([db-spec]
   (when-let [spec (or db-spec (config/get-spec :database))]
     {:datasource (jdbc/get-datasource spec)})))

(defmulti build-clause
  "Create build clause multimethod with associated
          dispatch function: (honeysql-postgres.helpers args)."
  (fn [optype dbtype _args] [dbtype optype]))

(defmethod build-clause [:default :create-table]
  [_ _ args] (helpers/create-table (:table-name args)))

(defmethod build-clause [:default :drop-table]
  [_ _ args] (helpers/drop-table (:table-name args)))

(defmethod build-clause [:default :with-columns]
  [_ _ args]
  (let [{:keys [map rows]} args]
    (helpers/with-columns map rows)))

(defn ->sql-params
  "Parse sql-map using honeysql format function with pre-defined
  options that target postgresql."
  [sql-map]
  (sql/format sql-map
              {:quoting            :ansi
               :parameterizer      :postgresql
               :return-param-names false}))

(defn execute
  "Get connection, parse the given sql-map (query) and
  execute it using `jdbc/execute!`.
  If some error/exceptions occurs returns an empty map."
  [datasource sql-map]
  (with-open [connection (.getConnection datasource)]
    (let [sql-params (->sql-params sql-map)]
      (jdbc/execute! connection sql-params {:return-keys true}))))

(defn create-table
  "Create table specified by its name on the database."
  ([table-name]
   (create-table table-name {:dbtype :default}))
  ([table-name opts]
   (let [dbtype (:dbtype opts)
         args {:table-name table-name}]
     (build-clause :create-table dbtype args))))

(defn drop-table
  "Delete table."
  ([table-name]
   (drop-table table-name {:dbtype :default}))
  ([table-name opts]
   (let [dbtype (:dbtype opts)
         args {:table-name table-name}]
     (build-clause :drop-table dbtype args))))

(defn with-columns
  "Dispatch database operation with columns arguments."
  [m rows]
  (let [args {:map m :rows rows}]
    (build-clause :with-columns :default args)))

(def interceptor
  "Database access interceptor.
  Enter: nil.
  Leave: Fetch and execute a given query using the chosen database
  driver, if succeeds associate its results into state response data.
  Remember the entry query must be a sql-map, e.g:
  {:select [:*] :from [:users]}."
  {:leave
   (fn [{query :query :as state}]
     (xiana/ok
       (if query
         (assoc-in state
                   [:response-data :db-data]
                   ;; returns the result of the database-query
                   ;; execution or empty ({})
                   (execute (get-in state [:deps :db :datasource]) query))
         state)))})
