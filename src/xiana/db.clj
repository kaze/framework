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

(defonce -datasource (atom nil))

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

(defn connection
  "Get or start database connection."
  []
  (.getConnection (or @-datasource
                      (reset! -datasource
                              (jdbc/get-datasource (config/get-spec :postgresql))))))

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
  [sql-map]
  (with-open [connection (connection)]
    (let [sql-params (->sql-params sql-map)]
      (jdbc/execute! connection sql-params {:return-keys true}))))

(def interceptor
  "Database access interceptor.
  Enter: nil.
  Leave: Fetch and execute a given query using the database
  driver, if succeeds associate its results into context response data.
  Remember the entry query must be a sql-map, e.g:
  {:select [:*] :from [:users]}."
  {:leave
   (fn [{query :query :as ctx}]
     (xiana/ok
       (if query
         (assoc-in ctx
                   [:response-data :db-data]
                   (execute query))
         ctx)))})
