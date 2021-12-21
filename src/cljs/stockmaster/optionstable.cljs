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



(defn format-float
  ([] (format-float "%.2f"))
  ([format-string]
   (fn [float-to-convert]
     (if-not (nil? float-to-convert)
       (gstring/format format-string float-to-convert)
       "0.00"))))

(defn header-bar []
  (let [underlying-symbol @(reframe/subscribe [::underlying-symbol-db])
        underlying-mark ((format-float) @(reframe/subscribe [::underlying-mark]))]
    [:div {:class "header"}
     [:div {:id "symbol"}
      [:span {:id "underlying-symbol"} underlying-symbol]
      [:span {:id "underlying-price"} underlying-mark]]
     [:div {:id "stats"}
     ;;  [:div {:class "stat"}
     ;;   [:div {:class "title"} "Market Cap"]
     ;;   [:div {:class "value"} "0"]]
     ;;  [:div {:class "stat"}
     ;;   [:div {:class "title"} "Book/Share"]
     ;;   [:div {:class "value"} "0"]]
     ;;  [:div {:class "stat"}
     ;;   [:div {:class "title"} "Dividend Yield"]
     ;;   [:div {:class "value"} "0"]]
     ;;  [:div {:class "stat"}
     ;;   [:div {:class "title"} "Price/Book"]
     ;;   [:div {:class "value"} "0"]]
     ;;  [:div {:class "stat"}
     ;;   [:div {:class "title"} "Price/Earnings"]
      ;;   [:div {:class "value"} "0"]]
      ]
     [search-input]]))

(defn option-table-cell
  ([symbol attr itm?]
   (option-table-cell symbol attr itm? (format-float)))
  ([symbol attr itm? display-float]
  (let [value @(reframe/subscribe [attr symbol])
        display-value (display-float value)
        style (if itm? "itm" "")]
    [:td {:class style} display-value])))

(defn return-on-risk-cell [symbol itm?]
  (let [eror (* 100 @(reframe/subscribe [::extrinsic-return-on-risk-annualized symbol]))
        display-value ((format-float) eror)
        style (if itm? "itm" "")]
    [:td {:class style} display-value]))

(defn strike-cell [symbol]
  (let [[diff pdiff] @(reframe/subscribe [::diff-to-underlying symbol])
        strike @(reframe/subscribe [::strike symbol])
        display-value (str ((format-float) strike) " (" ((format-float) (* 100 pdiff)) ")")]
    [:td {:class "strike"} display-value]))

(defn covered-call-break-even-cell [symbol itm?]
  (let [cc-be @(reframe/subscribe [::cc-be symbol])
        cc-be-percent @(reframe/subscribe [::cc-be-pu symbol])
        display-value (str ((format-float) cc-be) " (" ((format-float) cc-be-percent) ")")
        style (if itm? "itm" "")]
    [:td {:class style} display-value]))

(defn extrinsic-put-cell [symbol itm?]
  (let [extrinsic @(reframe/subscribe [::extrinsic symbol])
        extrinsic-per-month @(reframe/subscribe [::extrinsic-per-month symbol])
        ff (format-float)
        display-value (str (ff extrinsic) "/" (ff extrinsic-per-month))
        style (if itm? "itm" "")]
    [:td {:class style} display-value]))

;;how many stds?
(defn x-std-return-cell [symbol itm? stds]
  (let [x-std-return @(reframe/subscribe [::x-std-return-annualized symbol stds])
        display-value (str ((format-float) (first x-std-return))
                           "/"
                           ((format-float) (second x-std-return))
                           " ("
                           ((format-float) (get x-std-return 2))
                           ")")
        style (if itm? "itm" "")]
    [:td {:class style} display-value]))

(defn option-table-row [call-symbol put-symbol]
  (let [call-itm? @(reframe/subscribe [::itm? call-symbol])
        put-itm? @(reframe/subscribe [::itm? put-symbol])]
    [:tr
     ;[option-table-cell call-symbol ::vega call-itm? (format-float "%.3f")]
     [option-table-cell call-symbol ::iv-mark call-itm?]
     [covered-call-break-even-cell call-symbol call-itm?]
     [option-table-cell call-symbol ::ask call-itm?]
     [option-table-cell call-symbol ::bid call-itm?]
     [option-table-cell call-symbol ::mark call-itm?]
     [strike-cell call-symbol]
     [option-table-cell put-symbol ::mark put-itm?]
     [option-table-cell put-symbol ::bid put-itm?]
     [option-table-cell put-symbol ::ask put-itm?]
     [option-table-cell put-symbol ::basis put-itm?]
;     [option-table-cell put-symbol ::put-profit put-itm?]
     [return-on-risk-cell put-symbol put-itm?]
     [option-table-cell put-symbol ::iv-mark put-itm?]
     ;[x-std-return-cell put-symbol put-itm? 2]
     [extrinsic-put-cell put-symbol put-itm?]
     [option-table-cell put-symbol ::delta put-itm?]
     ;[option-table-cell put-symbol ::vega put-itm? (format-float "%.3f")]
     [option-table-cell put-symbol ::theta put-itm? (format-float "%.3f")]]))

(defn option-table [expiration]
  (let [strike-containers @(reframe/subscribe [::option-expiration-strikes expiration])]
    [:div {:class "table-content"}
     [:table {:class "option-table"}
      [:thead
       [:tr {:class "table-header"}
        ;[:td "Vega"]
        [:td "IV% (mid)"]
        [:td "CC-BE (%U)"]
        [:td "Ask"]
        [:td "Bid"]
        [:td "Mark"]
        [:td {:class "strike"} "Strike (%)"]
        [:td "Mark"]
        [:td "Bid"]
        [:td "Ask"]
        [:td "Short Basis"]
;	[:td "Put Profit"]
        [:td "Ext ROB%(a)"]
        [:td "IV% (mid)"]
        ;[:td "Imp Return (Exp)"]
        [:td "Extrinsic/PM"]
        [:td "Delta"]
        ;[:td "Vega"]
        [:td "Theta"]]]
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
       [:button {:class "expiration-button"
                 :on-click option-list-click}
        (str option-expiration " (" days-remaining ")")]
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

(defn zero-floor [num]
  (if (> num 0) num 0))

(reframe/reg-sub
 ::x-std-return-annualized
 (fn [[_ symbol num-stds] _]
   [(reframe/subscribe [::strike symbol])
    (reframe/subscribe [::iv-mark symbol])
    (reframe/subscribe [::mark symbol])
    (reframe/subscribe [::underlying-mark])
    (reframe/subscribe [::expiration symbol])
    (reframe/subscribe [::basis symbol])])
 (fn [[strike iv mark underlying-mark expiration basis] _]
   (let [days-remaining (days-to-expiration expiration)
         period-adjustment (.sqrt js/Math (/ days-remaining 365.0))
         deviation (* underlying-mark (/ iv 100.0) period-adjustment)
         low (- underlying-mark deviation)
         high (+ underlying-mark deviation)
         low-return (- mark (zero-floor (- strike low)))
         high-return (- mark (zero-floor (- strike high)))
         low-rob (* 100.0 (* (/ 365.0 days-remaining) (/ low-return basis)))
         high-rob (* 100.0 (* (/ 365.0 days-remaining) (/ high-return basis)))]
     [low-rob high-rob (/ (+ low-rob high-rob) 2)])))


(reframe/reg-sub
 ::basis
 (fn [[_ symbol] _]
   [(reframe/subscribe [::mark symbol])
    (reframe/subscribe [::strike symbol])])
 (fn [[mark strike] _]
   (- strike mark)))

;;Covered Call Break Even
(reframe/reg-sub
 ::cc-be
 (fn [[_ symbol] _]
   [(reframe/subscribe [::mark symbol])
    (reframe/subscribe [::strike symbol])])
 (fn [[mark strike] _]
   (+ mark strike)))

(reframe/reg-sub
 ::cc-be-pu
 (fn [[_ symbol] _]
   [(reframe/subscribe [::cc-be symbol])
    (reframe/subscribe [::underlying-mark])])
 (fn [[cc-be underlying-mark] _]
   (let [diff (- cc-be underlying-mark)]
     (* 100 (/ diff underlying-mark)))))

(reframe/reg-sub
 ::return-on-risk
 (fn [[_ symbol] _]
   [(reframe/subscribe [::basis symbol])
    (reframe/subscribe [::mark symbol])])
 (fn [[basis mark] _]
   (/ mark basis)))


(reframe/reg-sub
 ::extrinsic-return-on-risk
 (fn [[_ symbol] _]
   [(reframe/subscribe [::basis symbol])
    (reframe/subscribe [::extrinsic symbol])])
 (fn [[basis extrinsic] _]
   (/ extrinsic basis)))

(reframe/reg-sub
 ::extrinsic-return-on-risk-annualized
 (fn [[_ symbol] _]
   [(reframe/subscribe [::extrinsic-return-on-risk symbol])
    (reframe/subscribe [::expiration symbol])])
 (fn [[eror expiration] _]
   (let [days-remaining (days-to-expiration expiration)]
     (/ (* eror 365.0) days-remaining))))

(reframe/reg-sub
 ::return-on-risk-annualized
 (fn [[_ symbol] _]
   [(reframe/subscribe [::return-on-risk symbol])
    (reframe/subscribe [::expiration symbol])])
 (fn [[ror expiration] _]
   (let [days-remaining (days-to-expiration expiration)]
     (/ (* ror 365.0) days-remaining))))

(reframe/reg-sub
 ::extrinsic-per-month
 (fn [[_ symbol] _]
   [(reframe/subscribe [::expiration symbol])
    (reframe/subscribe [::extrinsic symbol])])
 (fn [[expiration extrinsic] _]
   (let [days-remaining (days-to-expiration expiration)
         months-remaining (/ days-remaining 30)]
     (/ extrinsic months-remaining))))

(reframe/reg-sub
 ::put-profit
 (fn [[_ symbol target budget] _]
     [(reframe/subscribe [::mark symbol])
     (reframe/subscribe [::basis symbol])])
 (fn [[mark basis] _]
     (let [target 10.0 budget 1000.0]
	  (* (- basis target) (/ budget mark)))))

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
 ::iv-mark
 (fn [[_ symbol] _]
   (reframe/subscribe [::greeks symbol]))
 (fn [greeks _]
   (* 100 (:mid_iv greeks))))

(reframe/reg-sub ;; only for puts right now
 ::extrinsic
 (fn [[_ symbol] _]
   [(reframe/subscribe [::mark symbol])
    (reframe/subscribe [::strike symbol])
    (reframe/subscribe [::underlying-mark])])
 (fn [[mark strike underlying-mark] _]
   (if (> strike underlying-mark)
     (- underlying-mark (- strike mark))
     mark)))

(reframe/reg-sub
 ::delta
 (fn [[_ symbol] _]
   (reframe/subscribe [::greeks symbol]))
 (fn [greeks _]
   (:delta greeks)))

(reframe/reg-sub
 ::vega
 (fn [[_ symbol] _]
   (reframe/subscribe [::greeks symbol]))
 (fn [greeks _]
   (:vega greeks)))

(reframe/reg-sub
 ::theta
 (fn [[_ symbol] _]
   (reframe/subscribe [::greeks symbol]))
 (fn [greeks _]
   (:theta greeks)))
(reframe/reg-sub
 ::greeks
 (fn [[_ symbol] _]
   (reframe/subscribe [::symbol-quote symbol]))
 (fn [quote _]
   (:greeks quote)))

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

;*********************************************
;***************************************************
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
                                    :Authorization "Bearer Lczl46XDt3ug6n8No6q4YZM0tp20"}
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
                                    :Authorization "Bearer Lczl46XDt3ug6n8No6q4YZM0tp20"}
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
                                          :Authorization "Bearer Lczl46XDt3ug6n8No6q4YZM0tp20"}
                                :params {:symbol symbol
                                         :expiration expiration
                                         :greeks "true"}
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
