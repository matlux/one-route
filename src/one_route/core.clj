(ns one-route.core
  (:use [ring.middleware.reload]
        [ring.util.response])
  (:require [compojure.handler :as handler]
            [compojure.core
             :as c-core
             :refer [defroutes GET POST PUT DELETE HEAD OPTIONS PATCH ANY]]
            [compojure.route :as c-route]
            [ring.server.standalone :as server]
            [ring.middleware.json :as ring-json]
            [monger.core :as mg]
            [monger.collection :as mc]
            )
   (:import [com.mongodb MongoOptions ServerAddress]))

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(def json {:a [1,
               2,
               3,
               4,
               {:b "myvalue"}]})

;; (def db (atom [{:_id "steve" :email "steves@mail.com"}
;;                {:_id "mike" :email "mike@gmail.com"}]))
(defn setDB []
  (mg/connect!)
  (mg/set-db! (mg/get-db "usersdb"))
  ;; (mc/insert-batch "users" [{:_id "steve" :email "steves@mail.com"}
  ;;                           {:_id "mike" :email "mike@gmail.com"}])
  )

(defn lookup [user]
  (into {} (mc/find-one "users" { :_id user })))


(defn create-user [user]
  (into {}  (do (dbg (mc/insert "users" user)) {:status "ok"})))

(defn delete-user [user])

(defn delete-user [id]
  (do (mc/remove "users" { :_id id }) {:status "ok"}))

(defroutes  api
  (GET "/" [] (slurp "resources/public/html/index.html"))
  (GET "/health" [name] (mc/find-maps "users"))
  (GET "/entry/:query" [query] (response (lookup query)))
  (PUT "/entry" {user :body} (response (create-user user)))
  (DELETE "/delete/entry" {{name :_id} :body} (response (delete-user name)))
  (c-route/resources "/"))

(def app
  (-> (var api)
      (handler/api)
      (wrap-reload '(one-route.core))
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)))

(defn start-server []
  (setDB)
  (server/serve #'app {:port 8070
                       :join? false
                       :open-browser? false}))

;;(def server (start-server))




;;(mg/connect!)

;;(mg/set-db! (mg/get-db "monger-test"))

;;(mc/insert "documents" {  :first_name "John" :last_name "Lennon" })
;;(mc/insert "documents" (first @db))
;;(mc/insert "documents" {:_id "steve" :email "steves@mail.com"})
(type (into {} (mc/find-one "users" { :_id "steve" })))
;;(mc/find-maps "users" { :_id "steve"})
;;(mc/find-maps "users")
;;(mc/insert "users" {:_id "bob" :email "bob@gmail.com"} )
