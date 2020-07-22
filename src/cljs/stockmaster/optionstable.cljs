(ns stockmaster.optionstable
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [cljs-time.core :as cljs-time]
            [cljs-time.format :as cljs-time-format]
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
            ["@material-ui/icons/KeyboardArrowDown" :default KeyboardArrowDown]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Typography" :default Typography]))




;************************************************************************************************
;************************************************************************************************
;************************************************************************************************
;*Utilities
;************************************************************************************************
;************************************************************************************************
;*************************************************************************************************

(defn days-to-expiration
  "Takes expiration as a string"
  [expiration]
  (let [exp-date (cljs-time-format/parse (:date cljs-time-format/formatters) expiration)
        today (cljs-time/today)
        interval (cljs-time/interval today exp-date)]
    (+ 1 (cljs-time/in-days interval))))

;************************************************************************************************
;************************************************************************************************
;************************************************************************************************
;*Components
;************************************************************************************************
;************************************************************************************************
;*************************************************************************************************

(defn underlying-header
  "Header for the table with the underlying symbol's trade information"
  []
  (let [underlying-symbol @(reframe/subscribe [::underlying-symbol-db])
        underlying-mark @(reframe/subscribe [::underlying-mark])]
        [:> Grid {:container true :spacing 3}
         [:> Grid {:item true :xs 12}
          [:> Typography underlying-symbol]]
         [:> Grid {:item true :xs 3}
          [:> Typography underlying-mark]]]))

(defn option-table-cell [symbol attr itm?]
  (let [value @(reframe/subscribe [attr symbol])
        display-value (gstring/format "%.2f" value)
        style (if itm? {:background-color "gray"} {})]
    [:> TableCell {:style style :align "center"} display-value]))

(defn return-on-risk-cell [symbol itm?]
  (let [ror (* 100 @(reframe/subscribe [::return-on-risk symbol]))
        ror-annualized (* 100 @(reframe/subscribe [::return-on-risk-annualized symbol]))
        display-value (str (gstring/format "%.2f" ror) "/" (gstring/format "%.2f" ror-annualized))
        style (if itm? {:background-color "gray"} {})]
    [:> TableCell {:style style :align "center"} display-value])) 


(defn option-table-row [call-symbol put-symbol]
  (let [call-itm? @(reframe/subscribe [::itm? call-symbol])
        put-itm? @(reframe/subscribe [::itm? put-symbol])]
    [:> TableRow
     [option-table-cell call-symbol ::ask call-itm?]
     [option-table-cell call-symbol ::bid call-itm?]
     [option-table-cell call-symbol ::mark call-itm?]
     [option-table-cell call-symbol ::strike false]
     [option-table-cell put-symbol ::mark put-itm?]
     [option-table-cell put-symbol ::bid put-itm?]
     [option-table-cell put-symbol ::ask put-itm?]
     [return-on-risk-cell put-symbol put-itm?]]))

(defn option-table [expiration]
  (let [strike-containers @(reframe/subscribe [::option-expiration-strikes expiration])]
    [:> TableContainer {:component Paper}
     [:> Table {:size "small"}
      [:> TableHead
       [:> TableRow
        [:> TableCell {:align "center"} "Ask"]
        [:> TableCell {:align "center"} "Bid"]
        [:> TableCell {:align "center"} "Mark"]
        [:> TableCell {:align "center"} "Strike"]
        [:> TableCell {:align "center"} "Mark"]
        [:> TableCell {:align "center"} "Bid"]
        [:> TableCell {:align "center"} "Ask"]
        [:> TableCell {:align "center"} "RoR% (p/a)"]]]
      [:> TableBody
       (for [strike (keys strike-containers)]
         (let [{call-symbol ::call-symbol put-symbol ::put-symbol} (get strike-containers strike)]
           ^{:key strike} [option-table-row call-symbol put-symbol]))]]]))

(defn option-list-entry [option-expiration]
  (let [component-state (reagent/atom {:isopen false})
        days-remaining (days-to-expiration option-expiration)
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
         (str option-expiration " (" days-remaining ")")]]
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


(defn option-table-root []
  [:<>
   [underlying-header]
   [option-expiration-list]])

;************************************************************************************************
;************************************************************************************************
;************************************************************************************************
;*Subscriptions
;************************************************************************************************
;************************************************************************************************
;************************************************************************************************

(defn calc-mark
  [quote]
  (let [bid (:bid quote)
        ask (:ask quote)]
    (/ (+ bid ask) 2)))

(reframe/reg-sub
 ::itm?
 (fn [[_ symbol] _]
   [(reframe/subscribe [::strike symbol])
    (reframe/subscribe [::option-type symbol])
    (reframe/subscribe [::underlying-mark])])
 (fn [[strike option-type underlying-mark] _]
   (some true?
         [(and (= ::call option-type) (< strike underlying-mark))
          (and (= ::put option-type) (< underlying-mark strike))])))

(reframe/reg-sub
 ::option-type
 (fn [[_ symbol] _]
   (reframe/subscribe [::symbol-quote symbol]))
 (fn [quote _]
   (case (:option_type quote)
     "put" ::put
     "call" ::call
     ::not-an-option)))

(reframe/reg-sub
 ::return-on-risk
 (fn [[_ symbol] _]
   (reframe/subscribe [::symbol-quote symbol]))
 (fn [quote _]
   (let [contract-size (:contract_size quote)
         strike (:strike quote)
         mark (calc-mark quote)]
     (/ (* mark contract-size) (* strike contract-size)))))

(reframe/reg-sub
 ::return-on-risk-annualized
 (fn [[_ symbol] _]
   [(reframe/subscribe [::return-on-risk symbol])
    (reframe/subscribe [::expiration symbol])])
 (fn [[ror expiration] _]
   (let [days-remaining (days-to-expiration expiration)]
     (/ (* ror 365.0) days-remaining))))

(reframe/reg-sub
 ::expiration
 (fn [[_ symbol] _]
   (reframe/subscribe [::symbol-quote symbol]))
 (fn [quote _]
   (:expiration_date quote)))

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

(reframe/reg-sub
 ::mark
 (fn [[_ symbol] _]
   (reframe/subscribe [::symbol-quote symbol]))
 (fn [quote _]
   (calc-mark quote)))

(reframe/reg-sub
 ::underlying-symbol-db
 (fn [db _]
   (get db ::underlying-symbol)))

(reframe/reg-sub
 ::underlying-quote
 (fn [db _]
   (let [underlying-symbol (::underlying-symbol db)]
     (get-in db [::symbols underlying-symbol]))))

(reframe/reg-sub
 ::symbol-quote
 (fn [db [_ symbol]]
      (get-in db [::symbols symbol])))

(reframe/reg-sub
 ::underlying-mark
 (fn [_ _]
   (reframe/subscribe [::underlying-quote]))
 (fn [quote _]
   (calc-mark quote)))


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
            (let [cap-symbol (clojure.string/upper-case symbol)]
              {:dispatch-n [[::request-option-expirations cap-symbol]
                            [::set-underlying-symbol cap-symbol]
                            [::request-quote cap-symbol]]})))

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
 ::request-quote
 (fn-traced [_ [_ symbol]]
            {:http-xhrio {:method :get
                          :uri "https://sandbox.tradier.com/v1/markets/quotes"
                          :headers {:Accept "application/json"
                                    :Authorization "Bearer fUMaw0yjP8h253ko8uYS6rxwFoli"}
                          :params {:symbols symbol :greeks "true"}
                          :format (ajax/json-request-format)
                          :response-format (ajax/json-response-format {:keywords? true})
                          :on-success [::success-http-request-quote]
                          :on-failure [::failure-http-request-quote]}}))
(reframe/reg-event-db
 ::success-http-request-quote
 (fn-traced [db [_ result]]
            (let [quote (get-in result [:quotes :quote])]
              (merge-with into db {::symbols {(:symbol quote) quote}}))))

(reframe/reg-event-db
 ::failure-http-request-quote
 (fn-traced [db _]
            (assoc db ::request-fail true)))

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
