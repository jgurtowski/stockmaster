 (ns stockmaster.optionstable
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [cljs-time.core :as cljs-time]
            [cljs-time.format :as cljs-time-format]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [goog.string :as gstring]
            [goog.string.format]))


;************************************************************************************************
;************************************************************************************************
;************************************************************************************************
;*Utilities
;************************************************************************************************
;************************************************************************************************
;*************************************************************************************************

(def parse-tradier-date
  (partial cljs-time-format/parse (:date cljs-time-format/formatters)))

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

(defn search-input-change [event]
  (if (= (. event -key) "Enter")
    (reframe/dispatch [:navigate :routes/options {:symbol (.. event -target -value)}])))

(defn search-input []
  [:div {:id "search"}
   [:input {:type "search" :placeholder "Symbol..." :auto-correct "off" :on-key-press search-input-change}]])

(def format-float
  #(gstring/format "%.2f" %))

(defn header-bar []
  (let [underlying-symbol @(reframe/subscribe [::underlying-symbol-db])
        underlying-mark (format-float @(reframe/subscribe [::underlying-mark]))]
    [:div {:class "header"}
     [:div {:id "symbol"}
      [:span {:id "underlying-symbol"} underlying-symbol]
      [:span {:id "underlying-price"} underlying-mark]]
     [:div {:id "stats"}
      [:div {:class "stat"}
       [:div {:class "title"} "Market Cap"]
       [:div {:class "value"} "$100,000,000"]]
      [:div {:class "stat"}
       [:div {:class "title"} "Book/Share"]
       [:div {:class "value"} "$13.29"]]
      [:div {:class "stat"}
       [:div {:class "title"} "Dividend Yield"]
       [:div {:class "value"} "110%"]]
      [:div {:class "stat"}
       [:div {:class "title"} "Price/Book"]
       [:div {:class "value"} "50%"]]
      [:div {:class "stat"}
       [:div {:class "title"} "Price/Earnings"]
       [:div {:class "value"} "1.20x"]]]
     [search-input]]))


(defn option-table-cell [symbol attr itm?]
  (let [value @(reframe/subscribe [attr symbol])
        display-value (gstring/format "%.2f" value)
        style (if itm? "itm" "")]
    [:td {:class style} display-value]))

(defn return-on-risk-cell [symbol itm?]
  (let [ror (* 100 @(reframe/subscribe [::return-on-risk symbol]))
        ror-annualized (* 100 @(reframe/subscribe [::return-on-risk-annualized symbol]))
        display-value (str (gstring/format "%.2f" ror) "/" (gstring/format "%.2f" ror-annualized))
        style (if itm? "itm" "")]
    [:td {:class style} display-value])) 

(defn strike-cell [symbol]
  (let [[diff pdiff] @(reframe/subscribe [::diff-to-underlying symbol])
        strike @(reframe/subscribe [::strike symbol])
        display-value (str (format-float strike) " (" (format-float (* 100 pdiff)) ")")]
    [:td {:class "strike"} display-value]))


(defn option-table-row [call-symbol put-symbol]
  (let [call-itm? @(reframe/subscribe [::itm? call-symbol])
        put-itm? @(reframe/subscribe [::itm? put-symbol])]
    [:tr
     [option-table-cell call-symbol ::ask call-itm?]
     [option-table-cell call-symbol ::bid call-itm?]
     [option-table-cell call-symbol ::mark call-itm?]
     [strike-cell call-symbol]
     [option-table-cell put-symbol ::mark put-itm?]
     [option-table-cell put-symbol ::bid put-itm?]
     [option-table-cell put-symbol ::ask put-itm?]
     [return-on-risk-cell put-symbol put-itm?]]))

(defn option-table [expiration]
  (let [strike-containers @(reframe/subscribe [::option-expiration-strikes expiration])]
    [:div {:class "table-content"}
     [:table {:class "option-table"}
      [:thead
       [:tr {:class "table-header"}
        [:td "Ask"]
        [:td "Bid"]
        [:td "Mark"]
        [:td "Strike (%)"]
        [:td "Mark"]
        [:td "Bid"]
        [:td "Ask"]
        [:td "ROR% (p/a)"]]]
      [:tbody
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
       [:button {:on-click option-list-click} (str option-expiration " (" days-remaining ")")]
       (if (:isopen @component-state) [option-table option-expiration])])))

(defn option-expiration-list []
  "Top Level Option List that has the Different Expirations"
  (let [option-expirations @(reframe/subscribe [::option-expirations])
        underlying-symbol @(reframe/subscribe [::underlying-symbol-db])]
    (if (not (empty? option-expirations))
      [:<>
       (for [x option-expirations]
         ^{:key (str underlying-symbol x)} [option-list-entry x])])))


(defn option-table-root []
  [:<>
   [header-bar]
   [:div {:class "main"}
    [option-expiration-list]]])

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
     (/ (* mark contract-size) (- (* strike contract-size) (* mark contract-size))))))

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


(reframe/reg-sub
  ::diff-to-underlying
 (fn [[_ symbol] _]
   [(reframe/subscribe [::underlying-mark])
    (reframe/subscribe [::strike symbol])])
 (fn [[underlying-mark symbol-strike] _]
   (let [diff (- symbol-strike underlying-mark)]
     [diff (/ diff underlying-mark)])))

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
                                    :Authorization "Bearer gmbnBHbj7m1X2PSEZy7rGOgpnA26"}
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
                                    :Authorization "Bearer gmbnBHbj7m1X2PSEZy7rGOgpnA26"}
                          :params {:symbol (second event)}
                          :format (ajax/json-request-format)
                          :response-format (ajax/json-response-format {:keywords? true})
                          :on-success [::success-http-option-expirations]
                          :on-failure [::failure-http-option-expirations]}}))

(reframe/reg-event-db
 ::success-http-option-expirations
 (fn-traced [db [_ result]]
   (let [expiration-dates-list (get-in result [:expirations :date])
         clean-expiration-dates (filter #(<= (cljs-time/today) (parse-tradier-date %))
                                       expiration-dates-list)]
     (assoc db ::option-expirations
            (apply merge (map #(hash-map % {}) clean-expiration-dates))))))

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
                                          :Authorization "Bearern gmbnBHbj7m1X2PSEZy7rGOgpnA26"}
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
