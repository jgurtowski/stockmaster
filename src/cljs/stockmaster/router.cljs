(ns stockmaster.router
  (:require
   [re-frame.core :as reframe]
   [reitit.frontend]
   [reitit.frontend.easy :as reitit-easy]
   [reitit.frontend.controllers :as reitit-controllers]
   [reitit.coercion.spec]
   [stockmaster.optionstable :as optionstable]))



(reframe/reg-sub
 ::current-route
 (fn [db]
   (::current-route db)))

(reframe/reg-event-fx
 :navigate
 (fn [_cofx [_ & route]]
   {::navigate! route}))

(reframe/reg-fx
 ::navigate!
 (fn [route]
   (apply reitit-easy/push-state route)))

(reframe/reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (let [old-match (::current-route db)
         controllers (reitit-controllers/apply-controllers
                      (:controllers old-match)
                      new-match)]
     (assoc db ::current-route (assoc new-match :controllers controllers)))))

(def routes
  [["/"
    {:name :routes/root
     :view optionstable/option-table-root}]
   ["/login"
    {:name :routes/login}]
     ;:view login/main}]
  ;will have some problems w/ futures and indexes that have /
  ;in the name
   ["/options/:symbol"
    {:name :routes/options
     :view optionstable/option-table-root
     :controllers
     [{:parameters {:path [:symbol]}
       :start (fn [{:keys [path]}]
                (let [symbol (:symbol path)]
                  (reframe/dispatch [::optionstable/init-options-table symbol])))}]}]])

(def router
  (reitit.frontend/router
   routes
   {:data {:coercion reitit.coercion.spec/coercion}}))


(defn on-navigate [new-match]
  (when new-match
    (reframe/dispatch [::navigated new-match])))


(defn init-routes! []
  (reitit-easy/start!
   router
   on-navigate
   {:use-fragment true}))
