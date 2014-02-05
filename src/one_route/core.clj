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

(def json {:a [1,
               2,
               3,
               4,
               {:b "myvalue"}]})

;; (def db (atom [{:_id "steve" :email "steves@mail.com"}
;;                {:_id "mike" :email "mike@gmail.com"}]))
(mg/connect!)

(mg/set-db! (mg/get-db "users"))


(mc/insert-batch "users" [{:_id "steve" :email "steves@mail.com"}
                          {:_id "mike" :email "mike@gmail.com"}])


(defroutes  api
  (GET "/" [] (slurp "resources/public/html/index.html"))
  (GET "/health" [name] (response @db))
  (GET "/entry/:query" [query] (response (filter #(= (:_id %) query)@db)))
  (PUT "/entry" {user :body} (response (do (swap! db conj user))))
  (DELETE "/delete/entry" {{name :_id} :body} (response (swap! db (fn [db] (filter #(not= (:_id %) name) db)))))
  (c-route/resources "/"))

(def app
  (-> (var api)
      (handler/api)
      (wrap-reload '(one-route.core))
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)))

(defn start-server []
  (server/serve #'app {:port 8070
                       :join? false
                       :open-browser? false}))

;;(def server (start-server))
(for [{n :name :as user} @db
      :when (= n "steve")] user)



;;(mg/connect!)

;;(mg/set-db! (mg/get-db "monger-test"))

;;(mc/insert "documents" {  :first_name "John" :last_name "Lennon" })
;;(mc/insert "documents" (first @db))
;;(mc/insert "documents" {:_id "steve" :email "steves@mail.com"})
