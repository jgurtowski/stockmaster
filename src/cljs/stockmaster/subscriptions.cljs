(ns stockmaster.subscriptions
  (:require [re-frame.core :as reframe]))

(reframe/reg-sub
 :option-expirations
 (fn [db]
   (keys (:option-expirations db))))

(reframe/reg-sub
 :option-expiration-strikes
 (fn [db query]
   (let [expiration (second query)]
     (get-in db [:option-expirations expiration :strikes]))))
