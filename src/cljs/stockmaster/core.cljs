(ns stockmaster.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as reframe]
            [stockmaster.components :as components]
            [stockmaster.events]
            [stockmaster.subscriptions]))

(reframe/reg-event-db
 :initialize-db
 (fn [_ _]
   {:db {}}))

(defn init! []
  (reframe/dispatch-sync [:initialize-db])
  (reframe/dispatch [:request-option-expirations "SPY"])
  (reagent-dom/render
   [components/app]
   (.getElementById js/document "root")))


