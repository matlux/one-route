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
            [ring.util.response :as resp]
            [monger.core :as mg]
            [monger.collection :as mc]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [hiccup.page :as h]
            [hiccup.element :as e]
            [one-route.misc :as misc]
            )
   (:import [com.mongodb MongoOptions ServerAddress]))

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(def json {:a [1,
               2,
               3,
               4,
               {:b "myvalue"}]})

(def users {"root" {:username "root"
                    :password (creds/hash-bcrypt "admin")
                    :roles #{::admin}}
            "jane" {:username "jane"
                    :password (creds/hash-bcrypt "user")
                    :roles #{::user}}})

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


(def login-form
  [:div {:class "row"}
   [:div {:class "columns small-12"}
    [:h3 "Login"]
    [:div {:class "row"}
     [:form {:method "POST" :action "login" :class "columns small-4"}
      [:div "Username" [:input {:type "text" :name "username"}]]
      [:div "Password" [:input {:type "password" :name "password"}]]
      [:div [:input {:type "submit" :class "button" :value "Login"}]]]]]])

(defroutes user-routes
  (GET "/entry/:query" [query] (response (lookup query)))
)




(defroutes  api
  (GET "/" [] (friend/authorize #{::user} (slurp "resources/public/html/index.html")))

  (GET "/health" [name] (mc/find-maps "users"))
  (GET "/entry/:username" [username]
       (friend/authorize #{::user}  (response (lookup username))))

  (PUT "/entry" {user :body} (friend/authorize #{::user} (response (create-user user))))
  (DELETE "/delete/entry" {{name :_id} :body} (friend/authorize #{::user} (response (delete-user name))))
  (GET "/admin" request (friend/authorize #{::admin}
                                          #_any-code-requiring-admin-authorization
                                          "Admin page."))
  (GET "/login" req
       (h/html5 misc/pretty-head (misc/pretty-body login-form)))

  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (GET "/role-user" req
       (friend/authorize #{::user} "You're a user!"))
  (GET "/role-admin" req
       (friend/authorize #{::admin} "You're an admin!"))
  (c-route/resources "/" {:root "META-INF/resources/webjars/foundation/4.0.4/"})
  ;; (c-core/context "/user" request
  ;;    (friend/wrap-authorize user-routes #{::user ::admin}))

  )





(def app
  (->
   (friend/authenticate
              api
              {:allow-anon? true
               :login-uri "/login"
               :default-landing-uri "/"
               :unauthorized-handler #(-> (h/html5 [:h2 "You do not have sufficient privileges to access " (:uri %)])
                                        resp/response
                                        (resp/status 401))
               :credential-fn #(creds/bcrypt-credential-fn users %)
               :workflows [(workflows/interactive-form)]})
      (handler/site)
      (wrap-reload '(one-route.core))
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      ))




(defn start-server []
  (setDB)
  (server/serve #'app {:port 8070
                       :join? false
                       :open-browser? false}))

;;(or page)


;;(def server (start-server))
;;(.stop server)



;;(mg/connect!)

;;(mg/set-db! (mg/get-db "monger-test"))

;;(mc/insert "documents" {  :first_name "John" :last_name "Lennon" })
;;(mc/insert "documents" (first @db))
;;(mc/insert "documents" {:_id "steve" :email "steves@mail.com"})
;;(type (into {} (mc/find-one "users" { :_id "steve" })))

;;(mc/find-maps "users" { :_id "steve"})
;;(mc/find-maps "users")
;;(mc/insert "users" {:_id "bob" :email "bob@gmail.com"} )
