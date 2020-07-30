(ns stockmaster.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as reframe]
            [stockmaster.optionstable :refer [option-table-root]]))

(defn init! []
  (reagent-dom/render
   [option-table-root]
   (.getElementById js/document "root")))



