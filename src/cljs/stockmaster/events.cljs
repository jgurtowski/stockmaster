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
   (let [expiration-dates-list (get-in result [:expirations :date])]
     (assoc db :option-expirations
            (apply merge (map #(hash-map % {}) expiration-dates-list))))))

(reframe/reg-event-db
 ::failure-http-option-expirations
 (fn [db [_ request-type response]]
   (assoc db :request-fail true)))


(reframe/reg-event-fx
 :request-option-strikes
 (fn [_ event]
   (let [symbol (second event)
         expiration (get event 2)]
     {:http-xhrio {:method :get
                   :uri "https://sandbox.tradier.com/v1/markets/options/strikes"
                   :headers {:Accept "application/json"
                             :Authorization "Bearer fUMaw0yjP8h253ko8uYS6rxwFoli"}
                   :params {:symbol symbol
                            :expiration expiration}
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::success-http-option-strikes expiration]
                   :on-failure [::failure-http-option-strikes]}})))

(reframe/reg-event-db
 ::success-http-option-strikes
 (fn [db [_ expiration result]]
   (let [strikes (get-in result [:expirations :date])]
     (assoc-in db [:option-expirations expiration :strikes] (get-in result [:strikes :strike])))))

(reframe/reg-event-db
 ::failure-http-option-strikes
 (fn [db [_ request-type response]]
   (assoc db :request-fail true)))


;probably want to just get the chain and then organize by strike, no need to get the strikes separately

(reframe/reg-event-fx
 :request-option-chains
 (fn [_ event]
   (let [symbol (second event)
         expiration (get event 2)]
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
    (hash-map :put-symbol (:symbol option))
    (hash-map :call-symbol (:symbol option))))

(reframe/reg-event-db
 ::success-http-option-chains
 (fn [db [_ expiration result]]
   (let [options (get-in result [:options :option])
         symbol-map (apply merge (map #(hash-map (:symbol %) %) options))
         db-with-symbols (assoc db :symbols symbol-map)
         strikes (apply merge-with into (map #(sorted-map (:strike %) (extract-symbol-from-chain %)) options))]
     (assoc-in db-with-symbols [:option-expirations expiration :strikes] strikes))))

(reframe/reg-event-db
 ::failure-http-option-chains
 (fn [db [_ request-type response]]
   (assoc db :request-fail true)))


