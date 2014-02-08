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
                             [credentials :as creds]
                             [openid :as openid])
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
   [:div {:class "col-lg-10 col-lg-offset-0"}
    [:div {:class "panel"}
     [:h3 "Login:"]



     [:form {:method "POST" :action "login" :class "form-inline"}
      [:div "Username" [:input {:type "text" :name "username" :class "form-control"}]]
      [:div "Password" [:input {:type "password" :name "password" :class "form-control"}]]
      [:div [:input {:type "submit" :class "button" :value "Login"}]]]]]])

(defroutes user-routes
  (GET "/entry/:query" [query] (response (lookup query)))
)

(def providers [{:name "Google" :url "https://www.google.com/accounts/o8/id"}
                {:name "Yahoo" :url "http://me.yahoo.com/"}
                {:name "AOL" :url "http://openid.aol.com/"}
                {:name "Wordpress.com" :url "http://username.wordpress.com"}
                {:name "MyOpenID" :url "http://username.myopenid.com/"}])


(defn openid-info [req]
  [:div {:class "row"}
   [:h2 "Authenticating with various services using OpenID"]
    [:h3 "Current Status " [:small "(this will change when you log in/out)"]]
    (if-let [auth (friend/current-authentication req)]
      [:p "Some information delivered by your OpenID provider:"
       [:ul (for [[k v] auth
                  :let [[k v] (if (= :identity k)
                                ["Your OpenID identity" (str (subs v 0 (* (count v) 2/3)) "…")]
                                [k v])]]
              [:li [:strong (str (name k) ": ")] v])]]
      [:div
       [:h3 "Login with…"]
       (for [{:keys [name url]} providers
             :let [base-login-url (misc/context-uri req (str "/login?identifier=" url))
                   dom-id (str (gensym))]]
         [:form {:method "POST" :action (misc/context-uri req "login")
                 :onsubmit (when (.contains ^String url "username")
                             (format "var input = document.getElementById(%s); input.value = input.value.replace('username', prompt('What is your %s username?')); return true;"
                                     (str \' dom-id \') name))}
          [:input {:type "hidden" :name "identifier" :value url :id dom-id}]
          [:input {:type "submit" :class "button" :value name}]])
       [:p "…or, with a user-provided OpenID URL:"]
       [:form {:method "POST" :action (misc/context-uri req "login")}
        [:input {:type "text" :name "identifier" :style "width:250px;"}]
        [:input {:type "submit" :class "button" :value "Login"}]]])])

(defn home-page-openid [req]
  (h/html5
   misc/pretty-head
   (misc/pretty-body
    [:h2 "Authenticating with various services using OpenID"]
    [:h3 "Current Status " [:small "(this will change when you log in/out)"]]
    (if-let [auth (friend/current-authentication req)]
      [:p "Some information delivered by your OpenID provider:"
       [:ul (for [[k v] auth
                  :let [[k v] (if (= :identity k)
                                ["Your OpenID identity" (str (subs v 0 (* (count v) 2/3)) "…")]
                                [k v])]]
              [:li [:strong (str (name k) ": ")] v])]]
      [:div
       [:h3 "Login with…"]
       (for [{:keys [name url]} providers
             :let [base-login-url (misc/context-uri req (str "/login?identifier=" url))
                   dom-id (str (gensym))]]
         [:form {:method "POST" :action (misc/context-uri req "login")
                 :onsubmit (when (.contains ^String url "username")
                             (format "var input = document.getElementById(%s); input.value = input.value.replace('username', prompt('What is your %s username?')); return true;"
                                     (str \' dom-id \') name))}
          [:input {:type "hidden" :name "identifier" :value url :id dom-id}]
          [:input {:type "submit" :class "button" :value name}]])
       [:p "…or, with a user-provided OpenID URL:"]
       [:form {:method "POST" :action (misc/context-uri req "login")}
        [:input {:type "text" :name "identifier" :style "width:250px;"}]
        [:input {:type "submit" :class "button" :value "Login"}]]])
    [:h3 "Logging out"]
    [:p [:a {:href (misc/context-uri req "logout")} "Click here to log out"] "."]))
  )


(defn home-page [req] (h/html5
  misc/pretty-head
  (misc/pretty-body

   [:h3 "Current Status " [:small "(this will change when you log in/out)"]]
   [:p (if-let [identity (friend/identity req)]
         (apply str "Logged in, with these roles: "
                (-> identity friend/current-authentication :roles))
         "anonymous user")]
   (openid-info req)
   [:h3 "Authorization demos"]
   [:h1 "Address book"]
   [:h2 "Lookup"]
   [:form {:id "lookupForm" :class "form-inline" :onsubmit "return false;"}
    [:div [:input {:id "lookupName" :type "text" :class "form-control" :placeholder "Name"}]
     [:button {:type "submit" :onclick "lookupEntry();" :class "btn btn-success"} "Lookup"]]]
   [:hr]
   [:pre {:id "lookupEntryResult"}]

   [:h2 "Add entry"]
   [:form {:id "addForm" :class "form-inline" :onsubmit "return false;"}
    [:div [:input {:id "addName" :type "text" :class "form-control" :placeholder "Name"}]]
    [:div [:input {:id "addEmail" :type "text" :class "form-control" :placeholder "Email address"}]]
    [:button {:type "submit" :onclick "addEntry();" :class "btn btn-success"} "Add"]
    [:button {:type "submit" :onclick "deleteEntry();" :class "btn btn-failure"} "Delete"]
    ]
   [:hr]
   [:pre {:id "addEntryResult"}]


   [:p "Each of these links require particular roles (or, any authentication) to access. "
    "If you're not authenticated, you will be redirected to a dedicated login page. "
    "If you're already authenticated, but do not meet the authorization requirements "
    "(e.g. you don't have the proper role), then you'll get an Unauthorized HTTP response."]
   [:ul [:li (e/link-to (misc/context-uri req "role-user") "Requires the `user` role")]
    [:li (e/link-to (misc/context-uri req "role-admin") "Requires the `admin` role")]
    [:li (e/link-to (misc/context-uri req "requires-authentication")
                    "Requires any authentication, no specific role requirement")]]
   [:h3 "Logging out"]
   [:p (e/link-to (misc/context-uri req "logout") "Click here to log out") "."])))



(defroutes routes
  (GET "/" req
    (home-page-openid req))
  (GET "/logout" req
    (friend/logout* (resp/redirect (str (:context req) "/")))))





;;(slurp "resources/public/html/index.html")
(defroutes  api
  (GET "/" req (home-page-openid req))
  (GET "/logout" req
    (friend/logout* (resp/redirect (str (:context req) "/"))); (home-page-openid req)
    )
  (GET "/chess" req (home-page req))

  (GET "/health" [name] (mc/find-maps "users"))
  (GET "/entry/:username" [username]
       (response (lookup username)))

  ;; (PUT "/entry" {user :body} (friend/authorize #{::user} (response (create-user user))))
  ;; (DELETE "/delete/entry" {{name :_id} :body} (friend/authorize #{::user} (response (delete-user name))))
  ;; (GET "/admin" request (friend/authorize #{::admin}
  ;;                                         #_any-code-requiring-admin-authorization
  ;;                                         "Admin page."))
  ;; (GET "/login" req
  ;;      (h/html5 misc/pretty-head (misc/pretty-body login-form)))

  ;; (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  ;; (GET "/role-user" req
  ;;      (friend/authorize #{::user} "You're a user!"))
  ;; (GET "/role-admin" req
  ;;      (friend/authorize #{::admin} "You're an admin!"))
  (c-route/resources "/"; {:root "META-INF/resources/webjars/foundation/4.0.4/"}
  )

  ;; (c-core/context "/user" request
  ;;    (friend/wrap-authorize user-routes #{::user ::admin}))

  )





(def app
  (->
   (friend/authenticate
              api
              {:allow-anon? true
               ;;:login-uri "/login"
               :default-landing-uri "/"
               ;; :unauthorized-handler #(-> (h/html5 [:h2 "You do not have sufficient privileges to access " (:uri %)])
               ;;                          resp/response
               ;;                          (resp/status 401))
               :credential-fn #(creds/bcrypt-credential-fn users %)
               :workflows [(openid/workflow
                             :openid-uri "/login"
                             :credential-fn identity)]})
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
