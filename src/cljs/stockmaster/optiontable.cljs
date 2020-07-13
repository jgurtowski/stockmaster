(ns stockmaster.optiontable
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            ["@material-ui/core/Table" :default Table]
            ["@material-ui/core/TableContainer" :default TableContainer]
            ["@material-ui/core/TableHead" :default TableHead]
            ["@material-ui/core/TableRow" :default TableRow]
            ["@material-ui/core/TableCell" :default TableCell]
            ["@material-ui/core/TableBody" :default TableBody]
            ["@material-ui/core/Collapse" :default Collapse]
            ["@material-ui/core/Paper" :default Paper]
            ["@material-ui/core/IconButton" :default IconButton]
            ["@material-ui/icons/KeyboardArrowRight" :default KeyboardArrowRight]
            ["@material-ui/icons/KeyboardArrowDown" :default KeyboardArrowDown]))


(defn option-table []
  [:> TableContainer {:component Paper}
   [:> Table {:size "small"}
    [:> TableHead
     [:> TableRow
      [:> TableCell "Ask"]
      [:> TableCell "Bid"]
      [:> TableCell "Mark"]
      [:> TableCell "Strike"]
      [:> TableCell "Mark"]
      [:> TableCell "Bid"]
      [:> TableCell "Ask"]]]
    [:> TableBody
     [:> TableRow
      [:> TableCell 0.1]
      [:> TableCell 0.2]
      [:> TableCell 0.05]
      [:> TableCell 5]
      [:> TableCell 0.4]
      [:> TableCell 0.35]
      [:> TableCell 0.45]]]]])

(defn option-list-entry [rowname]
  (let [component-state (reagent/atom {:isopen false})]
    (fn []
      [:<>
       [:> TableRow
        [:> TableCell
         [:> IconButton {:size "small" :on-click #(swap! component-state update-in [:isopen] not)}
          (if (:isopen @component-state) [:> KeyboardArrowDown] [:> KeyboardArrowRight])]
         rowname]]
       [:> TableRow
        [:> TableCell {:style {:padding-bottom 0 :padding-top 0 } :col-span 1}
         [:> Collapse {:in (:isopen @component-state) :timeout "auto" :unmount-on-exit true}
          [option-table]]]]])))


(defn option-list []
  "Top Level Option List that has the Different Expirations"
  (let [option-expirations @(reframe/subscribe [:option-expirations])]
    [:> TableContainer {:component Paper}
     [:> Table {:size "small"}
      [:> TableBody
       (for [x option-expirations]
         ^{:key x} [option-list-entry x])]]]))







