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

(def user-table (atom [{:name "mathieu" :team "SRP"}
                       {:name "Mike" :team "Giraffe"}]))

(defroutes api
  (GET "/" [] (slurp "resources/public/html/index.html"))
  (GET "/entry/:name" [name] (response (filter (fn [{n :name}] (= n name )) @user-table)))
  (DELETE "/entry" {{name :name} :body} (response (do (println "delete" name) (swap! user-table (fn [u] (filter (fn [{n :name}] (not (= n name))) u))) @user-table)))
  (PUT "/entry" {newuser :body} (response (do (println "added" newuser) (swap! user-table (fn [u] (conj u newuser))) @user-table)))
  (c-route/resources "/"))

;;
(def app
  (->
    (var api)
    (handler/api)
    (wrap-reload '(one-route.core))
    (ring-json/wrap-json-body {:keywords? true})
    (ring-json/wrap-json-response)))

(defn start-server []
  (server/serve (var app) {:port 8070
                           :join? false
                       :open-browser? false}))


;;(def server (start-server))

;;(.stop server)


;;(macroexpand '(GET "/entry/:name" [name] (do (println "my test") "hello world")))
