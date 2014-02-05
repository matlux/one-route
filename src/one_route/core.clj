(ns one-route.core
  (:use [ring.middleware.reload]
        [ring.util.response])
  (:require [compojure.handler :as handler]
            [compojure.core
             :as c-core
             :refer [defroutes GET POST PUT DELETE HEAD OPTIONS PATCH ANY]]
            [compojure.route :as c-route]
            [ring.server.standalone :as server]
            [ring.middleware.json :as ring-json]))

(def json {:a [1,
               2,
               3,
               4,
               {:b "myvalue"}]})

(def db (atom [{:name "steve" :email "steves@mail.com"}
               {:name "mike" :email "mike@gmail.com"}]))


(defroutes  api
  (GET "/" [] (slurp "resources/public/html/index.html"))
  (GET "/health" [name] (response @db))
  (GET "/entry/:query" [query] (response (filter #(= (:name %) query)@db)))
  (PUT "/entry" {user :body} (response (do (swap! db conj user))))
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
