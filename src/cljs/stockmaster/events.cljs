(ns stockmaster.events
  (:require [re-frame.core :as reframe]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))


(reframe/reg-event-fx
 :request-quote
 (fn [_ symbols]
   {:http-xhrio {:method :get
                 :uri "https://sandbox.tradier.com/v1/markets/quotes"
                 :headers {:Accept "application/json"
                           :Authorization "Bearer fUMaw0yjP8h253ko8uYS6rxwFoli"}
                 :params {:symbols (clojure.string/join "," symbols)
                          :greeks "true"}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:success-http-quote]
                 :on-failure [:failure-http-quote]}}))

(reframe/reg-event-db
 :success-http-quote
 (fn [db [_ result]]
   (assoc db :quotes (get-in result [:quote :quotes]))))

(reframe/reg-event-db
 :failure-http-quote
 (fn [db [_ request-type response]]
   (assoc db :request-fail true)))


(reframe/reg-event-fx
 :request-option-expirations
 (fn [_ event]
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
 (fn [db [_ result]]
   (assoc db :option-expirations (get-in result [:expirations :date]))))

(reframe/reg-event-db
 ::failure-http-option-expirations
 (fn [db [_ request-type response]]
   (assoc db :request-fail true)))


(reframe/reg-event-fx
 :request-option-symbols
 (fn [_ event]
   (let [requested-symbol (second event)]
     {:http-xhrio {:method :get
                   :uri "https://sandbox.tradier.com/v1/markets/options/lookup"
                   :headers {:Accept "application/json"
                             :Authorization "Bearer fUMaw0yjP8h253ko8uYS6rxwFoli"}
                   :params {:underlying (second event)}
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::success-http-option-symbols
                                :requested-symbol requested-symbol]
                   :on-failure [::failure-http-option-symbols]}})))


;TODO finish parsing the option symbols into something
;that can make a table
;(defn parse-option-symbol
;  [symbol] 
;  (let [reversed-symbol (reverse symbol)]
;    (subs reversed-symbol 0 8) ;strike
;    (subs reversed-symbol 8 9) ;p/c
;    (subs reversed-symbol 9 15) ;date
;    (subs reversed-symbol 15) ;symbol)



;(reframe/reg-event-db
; ::success-http-option-symbols
; (fn [db [_ result]]
;   (let [requested-symbol (:requested-symbol result)
;         found-symbol (filter #(== (:root-symbol %) requested-symbol) (:symbols result))
;         option-symbols (:options found-symbol)]
;
;     (assoc db :option-symbols (get-in result [:expirations :date]))))

(reframe/reg-event-db
 ::failure-http-option-symbols
 (fn [db [_ request-type response]]
   (assoc db :request-fail true)))


