(ns stockmaster.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as reframe]
            [stockmaster.optiontable :as optiontable]
            [stockmaster.events]
            [stockmaster.subscriptions]))

(reframe/reg-event-db
 :initialize-db
 (fn [_ _]
   {:db {}}))


(defn app []
  [optiontable/option-list])

(defn init! []
  (reframe/dispatch-sync [:initialize-db])
  (reframe/dispatch [:request-option-expirations "SPY"])
  (reagent-dom/render
   [app]
   (.getElementById js/document "root")))


