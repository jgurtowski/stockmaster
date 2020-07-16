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


(defn option-table-row [strike]
  [:<>
   [:> TableRow
    [:> TableCell 0]
    [:> TableCell 0]
    [:> TableCell 0]
    [:> TableCell strike]
    [:> TableCell 0]
    [:> TableCell 0]
    [:> TableCell 0]]])

(defn option-table [expiration]
  (let [strikes @(reframe/subscribe [:option-expiration-strikes expiration])]
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
       (for [strike strikes]
         ^{:key strike} [option-table-row strike])]]]))

(defn option-list-entry [option-expiration]
  (let [component-state (reagent/atom {:isopen false})
        option-list-click (fn []
                            (swap! component-state update-in [:isopen] not)
                            (if (:isopen @component-state)
                              (reframe/dispatch [:request-option-strikes "SPY" option-expiration])))]
    (fn []
      [:<>
       [:> TableRow
        [:> TableCell
         [:> IconButton {:size "small" :on-click option-list-click}
          (if (:isopen @component-state) [:> KeyboardArrowDown] [:> KeyboardArrowRight])]
         option-expiration]]
       [:> TableRow
        [:> TableCell {:style {:padding-bottom 0 :padding-top 0 } :col-span 1}
         [:> Collapse {:in (:isopen @component-state) :timeout "auto" :unmount-on-exit true}
          [option-table option-expiration]]]]])))


(defn option-list []
  "Top Level Option List that has the Different Expirations"
  (let [option-expirations @(reframe/subscribe [:option-expirations])]
    [:> TableContainer {:component Paper}
     [:> Table {:size "small"}
      [:> TableBody
       (for [x option-expirations]
         ^{:key x} [option-list-entry x])]]]))






