(ns stockmaster.optionstable
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [goog.string :as gstring]
            [goog.string.format]
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



;************************************************************************************************
;************************************************************************************************
;************************************************************************************************
;*Components
;************************************************************************************************
;************************************************************************************************
;*************************************************************************************************

(defn option-table-row [call-symbol put-symbol]
  [:<>
   [:> TableRow
    [:> TableCell {:align "right"} (gstring/format "%.2f" @(reframe/subscribe [::ask call-symbol]))]
    [:> TableCell {:align "right"} (gstring/format "%.2f" @(reframe/subscribe [::bid call-symbol]))]
    [:> TableCell {:align "right"} (gstring/format "%.2f" @(reframe/subscribe [::mark call-symbol]))]
    [:> TableCell {:align "right"} (gstring/format "%.2f" @(reframe/subscribe [::strike call-symbol]))]
    [:> TableCell {:align "right"} (gstring/format "%.2f" @(reframe/subscribe [::mark put-symbol]))]
    [:> TableCell {:align "right"} (gstring/format "%.2f" @(reframe/subscribe [::bid put-symbol]))]
    [:> TableCell {:align "right"} (gstring/format "%.2f"@(reframe/subscribe [::ask put-symbol]))]]])

(defn option-table [expiration]
  (let [strike-containers @(reframe/subscribe [::option-expiration-strikes expiration])]
    [:> TableContainer {:component Paper}
     [:> Table {:size "small"}
      [:> TableHead
       [:> TableRow
        [:> TableCell {:align "right"} "Ask"]
        [:> TableCell {:align "right"} "Bid"]
        [:> TableCell {:align "right"} "Mark"]
        [:> TableCell {:align "right"} "Strike"]
        [:> TableCell {:align "right"} "Mark"]
        [:> TableCell {:align "right"} "Bid"]
        [:> TableCell {:align "right"} "Ask"]]]
      [:> TableBody
       (for [strike (keys strike-containers)]
         (let [{call-symbol ::call-symbol put-symbol ::put-symbol} (get strike-containers strike)]
           ^{:key strike} [option-table-row call-symbol put-symbol]))]]]))

(defn option-list-entry [option-expiration]
  (let [component-state (reagent/atom {:isopen false})
        option-list-click (fn []
                            (swap! component-state update-in [:isopen] not)
                            (if (:isopen @component-state)
                              (reframe/dispatch [::request-option-chains option-expiration])))]
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


(defn option-expiration-list []
  "Top Level Option List that has the Different Expirations"
  (let [option-expirations @(reframe/subscribe [::option-expirations])]
    [:> TableContainer {:component Paper}
     [:> Table {:size "small"}
      [:> TableBody
       (for [x option-expirations]
         ^{:key x} [option-list-entry x])]]]))


;************************************************************************************************
;************************************************************************************************
;************************************************************************************************
;*Subscriptions
;************************************************************************************************
;************************************************************************************************
;************************************************************************************************

(reframe/reg-sub
 ::option-expirations
 (fn [db]
   (sort (keys (::option-expirations db)))))

(reframe/reg-sub
 ::option-expiration-strikes
 (fn [db [_ expiration]]
   (get-in db [::option-expirations expiration ::strikes])))


(reframe/reg-sub
 ::bid
 (fn [db [_ symbol]]
   (get-in db [::symbols symbol :bid])))

(reframe/reg-sub
 ::ask
 (fn [db [_ symbol]]
   (get-in db [::symbols symbol :ask])))

(reframe/reg-sub
 ::strike
 (fn [db [_ symbol]]
   (get-in db [::symbols symbol :strike])))

(defn calc-mark
  [db symbol]
  (let [bid (get-in db [::symbols symbol :bid])
        ask (get-in db [::symbols symbol :ask])]
    (/ (+ bid ask) 2)))

(reframe/reg-sub
 ::mark
 (fn [db [_ symbol]]
   (calc-mark db symbol)))

(reframe/reg-sub
 ::underlying-symbol-db
 (fn [db _]
   (get db ::underlying-symbol)))

;************************************************************************************************
;************************************************************************************************
;************************************************************************************************
;*Events
;************************************************************************************************
;************************************************************************************************
;************************************************************************************************

(reframe/reg-event-fx
 ::init-options-table
 (fn-traced [_ [_ symbol]]
            {:dispatch-n [[::request-option-expirations symbol]
                          [::set-underlying-symbol symbol]]}))
                          ;[::clean-up-symbols]

;TODO we would like to clean up unused symbols when they are not used
(reframe/reg-event-db
 ::clean-up-symbols
 (fn-traced [db _]
     ;(assoc db ::symbols {})))
            nil))


(reframe/reg-event-db
 ::set-underlying-symbol
 (fn-traced [db [_ symbol]]
            (assoc db ::underlying-symbol symbol)))

(reframe/reg-event-fx
 ::request-option-expirations
 (fn-traced [_ event]
            {:http-xhrio {:method :get
                 :uri "https://sandbox.tradier.com/v1/markets/options/expirations"
                 :headers {:Accept "application/json"
                           :Authorization "Bearer fUMaw0yjP8h253ko8uYS6rxwFoli"}
                 :params {:symbol (second event)}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::success-http-option-expirations]
                 :on-failure [::failure-http-option-expirations]}}))

(reframe/reg-event-db
 ::success-http-option-expirations
 (fn-traced [db [_ result]]
   (let [expiration-dates-list (get-in result [:expirations :date])]
     (assoc db ::option-expirations
            (apply merge (map #(hash-map % {}) expiration-dates-list))))))

(reframe/reg-event-db
 ::failure-http-option-expirations
 (fn-traced [db [_ request-type response]]
   (assoc db ::request-fail true)))


(reframe/reg-event-fx
 ::request-option-chains
 (fn-traced [cofx [_ expiration]]
            (let [symbol (get-in cofx [:db ::underlying-symbol])]
                  {:http-xhrio {:method :get
                                :uri "https://sandbox.tradier.com/v1/markets/options/chains"
                                :headers {:Accept "application/json"
                                          :Authorization "Bearer fUMaw0yjP8h253ko8uYS6rxwFoli"}
                                :params {:symbol symbol
                                         :expiration expiration}
                                :format (ajax/json-request-format)
                                :response-format (ajax/json-response-format {:keywords? true})
                                :on-success [::success-http-option-chains expiration]
                                :on-failure [::failure-http-option-chains]}})))

(defn extract-symbol-from-chain
  [option]
  (if (= (:option_type option) "put")
    (hash-map ::put-symbol (:symbol option))
    (hash-map ::call-symbol (:symbol option))))

(reframe/reg-event-db
 ::success-http-option-chains
 (fn-traced [db [_ expiration result]]
   (let [options (get-in result [:options :option])
         symbol-map (apply merge (map #(hash-map (:symbol %) %) options))
         db-with-symbols (merge-with into db {::symbols symbol-map})
         strikes (apply merge-with into (map #(sorted-map (:strike %) (extract-symbol-from-chain %)) options))]
     (assoc-in db-with-symbols [::option-expirations expiration ::strikes] strikes))))

(reframe/reg-event-db
 ::failure-http-option-chains
 (fn-traced [db [_ request-type response]]
   (assoc db ::request-fail true)))
