(ns stockmaster.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as reframe]
            [stockmaster.router :as router]))

(defn root []
  (let [current-route @(reframe/subscribe [::router/current-route])]
    (when current-route
      [(-> current-route :data :view)])))

(defn init! []
  (router/init-routes!)
  (reagent-dom/render
   [root]
   (.getElementById js/document "root")))
