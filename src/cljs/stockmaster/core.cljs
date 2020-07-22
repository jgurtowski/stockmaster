(ns stockmaster.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as reframe]
            [stockmaster.appbar :refer [app-bar]]
            [stockmaster.optionstable :refer [option-expiration-list]]))

(defn init! []
  (reagent-dom/render
   [:<>
    [:> app-bar]
    [option-expiration-list]]
   (.getElementById js/document "root")))


