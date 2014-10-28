(ns one-route.core
  (:use [ring.middleware.reload]
        [ring.util.response]
        [org.httpkit.server])
  (:require [compojure.handler :as handler]
            [compojure.core
             :as c-core
             :refer [defroutes GET POST PUT DELETE HEAD OPTIONS PATCH ANY]]
            [compojure.route :as c-route]
            [ring.server.standalone :as server]
            [ring.middleware.json :as ring-json]
            [ring.middleware.params :as ring-params]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.nested-params :as nested-params]
            [ring.middleware.session :as session]
            [cemerick.drawbridge :as drawbridge])
  (:gen-class))

(def user-table (atom [{:name "mathieu" :team "SRP"}
                       {:name "Mike" :team "Giraffe"}]))

(defroutes api
  (GET "/" [] (clojure.java.io/resource "public/html/index.html"))
  (GET "/entry/:name" [name] (response (filter (fn [{n :name}] (= n name )) @user-table)))
  (DELETE "/entry" {{name :name} :body} (response (do (println "delete" name) (swap! user-table (fn [u] (filter (fn [{n :name}] (not (= n name))) u))) @user-table)))
  (PUT "/entry" {newuser :body} (response (do (println "added" newuser) (swap! user-table (fn [u] (conj u newuser))) @user-table)))
  (c-route/resources "/"))


(def drawbridge-handler
  (-> (cemerick.drawbridge/ring-handler)
      (keyword-params/wrap-keyword-params)
      (nested-params/wrap-nested-params)
      (ring-params/wrap-params)
      (session/wrap-session)))

(defn wrap-drawbridge [handler]
  (fn [req]
    (if (= "/repl" (:uri req))
      (drawbridge-handler req)
      (handler req))))

;;
(def app
  (->
    (var api)
    (handler/api)
    (wrap-reload '(one-route.core))
    (ring-json/wrap-json-body {:keywords? true})
    (ring-json/wrap-json-response)
    (wrap-drawbridge)))

(defn start-server []
  (server/serve (var app) {:port 8070
                           :join? false
                       :open-browser? false}))

(defn -main []
  (run-server (var app) {:port 8080}))




;;(def server (start-server))

;;(.stop server)


;;(macroexpand '(GET "/entry/:name" [name] (do (println "my test") "hello world")))
